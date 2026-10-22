package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.SysMenuForm;
import com.yaya.system.domain.query.SysMenuQuery;
import com.yaya.system.domain.vo.SysMenuVo;
import com.yaya.system.service.SysMenuService;
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

/**
 * 菜单管理
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    @Resource
    private SysMenuService sysMenuService;

    @Operation(summary = "查询菜单树")
    @PreAuthorize("hasRole('ROOT') and hasAnyAuthority('menu:search','role:menu')")
    @PostMapping("/getMenuTree")
    public Result<List<SysMenuVo>> getMenuTree(@RequestBody(required = false) SysMenuQuery query) {
        return Result.ok(sysMenuService.getMenuTree(query));
    }

    @Operation(summary = "查询当前用户导航菜单树")
    @PostMapping("/getRouter")
    public Result<List<SysMenuVo>> getRouter() {
        return Result.ok(sysMenuService.getRouter());
    }

    @Operation(summary = "查询菜单详情")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('menu:search')")
    @Parameters(value = {
            @Parameter(name = "menuId", description = "菜单ID", required = true)
    })
    @PostMapping("/getMenuDetail")
    public Result<SysMenuVo> getMenuDetail(@RequestParam("menuId") Long menuId) {
        return Result.ok(sysMenuService.getMenuDetail(menuId));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('menu:add')")
    @LogCollect(module = "菜单管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addMenu")
    public Result<Void> addMenu(@Valid @RequestBody SysMenuForm form) {
        sysMenuService.addMenu(form);
        return Result.ok();
    }

    @Operation(summary = "更新菜单")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('menu:update')")
    @LogCollect(module = "菜单管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateMenu")
    public Result<Void> updateMenu(@Valid @RequestBody SysMenuForm form) {
        sysMenuService.updateMenu(form);
        return Result.ok();
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('menu:del')")
    @Parameters(value = {
            @Parameter(name = "menuId", description = "菜单ID", required = true)
    })
    @LogCollect(module = "菜单管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteMenu")
    public Result<Void> deleteMenu(@RequestParam("menuId") Long menuId) {
        sysMenuService.deleteMenu(menuId);
        return Result.ok();
    }

    @Operation(summary = "启用/停用菜单")
    @PreAuthorize("hasRole('ROOT') and (#status == 1 ? hasAuthority('menu:enable') : hasAuthority('menu:disable'))")
    @Parameters(value = {
            @Parameter(name = "menuId", description = "菜单ID", required = true),
            @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "菜单管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateMenuStatus")
    public Result<Void> updateMenuStatus(@RequestParam("menuId") Long menuId,
                                         @RequestParam("status") Integer status) {
        sysMenuService.updateMenuStatus(menuId, status);
        return Result.ok();
    }
}
