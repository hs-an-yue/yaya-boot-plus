package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.form.SysRoleDeptForm;
import com.yaya.system.domain.form.SysRoleForm;
import com.yaya.system.domain.form.SysRoleMenuForm;
import com.yaya.system.domain.query.SysRoleQuery;
import com.yaya.system.domain.vo.SysRoleVo;
import com.yaya.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    @Operation(summary = "角色分页")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:search')")
    @PostMapping("/getRolePage")
    public Result<PageDTO<SysRoleVo>> getRolePage(@RequestBody @Valid SysRoleQuery query) {
        return Result.ok(sysRoleService.getRolePage(query));
    }

    @Operation(summary = "角色列表(用户分配使用)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:search')")
    @PostMapping("/getRoleList")
    public Result<List<SysRole>> getRoleList() {
        return Result.ok(sysRoleService.getRoleList());
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:search')")
    @Parameters(value = {
        @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    @PostMapping("/getRoleDetail")
    public Result<SysRoleVo> getRoleDetail(@RequestParam("roleId") Long roleId) {
        return Result.ok(sysRoleService.getRoleDetail(roleId));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:add')")
    @LogCollect(module = "角色管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addRole")
    public Result<Void> addRole(@RequestBody @Valid SysRoleForm form) {
        return sysRoleService.addRole(form) ? Result.ok() : Result.error("新增角色失败");
    }

    @Operation(summary = "更新角色")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:update')")
    @LogCollect(module = "角色管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateRole")
    public Result<Void> updateRole(@RequestBody @Valid SysRoleForm form) {
        return sysRoleService.updateRole(form) ? Result.ok() : Result.error("更新角色失败");
    }

    @Operation(summary = "删除角色(ROOT/已分配用户不允许删除)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:del')")
    @Parameters(value = {
        @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    @LogCollect(module = "角色管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteRole")
    public Result<Void> deleteRole(@RequestParam("roleId") Long roleId) {
        return sysRoleService.deleteRole(roleId) ? Result.ok() : Result.error("删除角色失败");
    }

    @Operation(summary = "启用/停用角色(ROOT不允许停用)")
    @PreAuthorize("hasRole('ROOT') and (#status == 1 ? hasAuthority('role:enable') : hasAuthority('role:disable'))")
    @Parameters(value = {
        @Parameter(name = "roleId", description = "角色ID", required = true),
        @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "角色管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateRoleStatus")
    public Result<Void> updateRoleStatus(@RequestParam("roleId") Long roleId,
                                         @RequestParam("status") Integer status) {
        return sysRoleService.updateRoleStatus(roleId, status)
                ? Result.ok() : Result.error("更新角色状态失败");
    }

    @Operation(summary = "查询角色已授权菜单ID")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:menu')")
    @Parameters(value = {
        @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    @PostMapping("/getRoleMenuIds")
    public Result<List<Long>> getRoleMenuIds(@RequestParam("roleId") Long roleId) {
        return Result.ok(sysRoleService.getRoleMenuIds(roleId));
    }

    @Operation(summary = "保存角色菜单授权(ROOT不允许修改)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:menu')")
    @LogCollect(module = "角色管理-菜单授权", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/saveRoleMenu")
    public Result<Void> saveRoleMenu(@RequestBody @Valid SysRoleMenuForm form) {
        return sysRoleService.saveRoleMenu(form.getRoleId(), form.getMenuIds())
                ? Result.ok() : Result.error("保存角色菜单授权失败");
    }

    @Operation(summary = "查询角色自定义部门ID")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:dept')")
    @Parameters(value = {
        @Parameter(name = "roleId", description = "角色ID", required = true)
    })
    @PostMapping("/getRoleDeptIds")
    public Result<List<Long>> getRoleDeptIds(@RequestParam("roleId") Long roleId) {
        return Result.ok(sysRoleService.getRoleDeptIds(roleId));
    }

    @Operation(summary = "保存角色自定义部门(仅data_scope=5)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('role:dept')")
    @LogCollect(module = "角色管理-数据权限部门", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/saveRoleDept")
    public Result<Void> saveRoleDept(@RequestBody @Valid SysRoleDeptForm form) {
        return sysRoleService.saveRoleDept(form.getRoleId(), form.getDeptIds())
                ? Result.ok() : Result.error("保存角色自定义部门失败");
    }
}
