package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.vo.SysRoleVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 角色表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysRoleMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 角色分页(含部门名称、创建人名称)
     *
     * @param page     分页参数
     * @param roleName 角色名称(模糊)
     * @param roleCode 角色编号(模糊)
     * @param status   状态
     * @return 分页数据
     */
    Page<SysRoleVo> getRolePage(Page<SysRoleVo> page,
                                @Param("roleName") String roleName,
                                @Param("roleCode") String roleCode,
                                @Param("status") Integer status);

    /**
     * 角色详情(含部门名称、创建人名称)
     *
     * @param roleId 角色ID
     * @return 角色详情, 不存在或已删除返回 null
     */
    SysRoleVo getRoleDetail(@Param("roleId") Long roleId);

    /**
     * 统计角色编号占用数量(包含已逻辑删除记录, 与数据库 role_code 唯一索引一致)
     *
     * @param roleCode      角色编号
     * @param excludeRoleId 排除的角色ID(更新时排除自身), 可为空
     * @return 占用数量
     */
    int countByRoleCode(@Param("roleCode") String roleCode,
                        @Param("excludeRoleId") Long excludeRoleId);

    /**
     * 统计角色关联的未删除用户数
     *
     * @param roleId 角色ID
     * @return 未删除用户数
     */
    int countUsersByRole(@Param("roleId") Long roleId);

    /**
     * 逻辑删除角色, 同时维护审计字段
     *
     * @param roleId     角色ID
     * @param updateBy   更新人ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int logicDeleteById(@Param("roleId") Long roleId,
                        @Param("updateBy") Long updateBy,
                        @Param("updateTime") LocalDateTime updateTime);
}
