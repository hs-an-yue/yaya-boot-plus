package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysNoticeType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 公告类型表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysNoticeTypeMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysNoticeTypeMapper extends BaseMapper<SysNoticeType> {

    /**
     * 消息类型分页
     * @param page 分页信息
     * @param queryWrapper 条件
     * @return 页数据
     */
    @DataScope(userAlias = "nt",userIdColumnName = "create_by",deptAlias = "nt",deptIdColumnName = "dept_id")
    IPage<SysNoticeType> selectNoticeTypePage(Page<SysNoticeType> page, @Param(Constants.WRAPPER) Wrapper<SysNoticeType> queryWrapper);

    /**
     * 逻辑删除公告类型, 同时维护审计字段
     *
     * @param noticeTypeId 公告类型ID
     * @param updateBy     更新人ID
     * @param updateTime   更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("noticeTypeId") Long noticeTypeId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
