package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.entity.SysUserPost;
import com.yaya.system.domain.entity.SysUserRole;
import com.yaya.system.domain.form.SysUserForm;
import com.yaya.system.domain.query.SysUserQuery;
import com.yaya.system.domain.vo.SysUserImportResultVo;
import com.yaya.system.domain.vo.SysUserVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.mapper.SysRoleMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.mapper.SysUserPostMapper;
import com.yaya.system.mapper.SysUserRoleMapper;
import com.yaya.system.service.SysLoginAttemptService;
import com.yaya.system.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户管理 Service 实现
 *
 * @author yaya
 */
@Slf4j
@Service
@Transactional
public class SysUserServiceImpl implements SysUserService {

    /** 状态: 正常 */
    private static final int STATUS_NORMAL = 1;
    /** 状态: 禁用 */
    private static final int STATUS_DISABLED = 0;
    /** 状态: 锁定 */
    private static final int STATUS_LOCKED = 2;

    /** 默认头像 */
    private static final String DEFAULT_AVATAR = "file/avatar.png";
    /** 导入时密码为空时的默认密码 */
    private static final String DEFAULT_IMPORT_PASSWORD = "123456";
    /** ROOT 角色编号 */
    private static final String ROOT_ROLE_CODE = "ROOT";

    private static final int MAX_USERNAME_LENGTH = 200;
    private static final int MAX_NICKNAME_LENGTH = 100;
    private static final int MAX_CONTACT_LENGTH = 100;
    private static final int MAX_REMARK_LENGTH = 500;
    private static final int MAX_PASSWORD_LENGTH = 200;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysUserPostMapper sysUserPostMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPostMapper sysPostMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private RedisClient redisClient;

    @Resource
    private SysLoginAttemptService sysLoginAttemptService;

    @Override
    public PageDTO<SysUserVo> getUserPage(SysUserQuery query) {
        List<Long> deptIds = resolveDeptIdsWithDescendants(query.getDeptId());
        Page<SysUserVo> page = sysUserMapper.getUserPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getUsername(), query.getNickname(), deptIds, query.getStatus());
        fillUserRelations(page.getRecords());
        fillAutoLockStatus(page.getRecords());
        PageDTO<SysUserVo> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public SysUserVo getUserDetail(Long userId) {
        if (userId == null) {
            throw new GlobalCommonException("用户ID不能为空");
        }
        SysUserVo userVo = sysUserMapper.getUserDetail(userId);
        if (userVo == null) {
            throw new GlobalCommonException("用户不存在");
        }
        fillUserRelations(List.of(userVo));
        fillAutoLockStatus(List.of(userVo));
        return userVo;
    }

    @Override
    public Long addUser(SysUserForm form) {
        String username = StrUtil.trim(form.getUsername());
        if (StrUtil.isBlank(username)) {
            throw new GlobalCommonException("用户名不能为空");
        }
        String nickname = StrUtil.trim(form.getNickname());
        String phone = trimToNull(form.getPhone());
        String email = trimToNull(form.getEmail());
        validateUserProfile(username, nickname, phone, email, form.getSex(),
                form.getRemark(), form.getPassword(), true);
        if (sysUserMapper.countByUsername(username, null) > 0) {
            throw new GlobalCommonException("用户名已存在");
        }
        validatePhoneUnique(phone, null);
        validateEmailUnique(email, null);
        validateDept(form.getDeptId());
        List<Long> roleIds = normalizeIds(form.getRoleIds());
        List<Long> postIds = normalizeIds(form.getPostIds());
        validateRoles(roleIds);
        validatePosts(postIds);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setSex(form.getSex() == null ? 1 : form.getSex());
        user.setDeptId(form.getDeptId());
        user.setAvatar(DEFAULT_AVATAR);
        user.setExpiredTime(form.getExpiredTime());
        user.setRemark(form.getRemark());
        user.setIsLocked(0);
        user.setIsEnabled(1);
        user.setIsDeleted(0);
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        user.setCreateBy(userId);
        user.setUpdateBy(userId);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        if (sysUserMapper.insert(user) <= 0) {
            throw new GlobalCommonException("新增用户失败");
        }
        saveUserRoles(user.getUserId(), roleIds);
        saveUserPosts(user.getUserId(), postIds);
        return user.getUserId();
    }

