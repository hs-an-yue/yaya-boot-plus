package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.form.SysRoleForm;
import com.yaya.system.domain.query.SysRoleQuery;
import com.yaya.system.domain.vo.SysRoleVo;

import java.util.List;

/**
 * 角色管理 Service
 *
 * @author yaya
 */
public interface SysRoleService {

    /**
     * 角色分页
     *
     * @param query 查询条件
     * @return 分页数据
     */
    PageDTO<SysRoleVo> getRolePage(SysRoleQuery query);

    /**
     * 角色列表(用户分配使用, 包含停用角色, 前端按 status 过滤)
     *
     * @return 未删除的角色列表
     */
    List<SysRole> getRoleList();

    /**
     * 角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    SysRoleVo getRoleDetail(Long roleId);

    /**
     * 新增角色
     *
     * @param form 角色表单
     * @return 是否成功
     */
    boolean addRole(SysRoleForm form);

    /**
     * 更新角色
     * 角色编号创建后不允许修改; ROOT 角色不允许停用或降权
     *
     * @param form 角色表单
     * @return 是否成功
     */
    boolean updateRole(SysRoleForm form);

    /**
     * 删除角色(逻辑删除)
     * ROOT 角色不允许删除; 已分配给未删除用户时不允许删除
     *
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean deleteRole(Long roleId);

    /**
     * 启用/停用角色
     * ROOT 角色不允许停用
     *
     * @param roleId 角色ID
     * @param status 状态(1-正常,0-停用)
     * @return 是否成功
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 查询角色已授权的菜单ID
     * ROOT 返回全部未删除菜单ID
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 保存角色菜单授权
     * ROOT 角色不允许修改菜单授权
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    boolean saveRoleMenu(Long roleId, List<Long> menuIds);

    /**
     * 查询角色自定义数据权限的部门ID
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> getRoleDeptIds(Long roleId);

    /**
     * 保存角色自定义数据权限的部门
     * 仅 data_scope=5 的角色允许配置
     *
     * @param roleId  角色ID
     * @param deptIds 部门ID列表
     * @return 是否成功
     */
    boolean saveRoleDept(Long roleId, List<Long> deptIds);
}
