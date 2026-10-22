package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysNotice;
import com.yaya.system.domain.entity.SysNoticeDept;
import com.yaya.system.domain.entity.SysNoticeFile;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.entity.SysNoticeUser;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.SysNoticeForm;
import com.yaya.system.domain.query.SysMyNoticeQuery;
import com.yaya.system.domain.query.SysNoticeQuery;
import com.yaya.system.domain.vo.SysNoticeAttachmentVo;
import com.yaya.system.domain.vo.SysNoticeVo;
import com.yaya.system.mapper.SysDeptMapper;
import com.yaya.system.mapper.SysFileMapper;
import com.yaya.system.mapper.SysNoticeDeptMapper;
import com.yaya.system.mapper.SysNoticeFileMapper;
import com.yaya.system.mapper.SysNoticeMapper;
import com.yaya.system.mapper.SysNoticeTypeMapper;
import com.yaya.system.mapper.SysNoticeUserMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysNoticeService;
import com.yaya.system.util.NoticeHtmlSanitizer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告 Service 实现
 *
 * @author yaya
 */
@Slf4j
@Service
@Transactional
public class SysNoticeServiceImpl implements SysNoticeService {

    /** 发布状态: 草稿 */
    private static final int STATUS_DRAFT = 0;
    /** 发布状态: 已发布 */
    private static final int STATUS_PUBLISHED = 1;

    /** 发布范围: 全员 */
    private static final int SCOPE_ALL = 1;
    /** 发布范围: 指定部门 */
    private static final int SCOPE_DEPT = 2;
    /** 发布范围: 指定用户 */
    private static final int SCOPE_USERS = 3;

    /** 单个公告最大附件数 */
    private static final int MAX_ATTACHMENT_COUNT = 20;

    @Resource
    private SysNoticeMapper sysNoticeMapper;

    @Resource
    private SysNoticeTypeMapper sysNoticeTypeMapper;

    @Resource
    private SysNoticeUserMapper sysNoticeUserMapper;

    @Resource
    private SysNoticeDeptMapper sysNoticeDeptMapper;

    @Resource
    private SysNoticeFileMapper sysNoticeFileMapper;

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public PageDTO<SysNoticeVo> getNoticePage(SysNoticeQuery query) {
        Page<SysNoticeVo> page = sysNoticeMapper.getNoticePage(new Page<>(query.getPageNum(), query.getPageSize()),query.getNoticeTitle(), query.getNoticeTypeId(), query.getNoticeLevel(), query.getStatus());
        return toPageDTO(page);
    }