    @Override
    public boolean updateUser(SysUserForm form) {
        if (form.getUserId() == null) {
            throw new GlobalCommonException("用户ID不能为空");
        }
        SysUser existing = getExistingUser(form.getUserId());
        if (isRootUser(form.getUserId())) {
            throw new GlobalCommonException("ROOT账户不允许通过用户管理修改");
        }
        String submittedUsername = StrUtil.trim(form.getUsername());
        if (StrUtil.isNotBlank(submittedUsername)
                && !submittedUsername.equals(existing.getUsername())) {
            throw new GlobalCommonException("用户名创建后不允许修改");
        }
        String nickname = StrUtil.trim(form.getNickname());
        String phone = trimToNull(form.getPhone());
        String email = trimToNull(form.getEmail());
        validateUserProfile(submittedUsername, nickname, phone, email, form.getSex(),
                form.getRemark(), null, false);
        validatePhoneUnique(phone, form.getUserId());
        validateEmailUnique(email, form.getUserId());
        validateDept(form.getDeptId());
        List<Long> roleIds = form.getRoleIds() == null ? null : normalizeIds(form.getRoleIds());
        List<Long> postIds = form.getPostIds() == null ? null : normalizeIds(form.getPostIds());
        if (roleIds != null) {
            validateRoles(roleIds);
        }
        if (postIds != null) {
            validatePosts(postIds);
        }

        Integer effectiveSex = form.getSex() == null ? existing.getSex() : form.getSex();
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, form.getUserId())
                .set(SysUser::getNickname, nickname)
                .set(SysUser::getPhone, phone)
                .set(SysUser::getEmail, email)
                .set(SysUser::getSex, effectiveSex)
                .set(SysUser::getDeptId, form.getDeptId())
                .set(SysUser::getExpiredTime, form.getExpiredTime())
                .set(SysUser::getRemark, form.getRemark())
                .set(SysUser::getUpdateBy, SecurityUtils.getUserId())
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        boolean updated = sysUserMapper.update(null, wrapper) > 0;
        if (!updated) {
            return false;
        }
        if (roleIds != null) {
            saveUserRoles(form.getUserId(), roleIds);
        }
        if (postIds != null) {
            saveUserPosts(form.getUserId(), postIds);
        }
        // 部门、过期时间、角色变化会影响登录后的权限/数据范围, 清理旧会话强制重新登录
        if (!Objects.equals(existing.getDeptId(), form.getDeptId())
                || !Objects.equals(existing.getExpiredTime(), form.getExpiredTime())
                || roleIds != null) {
            clearLoginSession(existing.getUsername());
        }
        return true;
    }

    @Override
    public boolean deleteUser(Long userId) {
        SysUser existing = getExistingUser(userId);
        if (isRootUser(userId)) {
            throw new GlobalCommonException("ROOT账户不允许删除");
        }
        boolean deleted = sysUserMapper.logicDeleteById(
                userId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
        if (deleted) {
            sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
            sysUserPostMapper.delete(new QueryWrapper<SysUserPost>().eq("user_id", userId));
            clearLoginSession(existing.getUsername());
        }
        return deleted;
    }

    @Override
    public boolean resetPassword(Long userId, String password) {
        SysUser existing = getExistingUser(userId);
        if (isRootUser(userId)) {
            throw new GlobalCommonException("ROOT账户不允许重置密码");
        }
        if (StrUtil.isBlank(password)) {
            throw new GlobalCommonException("密码不能为空");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(password));
        update.setUpdateBy(SecurityUtils.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        boolean updated = sysUserMapper.updateById(update) > 0;
        if (updated) {
            clearLoginSession(existing.getUsername());
            clearLoginAttempts(userId);
        }
        return updated;
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        if (status == null
                || (status != STATUS_NORMAL && status != STATUS_DISABLED && status != STATUS_LOCKED)) {
            throw new GlobalCommonException("用户状态值不正确");
        }
        SysUser existing = getExistingUser(userId);
        if (isRootUser(userId) && status != STATUS_NORMAL) {
            throw new GlobalCommonException("ROOT账户不允许禁用或锁定");
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        if (status == STATUS_NORMAL) {
            update.setIsEnabled(1);
            update.setIsLocked(0);
        } else if (status == STATUS_DISABLED) {
            update.setIsEnabled(0);
            update.setIsLocked(0);
        } else {
            update.setIsEnabled(1);
            update.setIsLocked(1);
        }
        update.setUpdateBy(SecurityUtils.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        boolean updated = sysUserMapper.updateById(update) > 0;
        if (updated) {
            clearLoginSession(existing.getUsername());
            clearLoginAttempts(userId);
        }
        return updated;
    }

    @Override
    public boolean unlockUser(Long userId) {
        SysUser existing = getExistingUser(userId);
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setIsLocked(0);
        update.setUpdateBy(SecurityUtils.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        boolean updated = sysUserMapper.updateById(update) > 0;
        if (updated) {
            clearLoginSession(existing.getUsername());
            clearLoginAttempts(userId);
        }
        return updated;
    }

    @Override
    public boolean assignUserRoles(Long userId, List<Long> roleIds) {
        SysUser existing = getExistingUser(userId);
        if (isRootUser(userId)) {
            throw new GlobalCommonException("ROOT账户不允许修改角色");
        }
        List<Long> targetRoleIds = normalizeIds(roleIds);
        validateRoles(targetRoleIds);
        saveUserRoles(userId, targetRoleIds);
        clearLoginSession(existing.getUsername());
        return true;
    }

    @Override
    public boolean assignUserPosts(Long userId, List<Long> postIds) {
        getExistingUser(userId);
        List<Long> targetPostIds = normalizeIds(postIds);
        validatePosts(targetPostIds);
        saveUserPosts(userId, targetPostIds);
        return true;
    }

    @Override
    public List<SysUserVo> getUserOptions(String keyword) {
        return sysUserMapper.getUserOptions(StrUtil.trim(keyword));
    }

    @Override
    public byte[] exportUsers(SysUserQuery query) {
        SysUserQuery effectiveQuery = query == null ? new SysUserQuery() : query;
        List<Long> deptIds = resolveDeptIdsWithDescendants(effectiveQuery.getDeptId());
        List<SysUserVo> users = sysUserMapper.getUserList(
                effectiveQuery.getUsername(), effectiveQuery.getNickname(),
                deptIds, effectiveQuery.getStatus());
        fillUserRelations(users);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户数据");
            String[] headers = {"用户ID", "用户名", "昵称", "手机号", "邮箱", "性别",
                    "部门", "角色", "岗位", "状态", "过期时间", "创建时间"};
            writeHeader(sheet, workbook, headers);
            int rowIndex = 1;
            for (SysUserVo user : users) {
                Row row = sheet.createRow(rowIndex++);
                setCell(row, 0, String.valueOf(user.getUserId()));
                setCell(row, 1, user.getUsername());
                setCell(row, 2, user.getNickname());
                setCell(row, 3, user.getPhone());
                setCell(row, 4, user.getEmail());
                setCell(row, 5, sexText(user.getSex()));
                setCell(row, 6, user.getDeptName());
                setCell(row, 7, joinNames(user.getRoleNames()));
                setCell(row, 8, joinNames(user.getPostNames()));
                setCell(row, 9, statusText(user.getStatus()));
                setCell(row, 10, formatDateTime(user.getExpiredTime()));
                setCell(row, 11, formatDateTime(user.getCreateTime()));
            }
            setColumnWidths(sheet, 12);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("用户导出-Excel生成失败", e);
            throw new GlobalCommonException("用户导出失败");
        }
    }

    @Override
    public SysUserImportResultVo importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalCommonException("导入文件不能为空");
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase();
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new GlobalCommonException("仅支持 xlsx/xls 格式的Excel文件");
        }
        List<PreparedUser> preparedUsers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new GlobalCommonException("导入文件没有数据");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            String headerUser = cellString(header, 0, formatter);
            String headerNickname = cellString(header, 1, formatter);
            if (!headerUser.contains("用户名") || !headerNickname.contains("昵称")) {
                throw new GlobalCommonException("导入模板不正确，请下载最新模板");
            }
            if (sheet.getLastRowNum() < 1) {
                throw new GlobalCommonException("导入文件没有数据");
            }
            Set<String> fileUsernames = new HashSet<>();
            Set<String> filePhones = new HashSet<>();
            Set<String> fileEmails = new HashSet<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row, formatter, 11)) {
                    continue;
                }
                total++;
                int rowNo = i + 1;
                List<String> rowErrors = new ArrayList<>();
                String username = cellString(row, 0, formatter);
                String nickname = cellString(row, 1, formatter);
                String password = cellString(row, 2, formatter);
                String phone = trimToNull(cellString(row, 3, formatter));
                String email = trimToNull(cellString(row, 4, formatter));
                String sexValue = cellString(row, 5, formatter);
                String deptIdValue = cellString(row, 6, formatter);
                String roleCodesValue = cellString(row, 7, formatter);
                String postCodesValue = cellString(row, 8, formatter);
                String expiredValue = cellString(row, 9, formatter);
                String remark = cellString(row, 10, formatter);

                if (StrUtil.isBlank(username)) {
                    rowErrors.add("用户名不能为空");
                } else {
                    username = username.trim();
                    if (username.length() > MAX_USERNAME_LENGTH) {
                        rowErrors.add("用户名不能超过" + MAX_USERNAME_LENGTH + "个字符");
                    }
                    if (!fileUsernames.add(username)) {
                        rowErrors.add("用户名在文件中重复");
                    } else if (sysUserMapper.countByUsername(username, null) > 0) {
                        rowErrors.add("用户名已存在");
                    }
                }
                if (StrUtil.isBlank(nickname)) {
                    rowErrors.add("昵称不能为空");
                } else if (nickname.trim().length() > MAX_NICKNAME_LENGTH) {
                    rowErrors.add("昵称不能超过" + MAX_NICKNAME_LENGTH + "个字符");
                }
                if (StrUtil.isNotBlank(password) && password.length() > MAX_PASSWORD_LENGTH) {
                    rowErrors.add("密码不能超过" + MAX_PASSWORD_LENGTH + "个字符");
                }
                if (phone != null) {
                    if (phone.length() > MAX_CONTACT_LENGTH) {
                        rowErrors.add("手机号不能超过" + MAX_CONTACT_LENGTH + "个字符");
                    }
                    if (!filePhones.add(phone)) {
                        rowErrors.add("手机号在文件中重复");
                    } else if (sysUserMapper.countByPhone(phone, null) > 0) {
                        rowErrors.add("手机号已存在");
                    }
                }
                if (email != null) {
                    if (email.length() > MAX_CONTACT_LENGTH) {
                        rowErrors.add("邮箱不能超过" + MAX_CONTACT_LENGTH + "个字符");
                    } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                        rowErrors.add("邮箱格式不正确");
                    }
                    String emailKey = email.toLowerCase();
                    if (!fileEmails.add(emailKey)) {
                        rowErrors.add("邮箱在文件中重复");
                    } else if (sysUserMapper.countByEmail(email, null) > 0) {
                        rowErrors.add("邮箱已存在");
                    }
                }
                Long deptId = null;
                if (StrUtil.isBlank(deptIdValue)) {
                    rowErrors.add("部门ID不能为空");
                } else {
                    try {
                        deptId = Long.parseLong(deptIdValue.trim());
                        SysDept dept = sysDeptMapper.selectById(deptId);
                        if (dept == null || Objects.equals(dept.getIsDeleted(), 1)) {
                            rowErrors.add("所属部门不存在");
                        } else if (!Objects.equals(dept.getStatus(), 1)) {
                            rowErrors.add("所属部门已停用");
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add("部门ID格式不正确");
                    }
                }
                int sex = 1;
                if (StrUtil.isNotBlank(sexValue)) {
                    try {
                        sex = Integer.parseInt(sexValue.trim());
                        if (sex < 0 || sex > 2) {
                            rowErrors.add("性别值不正确");
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add("性别格式不正确");
                    }
                }
                LocalDateTime expiredTime = null;
                if (StrUtil.isNotBlank(expiredValue)) {
                    try {
                        expiredTime = parseDateTime(expiredValue.trim());
                    } catch (DateTimeParseException e) {
                        rowErrors.add("过期时间格式不正确(应为yyyy-MM-dd HH:mm:ss)");
                    }
                }
                List<String> roleCodes = splitCodes(roleCodesValue);
                List<Long> roleIds = new ArrayList<>();
                boolean hasRootCode = roleCodes.stream()
                        .anyMatch(code -> ROOT_ROLE_CODE.equalsIgnoreCase(code));
                if (hasRootCode) {
                    rowErrors.add("不允许分配ROOT角色");
                } else if (!roleCodes.isEmpty()) {
                    Map<String, Long> roleMap = loadRoleIdsByCodes(roleCodes);
                    for (String code : roleCodes) {
                        Long roleId = roleMap.get(code.toUpperCase(Locale.ROOT));
                        if (roleId == null) {
                            rowErrors.add("角色编号不存在: " + code);
                        } else {
                            roleIds.add(roleId);
                        }
                    }
                }
                List<String> postCodes = splitCodes(postCodesValue);
                List<Long> postIds = new ArrayList<>();
                if (!postCodes.isEmpty()) {
                    Map<String, Long> postMap = loadPostIdsByCodes(postCodes);
                    for (String code : postCodes) {
                        Long postId = postMap.get(code);
                        if (postId == null) {
                            rowErrors.add("岗位编号不存在: " + code);
                        } else {
                            postIds.add(postId);
                        }
                    }
                }
                if (remark != null && remark.length() > MAX_REMARK_LENGTH) {
                    rowErrors.add("备注不能超过" + MAX_REMARK_LENGTH + "个字符");
                }
                if (!rowErrors.isEmpty()) {
                    for (String error : rowErrors) {
                        errors.add("第" + rowNo + "行: " + error);
                    }
                    continue;
                }
                PreparedUser prepared = new PreparedUser();
                prepared.username = username;
                prepared.nickname = nickname.trim();
                prepared.password = StrUtil.isBlank(password) ? DEFAULT_IMPORT_PASSWORD : password;
                prepared.phone = phone;
                prepared.email = email;
                prepared.sex = sex;
                prepared.deptId = deptId;
                prepared.expiredTime = expiredTime;
                prepared.remark = remark;
                prepared.roleIds = roleIds;
                prepared.postIds = postIds;
                preparedUsers.add(prepared);
            }
        } catch (GlobalCommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户导入-Excel解析失败", e);
            throw new GlobalCommonException("Excel解析失败，请检查文件格式");
        }
        SysUserImportResultVo result = new SysUserImportResultVo();
        result.setTotal(total);
        result.setErrors(errors);
        if (!errors.isEmpty()) {
            result.setSuccessCount(0);
            result.setFailCount(total);
            return result;
        }
        if (total == 0) {
            throw new GlobalCommonException("导入文件没有数据");
        }
        Long operatorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        for (PreparedUser prepared : preparedUsers) {
            SysUser user = new SysUser();
            user.setUsername(prepared.username);
            user.setPassword(passwordEncoder.encode(prepared.password));
            user.setNickname(prepared.nickname);
            user.setPhone(prepared.phone);
            user.setEmail(prepared.email);
            user.setSex(prepared.sex);
            user.setDeptId(prepared.deptId);
            user.setAvatar(DEFAULT_AVATAR);
            user.setExpiredTime(prepared.expiredTime);
            user.setRemark(prepared.remark);
            user.setIsLocked(0);
            user.setIsEnabled(1);
            user.setIsDeleted(0);
            user.setCreateBy(operatorId);
            user.setUpdateBy(operatorId);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            sysUserMapper.insert(user);
            saveUserRoles(user.getUserId(), prepared.roleIds);
            saveUserPosts(user.getUserId(), prepared.postIds);
        }
        result.setSuccessCount(total);
        result.setFailCount(0);
        return result;
    }

    @Override
    public byte[] getUserImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            String[] headers = {"用户名*", "昵称*", "密码(留空默认123456)", "手机号", "邮箱",
                    "性别(1男/0女/2保密)", "部门ID*", "角色编号(逗号分隔)",
                    "岗位编号(逗号分隔)", "过期时间(yyyy-MM-dd HH:mm:ss)", "备注"};
            writeHeader(sheet, workbook, headers);
            setColumnWidths(sheet, headers.length);
            Sheet notes = workbook.createSheet("填写说明");
            String[] noteLines = {
                    "1. 用户名、昵称、部门ID为必填项；用户名全局唯一。",
                    "2. 密码留空时默认使用 123456。",
                    "3. 手机号、邮箱填写后不能与其他未删除用户重复。",
                    "4. 角色编号、岗位编号多个用英文逗号分隔，例如：OPERATION,MANAGER。",
                    "5. 不允许分配 ROOT 角色。",
                    "6. 部门ID必须是已存在的正常部门，可先在部门管理中查询。",
                    "7. 过期时间格式：yyyy-MM-dd HH:mm:ss；留空表示长期有效。",
                    "8. 导入采用整批校验：任意一行有错误时，整批都不会导入。"
            };
            for (int i = 0; i < noteLines.length; i++) {
                notes.createRow(i).createCell(0).setCellValue(noteLines[i]);
            }
            notes.setColumnWidth(0, 100 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("用户导入模板-生成失败", e);
            throw new GlobalCommonException("下载导入模板失败");
        }
    }

    // ==================== 私有方法 ====================

    private SysUser getExistingUser(Long userId) {
        if (userId == null) {
            throw new GlobalCommonException("用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Objects.equals(user.getIsDeleted(), 1)) {
            throw new GlobalCommonException("用户不存在");
        }
        return user;
    }

    private boolean isRootUser(Long userId) {
        return sysUserMapper.countRootRoleByUserId(userId) > 0;
    }

    private void validateUserProfile(String username, String nickname, String phone, String email,
                                     Integer sex, String remark, String password, boolean requirePassword) {
        if (StrUtil.isNotBlank(username) && username.length() > MAX_USERNAME_LENGTH) {
            throw new GlobalCommonException("用户名不能超过" + MAX_USERNAME_LENGTH + "个字符");
        }
        if (StrUtil.isBlank(nickname)) {
            throw new GlobalCommonException("昵称不能为空");
        }
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new GlobalCommonException("昵称不能超过" + MAX_NICKNAME_LENGTH + "个字符");
        }
        if (phone != null && phone.length() > MAX_CONTACT_LENGTH) {
            throw new GlobalCommonException("手机号不能超过" + MAX_CONTACT_LENGTH + "个字符");
        }
        if (email != null) {
            if (email.length() > MAX_CONTACT_LENGTH) {
                throw new GlobalCommonException("邮箱不能超过" + MAX_CONTACT_LENGTH + "个字符");
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new GlobalCommonException("邮箱格式不正确");
            }
        }
        if (sex != null && (sex < 0 || sex > 2)) {
            throw new GlobalCommonException("性别值不正确");
        }
        if (remark != null && remark.length() > MAX_REMARK_LENGTH) {
            throw new GlobalCommonException("备注不能超过" + MAX_REMARK_LENGTH + "个字符");
        }
        if (requirePassword) {
            if (StrUtil.isBlank(password)) {
                throw new GlobalCommonException("密码不能为空");
            }
            if (password.length() > MAX_PASSWORD_LENGTH) {
                throw new GlobalCommonException("密码不能超过" + MAX_PASSWORD_LENGTH + "个字符");
            }
        }
    }

    private void validatePhoneUnique(String phone, Long excludeUserId) {
        if (phone == null) {
            return;
        }
        if (sysUserMapper.countByPhone(phone, excludeUserId) > 0) {
            throw new GlobalCommonException("手机号已存在");
        }
    }

    private void validateEmailUnique(String email, Long excludeUserId) {
        if (email == null) {
            return;
        }
        if (sysUserMapper.countByEmail(email, excludeUserId) > 0) {
            throw new GlobalCommonException("邮箱已存在");
        }
    }

    /**
     * 将部门筛选条件展开为目标部门及其所有子孙部门, 用于用户分页和导出。
     */
    private List<Long> resolveDeptIdsWithDescendants(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept target = sysDeptMapper.selectById(deptId);
        if (target == null || Objects.equals(target.getIsDeleted(), 1)) {
            throw new GlobalCommonException("部门不存在");
        }
        String treePath = StrUtil.isBlank(target.getTreePath()) ? "0" : target.getTreePath().trim();
        String prefix = treePath + "," + deptId;
        List<SysDept> depts = sysDeptMapper.selectList(new QueryWrapper<SysDept>()
                .eq("is_deleted", 0)
                .and(wrapper -> wrapper
                        .eq("dept_id", deptId)
                        .or()
                        .eq("tree_path", prefix)
                        .or()
                        .likeRight("tree_path", prefix + ",")));
        return depts.stream()
                .map(SysDept::getDeptId)
                .distinct()
                .toList();
    }

    private void validateDept(Long deptId) {
        if (deptId == null) {
            throw new GlobalCommonException("所属部门不能为空");
        }
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null || Objects.equals(dept.getIsDeleted(), 1)) {
            throw new GlobalCommonException("所属部门不存在");
        }
        if (!Objects.equals(dept.getStatus(), 1)) {
            throw new GlobalCommonException("所属部门已停用");
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void validateRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                .in("role_id", roleIds)
                .eq("is_deleted", 0));
        if (roles.size() != roleIds.size()) {
            throw new GlobalCommonException("存在无效的角色ID");
        }
        boolean hasRoot = roles.stream()
                .anyMatch(role -> ROOT_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()));
        if (hasRoot) {
            throw new GlobalCommonException("不允许分配ROOT角色");
        }
    }

    private void validatePosts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        Long count = sysPostMapper.selectCount(new QueryWrapper<SysPost>()
                .in("post_id", postIds)
                .eq("is_deleted", 0));
        if (count == null || count.intValue() != postIds.size()) {
            throw new GlobalCommonException("存在无效的岗位ID");
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        sysUserPostMapper.delete(new QueryWrapper<SysUserPost>().eq("user_id", userId));
        for (Long postId : postIds) {
            SysUserPost userPost = new SysUserPost();
            userPost.setUserId(userId);
            userPost.setPostId(postId);
            sysUserPostMapper.insert(userPost);
        }
    }

    /**
     * 批量填充角色/岗位名称并计算状态
     */
    private void fillUserRelations(List<SysUserVo> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUserVo::getUserId).filter(Objects::nonNull).toList();
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new QueryWrapper<SysUserRole>()
                .in("user_id", userIds));
        Map<Long, List<Long>> roleIdsByUser = new LinkedHashMap<>();
        Set<Long> allRoleIds = new HashSet<>();
        for (SysUserRole userRole : userRoles) {
            roleIdsByUser.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>())
                    .add(userRole.getRoleId());
            allRoleIds.add(userRole.getRoleId());
        }
        Map<Long, String> roleNameMap = new HashMap<>();
        if (!allRoleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                    .in("role_id", allRoleIds)
                    .eq("is_deleted", 0));
            for (SysRole role : roles) {
                roleNameMap.put(role.getRoleId(), role.getRoleName());
            }
        }

        List<SysUserPost> userPosts = sysUserPostMapper.selectList(new QueryWrapper<SysUserPost>()
                .in("user_id", userIds));
        Map<Long, List<Long>> postIdsByUser = new LinkedHashMap<>();
        Set<Long> allPostIds = new HashSet<>();
        for (SysUserPost userPost : userPosts) {
            postIdsByUser.computeIfAbsent(userPost.getUserId(), key -> new ArrayList<>())
                    .add(userPost.getPostId());
            allPostIds.add(userPost.getPostId());
        }
        Map<Long, String> postNameMap = new HashMap<>();
        if (!allPostIds.isEmpty()) {
            List<SysPost> posts = sysPostMapper.selectList(new QueryWrapper<SysPost>()
                    .in("post_id", allPostIds)
                    .eq("is_deleted", 0));
            for (SysPost post : posts) {
                postNameMap.put(post.getPostId(), post.getPostName());
            }
        }

        for (SysUserVo user : users) {
            List<Long> roleIds = roleIdsByUser.getOrDefault(user.getUserId(), new ArrayList<>());
            user.setRoleIds(roleIds);
            user.setRoleNames(roleIds.stream().map(roleNameMap::get)
                    .filter(Objects::nonNull).toList());
            List<Long> postIds = postIdsByUser.getOrDefault(user.getUserId(), new ArrayList<>());
            user.setPostIds(postIds);
            user.setPostNames(postIds.stream().map(postNameMap::get)
                    .filter(Objects::nonNull).toList());
            if (Objects.equals(user.getIsLocked(), 1)) {
                user.setStatus(STATUS_LOCKED);
            } else if (Objects.equals(user.getIsEnabled(), 1)) {
                user.setStatus(STATUS_NORMAL);
            } else {
                user.setStatus(STATUS_DISABLED);
            }
        }
    }

    /**
     * 批量补充密码错误自动临时锁定状态, 用户列表仍以Redis实时状态为准
     */
    private void fillAutoLockStatus(List<SysUserVo> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        users.forEach(user -> {
            user.setAutoLocked(false);
            user.setAutoLockRemainingSeconds(null);
        });
        List<Long> userIds = users.stream()
                .map(SysUserVo::getUserId)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        try {
            Map<Long, Long> lockMap = sysLoginAttemptService.getAutoLockRemainingSeconds(userIds);
            for (SysUserVo user : users) {
                Long seconds = lockMap.get(user.getUserId());
                if (seconds != null && seconds > 0) {
                    user.setAutoLocked(true);
                    user.setAutoLockRemainingSeconds(seconds);
                }
            }
        } catch (Exception e) {
            log.warn("用户列表-查询自动锁定状态失败, 不展示临时锁定状态", e);
        }
    }

    private void clearLoginSession(String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        try {
            redisClient.del(KeyConstant.LOGIN_USER_KEY.getKey() + username);
        } catch (Exception e) {
            log.error("用户管理-清理登录会话失败, username={}", username, e);
        }
    }

    /**
     * 管理员解锁/重置密码后, 同步清理临时登录限制, 让用户立即恢复登录
     */
    private void clearLoginAttempts(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisClient.deleteKey(KeyConstant.LOGIN_FAIL_KEY.getKey() + userId);
            redisClient.deleteKey(KeyConstant.LOGIN_LOCK_KEY.getKey() + userId);
        } catch (Exception e) {
            log.error("用户管理-清理登录失败次数失败, userId={}", userId, e);
        }
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private Map<String, Long> loadRoleIdsByCodes(List<String> codes) {
        Map<String, Long> map = new HashMap<>();
        if (codes == null || codes.isEmpty()) {
            return map;
        }
        // 角色编号统一大写后匹配, 与角色表的大写规范化保持一致, MySQL/PostgreSQL 行为相同
        List<String> normalizedCodes = codes.stream()
                .filter(StrUtil::isNotBlank)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return map;
        }
        List<SysRole> roles = sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                .in("role_code", normalizedCodes)
                .eq("is_deleted", 0));
        for (SysRole role : roles) {
            map.put(role.getRoleCode().toUpperCase(Locale.ROOT), role.getRoleId());
        }
        return map;
    }

    private Map<String, Long> loadPostIdsByCodes(List<String> codes) {
        Map<String, Long> map = new HashMap<>();
        if (codes == null || codes.isEmpty()) {
            return map;
        }
        List<SysPost> posts = sysPostMapper.selectList(new QueryWrapper<SysPost>()
                .in("post_code", codes)
                .eq("is_deleted", 0));
        for (SysPost post : posts) {
            map.put(post.getPostCode(), post.getPostId());
        }
        return map;
    }

    private List<String> splitCodes(String value) {
        if (StrUtil.isBlank(value)) {
            return new ArrayList<>();
        }
        String normalized = value.replace('，', ',');
        return java.util.Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value.length() == 10) {
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String cellString(Row row, int index, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
                return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
            }
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < columnCount; i++) {
            if (StrUtil.isNotBlank(cellString(row, i, formatter))) {
                return false;
            }
        }
        return true;
    }

    private void writeHeader(Sheet sheet, Workbook workbook, String[] headers) {
        Row header = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private void setColumnWidths(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }
    }

    private void setCell(Row row, int index, String value) {
        row.createCell(index).setCellValue(value == null ? "" : value);
    }

    private String sexText(Integer sex) {
        if (Objects.equals(sex, 0)) {
            return "女";
        }
        if (Objects.equals(sex, 2)) {
            return "保密";
        }
        return "男";
    }

    private String statusText(Integer status) {
        if (Objects.equals(status, STATUS_DISABLED)) {
            return "禁用";
        }
        if (Objects.equals(status, STATUS_LOCKED)) {
            return "锁定";
        }
        return "正常";
    }

    private String joinNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        return String.join("，", names);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 导入时的用户暂存对象(全部校验通过后再落库)
     */
    private static class PreparedUser {
        private String username;
        private String nickname;
        private String password;
        private String phone;
        private String email;
        private Integer sex;
        private Long deptId;
        private LocalDateTime expiredTime;
        private String remark;
        private List<Long> roleIds;
        private List<Long> postIds;
    }
}
