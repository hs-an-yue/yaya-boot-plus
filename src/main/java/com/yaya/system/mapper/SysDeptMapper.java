package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yaya.system.domain.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 部门表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysDeptMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 逻辑删除部门, 同时维护审计字段
     *
     * @param deptId     部门ID
     * @param updateBy   更新人ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("deptId") Long deptId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 按ID查询部门(包含逻辑删除记录), 用于登录时区分部门删除/停用状态
     *
     * @param deptId 部门ID
     * @return 部门
     */
    SysDept selectByIdIncludeDeleted(@Param("deptId") Long deptId);
}
