package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.RedisClient;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysPersonalPasswordForm;
import com.yaya.system.domain.form.SysPersonalProfileForm;
import com.yaya.system.domain.vo.SysPersonalProfileVo;
import com.yaya.system.domain.vo.SysUserVo;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysFileService;
import com.yaya.system.service.SysPersonalService;
import com.yaya.system.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 个人中心 Service 实现
 */
@Slf4j
@Service
@Transactional
public class SysPersonalServiceImpl implements SysPersonalService {

    private static final int MAX_NICKNAME_LENGTH = 100;
    private static final int MAX_CONTACT_LENGTH = 100;
    private static final int MAX_PASSWORD_LENGTH = 200;
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Set<String> AVATAR_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$");

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysFileService sysFileService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private RedisClient redisClient;

    @Resource
    private YaYaConfig yaYaConfig;

    @Override
    public SysPersonalProfileVo getProfile() {
        Long userId = currentUserId();
        SysUserVo detail = sysUserService.getUserDetail(userId);
        if (detail == null) {
            throw new GlobalCommonException("用户不存在");
        }
        return toProfile(detail);
    }

    @Override
    public SysPersonalProfileVo updateProfile(SysPersonalProfileForm form) {
        Long userId = currentUserId();
        SysUser existing = getCurrentUser(userId);
        String nickname = StrUtil.trim(form.getNickname());
        String phone = trimToNull(form.getPhone());
        String email = trimToNull(form.getEmail());
        validateProfile(nickname, phone, email, form.getSex(), userId);
        Integer sex = form.getSex() == null ? existing.getSex() : form.getSex();
        LocalDateTime now = LocalDateTime.now();

        // 部门属于管理属性, 个人中心只能修改昵称/手机号/邮箱/性别, 不允许自行变更部门
        int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getNickname, nickname)
                .set(SysUser::getPhone, phone)
                .set(SysUser::getEmail, email)
                .set(SysUser::getSex, sex)
                .set(SysUser::getUpdateBy, userId)
                .set(SysUser::getUpdateTime, now));
        if (rows <= 0) {
            throw new GlobalCommonException("更新个人资料失败");
        }

        refreshProfileSession(existing, nickname, phone, email, sex, now);
        return getProfile();
    }

    @Override
    public void updatePassword(SysPersonalPasswordForm form) {
        Long userId = currentUserId();
        SysUser existing = getCurrentUser(userId);
        if (StrUtil.isBlank(form.getOldPassword())
                || !passwordEncoder.matches(form.getOldPassword(), existing.getPassword())) {
            throw new GlobalCommonException("原密码不正确");
        }
        if (StrUtil.isBlank(form.getNewPassword())) {
            throw new GlobalCommonException("新密码不能为空");
        }
        if (form.getNewPassword().length() > MAX_PASSWORD_LENGTH) {
            throw new GlobalCommonException("新密码不能超过" + MAX_PASSWORD_LENGTH + "个字符");
        }
        if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            throw new GlobalCommonException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(form.getNewPassword(), existing.getPassword())) {
            throw new GlobalCommonException("新密码不能与原密码相同");
        }

        int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(form.getNewPassword()))
                .set(SysUser::getUpdateBy, userId)
                .set(SysUser::getUpdateTime, LocalDateTime.now()));
        if (rows <= 0) {
            throw new GlobalCommonException("修改密码失败");
        }
        clearLoginSession(existing.getUsername());
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        Long userId = currentUserId();
        validateAvatar(file);
        SysFile uploaded = sysFileService.uploadFile(file);
        String avatar = uploaded.getFileServerUrl();
        if (StrUtil.isBlank(avatar)) {
            throw new GlobalCommonException("头像地址生成失败");
        }
        try {
            int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getUserId, userId)
                    .set(SysUser::getAvatar, avatar)
                    .set(SysUser::getUpdateBy, userId)
                    .set(SysUser::getUpdateTime, LocalDateTime.now()));
            if (rows <= 0) {
                throw new GlobalCommonException("更新头像失败");
            }
            refreshAvatarSession(avatar);
            return avatar;
        } catch (RuntimeException e) {
            // 有事务时由上传模块在回滚后清理, 避免新头像引用阻止事务内的补偿删除。
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                try {
                    sysFileService.deleteFile(uploaded.getFileId());
                } catch (Exception cleanupError) {
                    log.error("头像更新失败后清理文件失败, fileId={}", uploaded.getFileId(), cleanupError);
                }
            }
            throw e;
        }
    }

    /**
     * 当前登录用户ID, 个人中心接口只能操作当前用户
     */
    private Long currentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new GlobalCommonException("登录状态失效，请重新登录");
        }
        return userId;
    }

    /**
     * 查询当前用户, 不存在或已删除时拒绝
     */
    private SysUser getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDeleted(), 1)) {
            throw new GlobalCommonException("用户不存在");
        }
        return user;
    }

    /**
     * 资料字段校验, 手机号/邮箱保持与管理端一致
     */
    private void validateProfile(String nickname, String phone, String email, Integer sex, Long userId) {
        if (StrUtil.isBlank(nickname)) {
            throw new GlobalCommonException("昵称不能为空");
        }
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new GlobalCommonException("昵称不能超过" + MAX_NICKNAME_LENGTH + "个字符");
        }
        if (phone != null && phone.length() > MAX_CONTACT_LENGTH) {
            throw new GlobalCommonException("手机号不能超过" + MAX_CONTACT_LENGTH + "个字符");
        }
        if (phone != null && sysUserMapper.countByPhone(phone, userId) > 0) {
            throw new GlobalCommonException("手机号已存在");
        }
        if (email != null) {
            if (email.length() > MAX_CONTACT_LENGTH) {
                throw new GlobalCommonException("邮箱不能超过" + MAX_CONTACT_LENGTH + "个字符");
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new GlobalCommonException("邮箱格式不正确");
            }
            if (sysUserMapper.countByEmail(email, userId) > 0) {
                throw new GlobalCommonException("邮箱已存在");
            }
        }
        if (sex != null && (sex < 0 || sex > 2)) {
            throw new GlobalCommonException("性别值不正确");
        }
    }

    /**
     * 刷新Redis中的登录用户资料, 保证token继续使用时会话中的昵称等字段最新;
     * 部门不在此处变更, 会话保持登录时的部门
     */
    private void refreshProfileSession(SysUser existing, String nickname, String phone, String email,
                                       Integer sex, LocalDateTime updateTime) {
        LoginUserDetails loginUser = requireLoginUser();
        SysUser sessionUser = loginUser.getSysUser();
        if (sessionUser == null) {
            throw new GlobalCommonException("登录状态失效，请重新登录");
        }
        sessionUser.setUserId(existing.getUserId());
        sessionUser.setNickname(nickname);
        sessionUser.setPhone(phone);
        sessionUser.setEmail(email);
        sessionUser.setSex(sex);
        sessionUser.setUpdateBy(existing.getUserId());
        sessionUser.setUpdateTime(updateTime);
        loginUser.setSysUser(sessionUser);
        saveLoginSession(loginUser);
    }

    /**
     * 刷新Redis中的登录用户头像
     */
    private void refreshAvatarSession(String avatar) {
        LoginUserDetails loginUser = requireLoginUser();
        SysUser sessionUser = loginUser.getSysUser();
        if (sessionUser == null) {
            throw new GlobalCommonException("登录状态失效，请重新登录");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            String previousAvatar = sessionUser.getAvatar();
            LocalDateTime previousUpdateTime = sessionUser.getUpdateTime();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        sessionUser.setAvatar(previousAvatar);
                        sessionUser.setUpdateTime(previousUpdateTime);
                        // 回滚会删除新头像文件, 缓存不能继续指向该文件。
                        try {
                            redisClient.del(KeyConstant.LOGIN_USER_KEY.getKey() + loginUser.getUsername());
                        } catch (Exception e) {
                            log.error("头像事务回滚后清理登录会话失败, username={}", loginUser.getUsername(), e);
                        }
                    }
                }
            });
        }
        sessionUser.setAvatar(avatar);
        sessionUser.setUpdateTime(LocalDateTime.now());
        loginUser.setSysUser(sessionUser);
        saveLoginSession(loginUser);
    }

    private LoginUserDetails requireLoginUser() {
        LoginUserDetails loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new GlobalCommonException("登录状态失效，请重新登录");
        }
        return loginUser;
    }

    /**
     * 重新写入登录会话, 保持登录状态并同步最新资料
     */
    private void saveLoginSession(LoginUserDetails loginUser) {
        String username = loginUser.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new GlobalCommonException("登录状态失效，请重新登录");
        }
        Integer timeout = yaYaConfig.getTokenTimeout();
        try {
            redisClient.set(KeyConstant.LOGIN_USER_KEY.getKey() + username,
                    JSONUtil.toJsonStr(loginUser), timeout == null ? 30 : timeout);
        } catch (Exception e) {
            log.error("个人中心刷新登录会话失败, username={}", username, e);
            throw new GlobalCommonException("登录信息刷新失败，请重新登录");
        }
    }

    /**
     * 密码修改后必须清理登录会话, 旧token立即失效
     */
    private void clearLoginSession(String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        try {
            redisClient.del(KeyConstant.LOGIN_USER_KEY.getKey() + username);
        } catch (Exception e) {
            log.error("个人中心清理登录会话失败, username={}", username, e);
            throw new GlobalCommonException("修改密码失败，请稍后重试");
        }
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalCommonException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new GlobalCommonException("头像文件不能超过5MB");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new GlobalCommonException("头像仅支持jpg、jpeg、png、gif、webp、bmp格式");
        }
        String contentType = file.getContentType();
        if (StrUtil.isNotBlank(contentType)
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new GlobalCommonException("头像文件类型不正确");
        }
    }

    private String extractExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private SysPersonalProfileVo toProfile(SysUserVo detail) {
        SysPersonalProfileVo vo = new SysPersonalProfileVo();
        vo.setUserId(detail.getUserId());
        vo.setUsername(detail.getUsername());
        vo.setNickname(detail.getNickname());
        vo.setPhone(detail.getPhone());
        vo.setEmail(detail.getEmail());
        vo.setSex(detail.getSex());
        vo.setAvatar(detail.getAvatar());
        vo.setDeptId(detail.getDeptId());
        vo.setDeptName(detail.getDeptName());
        vo.setRoleNames(detail.getRoleNames());
        return vo;
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }
}
