package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.form.SysNoticeForm;
import com.yaya.system.domain.query.SysMyNoticeQuery;
import com.yaya.system.domain.query.SysNoticeQuery;
import com.yaya.system.domain.vo.SysNoticeVo;

/**
 * 公告 Service
 *
 * @author yaya
 */
public interface SysNoticeService {

    /**
     * 管理端公告分页(仅管理员)
     */
    PageDTO<SysNoticeVo> getNoticePage(SysNoticeQuery query);

    /**
     * 我的公告分页(当前用户接收的已发布公告)
     */
    PageDTO<SysNoticeVo> getMyNoticePage(SysMyNoticeQuery query);

    /**
     * 管理端公告详情
     */
    SysNoticeVo getNoticeDetail(Long noticeId);

    /**
     * 我的消息详情(仅当前用户接收的已发布公告)
     */
    SysNoticeVo getMyNoticeDetail(Long noticeId);

    /**
     * 新增公告(草稿)
     *
     * @return 公告ID
     */
    Long addNotice(SysNoticeForm form);

    /**
     * 更新公告
     */
    boolean updateNotice(SysNoticeForm form);

    /**
     * 逻辑删除公告
     */
    boolean deleteNotice(Long noticeId);

    /**
     * 发布公告(仅草稿可发布)
     */
    boolean publishNotice(Long noticeId);

    /**
     * 置顶/取消置顶
     */
    boolean updateNoticeTop(Long noticeId, Integer isTop);

    /**
     * 标记当前用户已读
     */
    boolean markNoticeRead(Long noticeId);

    /**
     * 当前用户全部公告标记已读
     */
    boolean markAllNoticeRead();

    /**
     * 当前用户未读公告数量
     */
    Long getUnreadNoticeCount();
}
