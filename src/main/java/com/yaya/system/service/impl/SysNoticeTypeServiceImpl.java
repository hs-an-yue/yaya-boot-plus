package com.yaya.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysNotice;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.query.SysNoticeTypeQuery;
import com.yaya.system.mapper.SysNoticeMapper;
import com.yaya.system.mapper.SysNoticeTypeMapper;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysNoticeTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 公告类型 Service 实现
 *
 * @author yaya
 */
@Service
@Transactional
public class SysNoticeTypeServiceImpl implements SysNoticeTypeService {

    @Resource
    private SysNoticeTypeMapper sysNoticeTypeMapper;

    @Resource
    private SysNoticeMapper sysNoticeMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public List<SysNoticeType> getNoticeTypeList() {
        List<SysNoticeType> types = sysNoticeTypeMapper.selectList(new LambdaQueryWrapper<SysNoticeType>()
                .eq(SysNoticeType::getIsDeleted, 0)
                .orderByDesc(SysNoticeType::getCreateTime)
                .orderByDesc(SysNoticeType::getNoticeTypeId));
        fillCreateByName(types);
        return types;
    }

    @Override
    public PageDTO<SysNoticeType> getNoticeTypePage(SysNoticeTypeQuery query) {
        QueryWrapper<SysNoticeType> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getNoticeTypeName()),
                        "notice_type_name", query.getNoticeTypeName())
                .eq(query.getStatus() != null, "status", query.getStatus())
                .orderByDesc("create_time")
                .orderByDesc("notice_type_id");
        Page<SysNoticeType> page = sysNoticeTypeMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        fillCreateByName(page.getRecords());

        PageDTO<SysNoticeType> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public SysNoticeType getNoticeTypeDetail(Long noticeTypeId) {
        SysNoticeType noticeType = sysNoticeTypeMapper.selectById(noticeTypeId);
        if (noticeType == null || Objects.equals(noticeType.getIsDeleted(), 1)) {
            throw new GlobalCommonException("公告类型不存在");
        }
        fillCreateByName(List.of(noticeType));
        return noticeType;
    }

    /**
     * 填充创建人名称
     */
    private void fillCreateByName(List<SysNoticeType> types) {
        Map<Long, String> cache = new HashMap<>();
        for (SysNoticeType type : types) {
            type.setCreateByName(resolveCreateByName(type.getCreateBy(), cache));
        }
    }

    private String resolveCreateByName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        String cached = cache.get(userId);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            cache.put(userId, "");
            return null;
        }
        String name = StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
        name = StrUtil.blankToDefault(name, "");
        cache.put(userId, name);
        return name.isEmpty() ? null : name;
    }

    @Override
    public boolean addNoticeType(SysNoticeType noticeType) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        noticeType.setNoticeTypeId(null);
        noticeType.setStatus(noticeType.getStatus() == null ? 1 : noticeType.getStatus());
        noticeType.setIsDeleted(0);
        noticeType.setDeptId(SecurityUtils.getDeptId());
        noticeType.setCreateBy(userId);
        noticeType.setUpdateBy(userId);
        noticeType.setCreateTime(now);
        noticeType.setUpdateTime(now);
        return sysNoticeTypeMapper.insert(noticeType) > 0;
    }

    @Override
    public boolean updateNoticeType(SysNoticeType noticeType) {
        if (noticeType.getNoticeTypeId() == null) {
            throw new GlobalCommonException("公告类型ID不能为空");
        }
        // 校验记录存在且未删除
        getNoticeTypeDetail(noticeType.getNoticeTypeId());
        // 只允许修改名称和状态; 数据权限字段(dept_id/create_by)、创建时间和逻辑删除标识不允许被前端篡改
        SysNoticeType update = new SysNoticeType();
        update.setNoticeTypeId(noticeType.getNoticeTypeId());
        update.setNoticeTypeName(noticeType.getNoticeTypeName());
        update.setStatus(noticeType.getStatus());
        update.setUpdateBy(SecurityUtils.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        return sysNoticeTypeMapper.updateById(update) > 0;
    }

    @Override
    public boolean deleteNoticeType(Long noticeTypeId) {
        getNoticeTypeDetail(noticeTypeId);
        Long noticeCount = sysNoticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getNoticeTypeId, noticeTypeId));
        if (noticeCount != null && noticeCount > 0) {
            throw new GlobalCommonException("公告类型已被公告引用，不允许删除");
        }
        return sysNoticeTypeMapper.logicDeleteById(
                noticeTypeId, SecurityUtils.getUserId(), LocalDateTime.now()) > 0;
    }

    @Override
    public boolean updateNoticeTypeStatus(Long noticeTypeId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new GlobalCommonException("公告类型状态值不正确");
        }
        getNoticeTypeDetail(noticeTypeId);
        SysNoticeType noticeType = new SysNoticeType();
        noticeType.setNoticeTypeId(noticeTypeId);
        noticeType.setStatus(status);
        noticeType.setUpdateBy(SecurityUtils.getUserId());
        noticeType.setUpdateTime(LocalDateTime.now());
        return sysNoticeTypeMapper.updateById(noticeType) > 0;
    }
}
