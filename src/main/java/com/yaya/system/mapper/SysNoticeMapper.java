package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysNotice;
import com.yaya.system.domain.vo.SysNoticeVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysNoticeMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    /**
     * 管理端公告分页(草稿 + 已发布)
     */
    @DataScope(userAlias = "n",userIdColumnName = "create_by",deptAlias = "n",deptIdColumnName = "dept_id")
    Page<SysNoticeVo> getNoticePage(Page<SysNoticeVo> page,
                                    @Param("noticeTitle") String noticeTitle,
                                    @Param("noticeTypeId") Long noticeTypeId,
                                    @Param("noticeLevel") Integer noticeLevel,
                                    @Param("status") Integer status);

    /**
     * 普通用户公告分页(仅已发布且为接收人)
     */
    Page<SysNoticeVo> getUserNoticePage(Page<SysNoticeVo> page,
                                        @Param("userId") Long userId,
                                        @Param("noticeTitle") String noticeTitle,
                                        @Param("noticeTypeId") Long noticeTypeId,
                                        @Param("noticeLevel") Integer noticeLevel,
                                        @Param("onlyUnread") Boolean onlyUnread);

    /**
     * 管理端公告详情(草稿 + 已发布)
     */
    SysNoticeVo getNoticeDetail(@Param("noticeId") Long noticeId);

    /**
     * 普通用户公告详情(仅已发布且为接收人)
     */
    SysNoticeVo getUserNoticeDetail(@Param("noticeId") Long noticeId,
                                    @Param("userId") Long userId);

    /**
     * 公告指定部门ID
     */
    List<Long> getTargetDeptIds(@Param("noticeId") Long noticeId);

    /**
     * 公告指定用户ID
     */
    List<Long> getTargetUserIds(@Param("noticeId") Long noticeId);

    /**
     * 逻辑删除公告
     */
    int logicDeleteById(@Param("noticeId") Long noticeId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 当前用户未读公告数量
     */
    Long countUnreadNotice(@Param("userId") Long userId);

    /**
     * 当前用户全部未读公告标记为已读
     */
    int markAllRead(@Param("userId") Long userId,
                    @Param("readTime") LocalDateTime readTime);
}
