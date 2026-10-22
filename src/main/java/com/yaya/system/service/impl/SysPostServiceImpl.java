package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.form.SysPostForm;
import com.yaya.system.domain.query.SysPostQuery;
import com.yaya.system.domain.vo.SysPostVo;
import com.yaya.system.mapper.SysPostMapper;
import com.yaya.system.service.SysPostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 岗位管理 Service 实现
 *
 * @author yaya
 */
@Service
@Transactional
public class SysPostServiceImpl implements SysPostService {

    /** 状态: 正常 */
    private static final int STATUS_ENABLED = 1;

    /** 状态: 停用 */
    private static final int STATUS_DISABLED = 0;

    @Resource
    private SysPostMapper sysPostMapper;

    @Override
    public PageDTO<SysPostVo> getPostPage(SysPostQuery query) {
        Page<SysPostVo> page = sysPostMapper.getPostPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getPostName(), query.getPostCode(), query.getStatus(), query.getDeptId());
        PageDTO<SysPostVo> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public List<SysPost> getPostList() {
        return sysPostMapper.selectList(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getIsDeleted, 0)
                .orderByAsc(SysPost::getPostId));
    }

    @Override
    public SysPostVo getPostDetail(Long postId) {
        if (postId == null) {
            throw new GlobalCommonException("岗位ID不能为空");
        }
        SysPostVo postVo = sysPostMapper.getPostDetail(postId);
        if (postVo == null) {
            throw new GlobalCommonException("岗位不存在");
        }
        return postVo;
    }

    @Override
    public boolean addPost(SysPostForm form) {
        String postCode = StrUtil.trim(form.getPostCode());
        checkPostCodeUnique(postCode, null);
        SysPost post = new SysPost();
        post.setPostName(StrUtil.trim(form.getPostName()));
        post.setPostCode(postCode);
        // 所属部门取创建人部门快照, 岗位表单不提供部门选择
        post.setDeptId(SecurityUtils.getDeptId());
        post.setStatus(form.getStatus() == null ? STATUS_ENABLED : form.getStatus());
        post.setIsDeleted(0);
        post.setRemark(form.getRemark());
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        post.setCreateBy(userId);
        post.setUpdateBy(userId);
        post.setCreateTime(now);
        post.setUpdateTime(now);
        return sysPostMapper.insert(post) > 0;
    }

    @Override
    public boolean updatePost(SysPostForm form) {
        if (form.getPostId() == null) {
            throw new GlobalCommonException("岗位ID不能为空");
        }
        SysPostVo existing = getPostDetail(form.getPostId());
        String postCode = StrUtil.trim(form.getPostCode());
        checkPostCodeUnique(postCode, form.getPostId());

        Integer effectiveStatus = form.getStatus() == null ? existing.getStatus() : form.getStatus();
        checkDisableWhenUsersExist(form.getPostId(), existing.getStatus(), effectiveStatus);

        // 显式 set 所有可编辑字段, 支持清空备注; 部门快照创建后不允许修改
        LambdaUpdateWrapper<SysPost> wrapper = new LambdaUpdateWrapper<SysPost>()
                .eq(SysPost::getPostId, form.getPostId())
                .set(SysPost::getPostName, StrUtil.trim(form.getPostName()))
                .set(SysPost::getPostCode, postCode)
                .set(SysPost::getStatus, effectiveStatus)
                .set(SysPost::getRemark, form.getRemark())
                .set(SysPost::getUpdateBy, SecurityUtils.getUserId())
                .set(SysPost::getUpdateTime, LocalDateTime.now());
        return sysPostMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean deletePost(Long postId) {
        getPostDetail(postId);
        if (sysPostMapper.countActiveUsers(postId) > 0) {
            throw new GlobalCommonException("岗位已分配给用户，不允许删除");
        }
        return sysPostMapper.logicDeleteById(
                postId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
    }

    @Override
    public boolean updatePostStatus(Long postId, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new GlobalCommonException("岗位状态值不正确");
        }
        SysPostVo existing = getPostDetail(postId);
        checkDisableWhenUsersExist(postId, existing.getStatus(), status);
        SysPost post = new SysPost();
        post.setPostId(postId);
        post.setStatus(status);
        post.setUpdateBy(SecurityUtils.getUserId());
        post.setUpdateTime(LocalDateTime.now());
        return sysPostMapper.updateById(post) > 0;
    }

    /**
     * 岗位编号全局唯一校验(包含已逻辑删除记录, 与数据库唯一索引一致)
     */
    private void checkPostCodeUnique(String postCode, Long excludePostId) {
        if (StrUtil.isBlank(postCode)) {
            throw new GlobalCommonException("岗位编号不能为空");
        }
        if (sysPostMapper.countByPostCode(postCode, excludePostId) > 0) {
            throw new GlobalCommonException("岗位编号已存在");
        }
    }

    /**
     * 停用校验: 只要岗位关联了未删除用户, 就不允许停用
     * 已停用的岗位重复停用不做拦截
     */
    private void checkDisableWhenUsersExist(Long postId, Integer currentStatus, Integer targetStatus) {
        boolean disableTransition = Objects.equals(targetStatus, STATUS_DISABLED)
                && !Objects.equals(currentStatus, STATUS_DISABLED);
        if (disableTransition && sysPostMapper.countActiveUsers(postId) > 0) {
            throw new GlobalCommonException("岗位已分配给用户，不允许停用");
        }
    }
}
