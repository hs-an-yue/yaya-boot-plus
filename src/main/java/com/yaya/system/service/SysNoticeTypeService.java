package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.query.SysNoticeTypeQuery;

import java.util.List;

/**
 * 公告类型 Service 接口
 *
 * @author yaya
 */
public interface SysNoticeTypeService {

    /**
     * 查询公告类型列表
     */
    List<SysNoticeType> getNoticeTypeList();

    /**
     * 分页查询公告类型
     */
    PageDTO<SysNoticeType> getNoticeTypePage(SysNoticeTypeQuery query);

    /**
     * 查询公告类型详情
     *
     * @param noticeTypeId 公告类型ID
     */
    SysNoticeType getNoticeTypeDetail(Long noticeTypeId);

    /**
     * 新增公告类型
     */
    boolean addNoticeType(SysNoticeType noticeType);

    /**
     * 更新公告类型
     */
    boolean updateNoticeType(SysNoticeType noticeType);

    /**
     * 删除公告类型(逻辑删除)
     *
     * @param noticeTypeId 公告类型ID
     */
    boolean deleteNoticeType(Long noticeTypeId);

    /**
     * 启用/停用公告类型
     */
    boolean updateNoticeTypeStatus(Long noticeTypeId, Integer status);
}