    @Override
    public PageDTO<SysNoticeVo> getMyNoticePage(SysMyNoticeQuery query) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new GlobalCommonException("当前用户未登录");
        }
        Page<SysNoticeVo> page = sysNoticeMapper.getUserNoticePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                userId, null, null, null, false);
        page.getRecords().forEach(this::fillNoticeSummary);
        return toPageDTO(page);
    }

    /**
     * 我的消息列表只返回纯文本摘要, 不把完整富文本带到列表响应
     */
    private void fillNoticeSummary(SysNoticeVo noticeVo) {
        String html = noticeVo.getNoticeContent();
        if (StrUtil.isBlank(html)) {
            noticeVo.setNoticeSummary("");
        } else {
            String text = HtmlUtil.unescape(HtmlUtil.cleanHtmlTag(html));
            text = text.replaceAll("\\s+", " ").trim();
            noticeVo.setNoticeSummary(StrUtil.maxLength(text, 160));
        }
        noticeVo.setNoticeContent(null);
    }

    private PageDTO<SysNoticeVo> toPageDTO(Page<SysNoticeVo> page) {
        PageDTO<SysNoticeVo> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public SysNoticeVo getNoticeDetail(Long noticeId) {
        if (!isAdmin()) {
            throw new GlobalCommonException("无权访问公告管理详情");
        }
        if (noticeId == null) {
            throw new GlobalCommonException("公告ID不能为空");
        }
        SysNoticeVo noticeVo = sysNoticeMapper.getNoticeDetail(noticeId);
        if (noticeVo == null) {
            throw new GlobalCommonException("公告不存在");
        }
        noticeVo.setNoticeContent(NoticeHtmlSanitizer.sanitize(noticeVo.getNoticeContent()));
        noticeVo.setAttachments(getAttachments(noticeId));
        fillTargetIds(noticeVo);
        return noticeVo;
    }

    @Override
    public SysNoticeVo getMyNoticeDetail(Long noticeId) {
        if (noticeId == null) {
            throw new GlobalCommonException("公告ID不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new GlobalCommonException("当前用户未登录");
        }
        SysNoticeVo noticeVo = sysNoticeMapper.getUserNoticeDetail(noticeId, userId);
        if (noticeVo == null) {
            throw new GlobalCommonException("公告不存在或无权查看");
        }
        noticeVo.setAttachments(getAttachments(noticeId));
        noticeVo.setNoticeContent(NoticeHtmlSanitizer.sanitize(noticeVo.getNoticeContent()));
        return noticeVo;
    }

    @Override
    public Long addNotice(SysNoticeForm form) {
        String title = validateFormAndGetTitle(form);
        String content = sanitizeNoticeContent(form.getNoticeContent());
        getNoticeType(form.getNoticeTypeId(), false);
        List<Long> deptIds = normalizeIds(form.getDeptIds());
        List<Long> userIds = normalizeIds(form.getUserIds());
        List<Long> fileIds = normalizeIds(form.getAttachmentFileIds());
        List<SysUser> scopeUsers = validateScope(form.getPublishScope(), deptIds, userIds, true);
        validateAttachments(fileIds);

        Long operatorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle(title);
        notice.setNoticeContent(content);
        notice.setNoticeLevel(form.getNoticeLevel());
        notice.setNoticeTypeId(form.getNoticeTypeId());
        notice.setIsTop(form.getIsTop() == null ? 0 : form.getIsTop());
        notice.setStatus(STATUS_DRAFT);
        notice.setPublishScope(form.getPublishScope());
        notice.setDeptId(SecurityUtils.getDeptId());
        notice.setCreateBy(operatorId);
        notice.setUpdateBy(operatorId);
        notice.setCreateTime(now);
        notice.setUpdateTime(now);
        notice.setIsDeleted(0);
        if (sysNoticeMapper.insert(notice) <= 0) {
            throw new GlobalCommonException("新增公告失败");
        }
        saveTargets(notice.getNoticeId(), form.getPublishScope(), deptIds, scopeUsers);
        saveAttachments(notice.getNoticeId(), fileIds);
        return notice.getNoticeId();
    }

    @Override
    public boolean updateNotice(SysNoticeForm form) {
        if (form.getNoticeId() == null) {
            throw new GlobalCommonException("公告ID不能为空");
        }
        SysNotice existing = getExistingNotice(form.getNoticeId());
        String title = validateFormAndGetTitle(form);
        String content = sanitizeNoticeContent(form.getNoticeContent());
        getNoticeType(form.getNoticeTypeId(), false);
        int scope = form.getPublishScope();
        List<Long> deptIds = normalizeIds(form.getDeptIds());
        List<Long> userIds = normalizeIds(form.getUserIds());
        List<SysUser> scopeUsers = new ArrayList<>();
        if (Objects.equals(existing.getStatus(), STATUS_PUBLISHED)) {
            if (!Objects.equals(existing.getPublishScope(), scope)) {
                throw new GlobalCommonException("已发布公告不允许修改发布范围");
            }
            if (scope == SCOPE_DEPT && form.getDeptIds() != null
                    && !sameIds(sysNoticeMapper.getTargetDeptIds(existing.getNoticeId()), deptIds)) {
                throw new GlobalCommonException("已发布公告不允许修改接收部门");
            }
            if (scope == SCOPE_USERS && form.getUserIds() != null
                    && !sameIds(sysNoticeMapper.getTargetUserIds(existing.getNoticeId()), userIds)) {
                throw new GlobalCommonException("已发布公告不允许修改接收用户");
            }
        } else {
            scopeUsers = validateScope(scope, deptIds, userIds, true);
        }
        List<Long> fileIds = normalizeIds(form.getAttachmentFileIds());
        validateAttachments(fileIds);

        Long operatorId = SecurityUtils.getUserId();
        SysNotice update = new SysNotice();
        update.setNoticeId(existing.getNoticeId());
        update.setNoticeTitle(title);
        update.setNoticeContent(content);
        update.setNoticeLevel(form.getNoticeLevel());
        update.setNoticeTypeId(form.getNoticeTypeId());
        update.setIsTop(form.getIsTop() == null ? existing.getIsTop() : form.getIsTop());
        update.setUpdateBy(operatorId);
        update.setUpdateTime(LocalDateTime.now());
        if (!Objects.equals(existing.getStatus(), STATUS_PUBLISHED)) {
            update.setPublishScope(scope);
        }
        if (sysNoticeMapper.updateById(update) <= 0) {
            throw new GlobalCommonException("更新公告失败");
        }
        if (!Objects.equals(existing.getStatus(), STATUS_PUBLISHED)) {
            saveTargets(existing.getNoticeId(), scope, deptIds, scopeUsers);
        }
        if (form.getAttachmentFileIds() != null) {
            saveAttachments(existing.getNoticeId(), fileIds);
        }
        return true;
    }

    @Override
    public boolean deleteNotice(Long noticeId) {
        getExistingNotice(noticeId);
        return sysNoticeMapper.logicDeleteById(
                noticeId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
    }

    @Override
    public boolean publishNotice(Long noticeId) {
        SysNotice notice = getExistingNotice(noticeId);
        if (Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            throw new GlobalCommonException("公告已发布，不允许重复发布");
        }
        getNoticeType(notice.getNoticeTypeId(), true);
        List<SysNoticeUser> recipients = collectRecipients(notice);
        if (recipients.isEmpty()) {
            throw new GlobalCommonException("没有可接收该公告的用户");
        }

        Long operatorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        SysNotice update = new SysNotice();
        update.setNoticeId(noticeId);
        update.setStatus(STATUS_PUBLISHED);
        update.setPublishTime(now);
        update.setUpdateBy(operatorId);
        update.setUpdateTime(now);
        if (sysNoticeMapper.updateById(update) <= 0) {
            throw new GlobalCommonException("发布公告失败");
        }
        // 发布时重新生成接收人, 草稿阶段指定的用户记录会被覆盖并重置未读状态
        sysNoticeUserMapper.delete(new QueryWrapper<SysNoticeUser>()
                .eq("notice_id", noticeId));
        for (SysNoticeUser recipient : recipients) {
            sysNoticeUserMapper.insert(recipient);
        }
        return true;
    }

    @Override
    public boolean updateNoticeTop(Long noticeId, Integer isTop) {
        if (isTop == null || (isTop != 0 && isTop != 1)) {
            throw new GlobalCommonException("置顶状态值不正确");
        }
        getExistingNotice(noticeId);
        SysNotice update = new SysNotice();
        update.setNoticeId(noticeId);
        update.setIsTop(isTop);
        update.setUpdateBy(SecurityUtils.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        return sysNoticeMapper.updateById(update) > 0;
    }

    @Override
    public boolean markNoticeRead(Long noticeId) {
        if (noticeId == null) {
            throw new GlobalCommonException("公告ID不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new GlobalCommonException("当前用户未登录");
        }
        SysNotice notice = getExistingNotice(noticeId);
        if (!Objects.equals(notice.getStatus(), STATUS_PUBLISHED)) {
            throw new GlobalCommonException("公告未发布，不能标记已读");
        }
        SysNoticeUser noticeUser = sysNoticeUserMapper.selectOne(new QueryWrapper<SysNoticeUser>()
                .eq("notice_id", noticeId)
                .eq("user_id", userId));
        if (noticeUser == null) {
            // 管理员查看管理端详情时可能不是接收人, 读取状态对其无意义
            if (isAdmin()) {
                return true;
            }
            throw new GlobalCommonException("公告不存在或无权阅读");
        }
        if (Objects.equals(noticeUser.getIsRead(), 1)) {
            return true;
        }
        return sysNoticeUserMapper.update(null, new LambdaUpdateWrapper<SysNoticeUser>()
                .eq(SysNoticeUser::getNoticeId, noticeId)
                .eq(SysNoticeUser::getUserId, userId)
                .set(SysNoticeUser::getIsRead, 1)
                .set(SysNoticeUser::getReadTime, LocalDateTime.now())) > 0;
    }

    @Override
    public boolean markAllNoticeRead() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new GlobalCommonException("当前用户未登录");
        }
        sysNoticeMapper.markAllRead(userId, LocalDateTime.now());
        return true;
    }

    @Override
    public Long getUnreadNoticeCount() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return 0L;
        }
        Long count = sysNoticeMapper.countUnreadNotice(userId);
        return count == null ? 0L : count;
    }

    // ==================== 私有方法 ====================

    private SysNotice getExistingNotice(Long noticeId) {
        if (noticeId == null) {
            throw new GlobalCommonException("公告ID不能为空");
        }
        SysNotice notice = sysNoticeMapper.selectById(noticeId);
        if (notice == null || Objects.equals(notice.getIsDeleted(), 1)) {
            throw new GlobalCommonException("公告不存在");
        }
        return notice;
    }

    private String validateFormAndGetTitle(SysNoticeForm form) {
        String title = StrUtil.trim(form.getNoticeTitle());
        if (StrUtil.isBlank(title)) {
            throw new GlobalCommonException("公告标题不能为空");
        }
        if (title.length() > 200) {
            throw new GlobalCommonException("公告标题不能超过200个字符");
        }
        if (StrUtil.isBlank(form.getNoticeContent())) {
            throw new GlobalCommonException("公告内容不能为空");
        }
        if (form.getNoticeLevel() == null || form.getNoticeLevel() < 0 || form.getNoticeLevel() > 2) {
            throw new GlobalCommonException("公告级别值不正确");
        }
        if (form.getIsTop() != null && form.getIsTop() != 0 && form.getIsTop() != 1) {
            throw new GlobalCommonException("置顶状态值不正确");
        }
        if (form.getPublishScope() == null
                || form.getPublishScope() < SCOPE_ALL
                || form.getPublishScope() > SCOPE_USERS) {
            throw new GlobalCommonException("发布范围值不正确");
        }
        return title;
    }

    /**
     * 富文本落库前做白名单清洗; 只有脚本/滥用标签的内容按空内容拒绝
     */
    private String sanitizeNoticeContent(String content) {
        String sanitized = NoticeHtmlSanitizer.sanitize(content);
        if (StrUtil.isBlank(sanitized)) {
            throw new GlobalCommonException("公告内容不能为空");
        }
        return sanitized;
    }

    private SysNoticeType getNoticeType(Long noticeTypeId, boolean requireEnabled) {
        if (noticeTypeId == null) {
            throw new GlobalCommonException("公告类型不能为空");
        }
        SysNoticeType noticeType = sysNoticeTypeMapper.selectById(noticeTypeId);
        if (noticeType == null || Objects.equals(noticeType.getIsDeleted(), 1)) {
            throw new GlobalCommonException("公告类型不存在");
        }
        if (requireEnabled && !Objects.equals(noticeType.getStatus(), 1)) {
            throw new GlobalCommonException("公告类型已停用");
        }
        return noticeType;
    }

    /**
     * 校验发布范围; 返回指定用户时的用户列表(含部门快照)
     */
    private List<SysUser> validateScope(Integer scope, List<Long> deptIds, List<Long> userIds,
                                        boolean requireTargets) {
        if (scope == null) {
            throw new GlobalCommonException("发布范围不能为空");
        }
        if (scope == SCOPE_ALL) {
            return new ArrayList<>();
        }
        if (scope == SCOPE_DEPT) {
            if (deptIds.isEmpty()) {
                if (requireTargets) {
                    throw new GlobalCommonException("请选择发布部门");
                }
                return new ArrayList<>();
            }
            validateDepts(deptIds);
            return new ArrayList<>();
        }
        if (scope == SCOPE_USERS) {
            if (userIds.isEmpty()) {
                if (requireTargets) {
                    throw new GlobalCommonException("请选择接收用户");
                }
                return new ArrayList<>();
            }
            return validateUsers(userIds);
        }
        throw new GlobalCommonException("发布范围值不正确");
    }

    private List<SysDept> validateDepts(List<Long> deptIds) {
        List<SysDept> depts = sysDeptMapper.selectList(new QueryWrapper<SysDept>()
                .in("dept_id", deptIds));
        if (depts.size() != deptIds.size()) {
            throw new GlobalCommonException("发布部门不存在");
        }
        for (SysDept dept : depts) {
            if (Objects.equals(dept.getIsDeleted(), 1)) {
                throw new GlobalCommonException("发布部门不存在");
            }
            if (!Objects.equals(dept.getStatus(), 1)) {
                throw new GlobalCommonException("发布部门已停用");
            }
        }
        return depts;
    }

    private List<SysUser> validateUsers(List<Long> userIds) {
        List<SysUser> users = sysUserMapper.selectList(new QueryWrapper<SysUser>()
                .in("user_id", userIds)
                .eq("is_enabled", 1)
                .eq("is_locked", 0));
        if (users.size() != userIds.size()) {
            throw new GlobalCommonException("存在无效或已停用的用户");
        }
        return users;
    }

    private void validateAttachments(List<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        if (fileIds.size() > MAX_ATTACHMENT_COUNT) {
            throw new GlobalCommonException("附件数量不能超过" + MAX_ATTACHMENT_COUNT + "个");
        }
        Long count = sysFileMapper.selectCount(new QueryWrapper<SysFile>()
                .in("file_id", fileIds));
        if (count == null || count.intValue() != fileIds.size()) {
            throw new GlobalCommonException("附件不存在");
        }
    }

    /**
     * 保存草稿的目标范围:
     * 部门范围只保存部门, 发布时展开为接收用户;
     * 用户范围直接保存接收用户, 发布时复用并重新校验;
     * 全员范围不保存目标记录.
     */
    private void saveTargets(Long noticeId, Integer scope, List<Long> deptIds,
                             List<SysUser> scopeUsers) {
        sysNoticeDeptMapper.delete(new QueryWrapper<SysNoticeDept>()
                .eq("notice_id", noticeId));
        sysNoticeUserMapper.delete(new QueryWrapper<SysNoticeUser>()
                .eq("notice_id", noticeId));
        if (Objects.equals(scope, SCOPE_DEPT)) {
            for (Long deptId : deptIds) {
                SysNoticeDept noticeDept = new SysNoticeDept();
                noticeDept.setNoticeId(noticeId);
                noticeDept.setDeptId(deptId);
                sysNoticeDeptMapper.insert(noticeDept);
            }
        } else if (Objects.equals(scope, SCOPE_USERS)) {
            for (SysUser user : scopeUsers) {
                SysNoticeUser noticeUser = new SysNoticeUser();
                noticeUser.setNoticeId(noticeId);
                noticeUser.setUserId(user.getUserId());
                noticeUser.setDeptId(user.getDeptId());
                noticeUser.setIsRead(0);
                noticeUser.setReadTime(null);
                sysNoticeUserMapper.insert(noticeUser);
            }
        }
    }

    private void saveAttachments(Long noticeId, List<Long> fileIds) {
        sysNoticeFileMapper.delete(new QueryWrapper<SysNoticeFile>()
                .eq("notice_id", noticeId));
        for (Long fileId : fileIds) {
            SysNoticeFile noticeFile = new SysNoticeFile();
            noticeFile.setNoticeId(noticeId);
            noticeFile.setFileId(fileId);
            sysNoticeFileMapper.insert(noticeFile);
        }
    }

    /**
     * 发布时生成接收人快照. 指定部门按选中部门精确匹配, 不自动包含子部门.
     */
    private List<SysNoticeUser> collectRecipients(SysNotice notice) {
        List<SysUser> users;
        Integer scope = notice.getPublishScope();
        if (Objects.equals(scope, SCOPE_ALL)) {
            users = sysUserMapper.selectList(new QueryWrapper<SysUser>()
                    .eq("is_enabled", 1)
                    .eq("is_locked", 0));
        } else if (Objects.equals(scope, SCOPE_DEPT)) {
            List<Long> deptIds = sysNoticeMapper.getTargetDeptIds(notice.getNoticeId());
            if (deptIds.isEmpty()) {
                throw new GlobalCommonException("请选择发布部门");
            }
            validateDepts(deptIds);
            users = sysUserMapper.selectList(new QueryWrapper<SysUser>()
                    .in("dept_id", deptIds)
                    .eq("is_enabled", 1)
                    .eq("is_locked", 0));
        } else if (Objects.equals(scope, SCOPE_USERS)) {
            List<Long> userIds = sysNoticeMapper.getTargetUserIds(notice.getNoticeId());
            if (userIds.isEmpty()) {
                throw new GlobalCommonException("请选择接收用户");
            }
            users = validateUsers(userIds);
        } else {
            throw new GlobalCommonException("发布范围值不正确");
        }

        Map<Long, SysNoticeUser> recipientMap = new LinkedHashMap<>();
        for (SysUser user : users) {
            if (user.getUserId() == null || recipientMap.containsKey(user.getUserId())) {
                continue;
            }
            SysNoticeUser recipient = new SysNoticeUser();
            recipient.setNoticeId(notice.getNoticeId());
            recipient.setUserId(user.getUserId());
            recipient.setDeptId(user.getDeptId());
            recipient.setIsRead(0);
            recipient.setReadTime(null);
            recipientMap.put(user.getUserId(), recipient);
        }
        return new ArrayList<>(recipientMap.values());
    }

    private List<SysNoticeAttachmentVo> getAttachments(Long noticeId) {
        List<SysNoticeFile> relations = sysNoticeFileMapper.selectList(
                new QueryWrapper<SysNoticeFile>().eq("notice_id", noticeId)
                        .orderByAsc("file_id"));
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> fileIds = relations.stream().map(SysNoticeFile::getFileId).toList();
        List<SysFile> files = sysFileMapper.selectList(new QueryWrapper<SysFile>()
                .in("file_id", fileIds));
        Map<Long, SysFile> fileMap = files.stream()
                .collect(Collectors.toMap(SysFile::getFileId, file -> file, (a, b) -> a));
        List<SysNoticeAttachmentVo> attachments = new ArrayList<>();
        for (Long fileId : fileIds) {
            SysFile file = fileMap.get(fileId);
            if (file != null) {
                SysNoticeAttachmentVo attachment = new SysNoticeAttachmentVo();
                attachment.setFileId(file.getFileId());
                attachment.setFileName(file.getFileName());
                attachment.setFileExt(file.getFileExt());
                attachment.setFileServerUrl(file.getFileServerUrl());
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private void fillTargetIds(SysNoticeVo noticeVo) {
        noticeVo.setDeptIds(new ArrayList<>());
        noticeVo.setUserIds(new ArrayList<>());
        if (Objects.equals(noticeVo.getPublishScope(), SCOPE_DEPT)) {
            noticeVo.setDeptIds(sysNoticeMapper.getTargetDeptIds(noticeVo.getNoticeId()));
        } else if (Objects.equals(noticeVo.getPublishScope(), SCOPE_USERS)) {
            noticeVo.setUserIds(sysNoticeMapper.getTargetUserIds(noticeVo.getNoticeId()));
        }
    }

    private boolean isAdmin() {
        return Boolean.TRUE.equals(SecurityUtils.isRootOrOperation());
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private boolean sameIds(List<Long> left, List<Long> right) {
        Set<Long> leftSet = new HashSet<>(left == null ? List.of() : left);
        Set<Long> rightSet = new HashSet<>(right == null ? List.of() : right);
        return leftSet.equals(rightSet);
    }
}
