package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.SysDeptForm;
import com.yaya.system.domain.query.SysDeptQuery;
import com.yaya.system.domain.vo.SysDeptVo;
import com.yaya.system.service.SysDeptService;
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
 * 部门管理
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Resource
    private SysDeptService sysDeptService;

    @Operation(summary = "查询部门树")
    @PreAuthorize("hasRole('ROOT') and hasAnyAuthority('dept:search','role:dept')")
    @PostMapping("/getDeptTree")
    public Result<List<SysDeptVo>> getDeptTree(@RequestBody(required = false) SysDeptQuery query) {
        return Result.ok(sysDeptService.getDeptTree(query));
    }

    @Operation(summary = "查询部门列表(下拉选择)")
    @PreAuthorize("hasRole('ROOT') and hasAnyAuthority('dept:search','role:dept')")
    @PostMapping("/getDeptList")
    public Result<List<SysDeptVo>> getDeptList() {
        return Result.ok(sysDeptService.getDeptList());
    }

    @Operation(summary = "查询部门详情")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('dept:search')")
    @Parameters(value = {
            @Parameter(name = "deptId", description = "部门ID", required = true)
    })
    @PostMapping("/getDeptDetail")
    public Result<SysDeptVo> getDeptDetail(@RequestParam("deptId") Long deptId) {
        return Result.ok(sysDeptService.getDeptDetail(deptId));
    }

    @Operation(summary = "新增部门")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('dept:add')")
    @LogCollect(module = "部门管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addDept")
    public Result<Void> addDept(@Valid @RequestBody SysDeptForm form) {
        sysDeptService.addDept(form);
        return Result.ok();
    }

    @Operation(summary = "更新部门")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('dept:update')")
    @LogCollect(module = "部门管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateDept")
    public Result<Void> updateDept(@Valid @RequestBody SysDeptForm form) {
        sysDeptService.updateDept(form);
        return Result.ok();
    }

    @Operation(summary = "删除部门")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('dept:del')")
    @Parameters(value = {
            @Parameter(name = "deptId", description = "部门ID", required = true)
    })
    @LogCollect(module = "部门管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteDept")
    public Result<Void> deleteDept(@RequestParam("deptId") Long deptId) {
        sysDeptService.deleteDept(deptId);
        return Result.ok();
    }

    @Operation(summary = "启用/停用部门")
    @PreAuthorize("hasRole('ROOT') and (#status == 1 ? hasAuthority('dept:enable') : hasAuthority('dept:disable'))")
    @Parameters(value = {
            @Parameter(name = "deptId", description = "部门ID", required = true),
            @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "部门管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateDeptStatus")
    public Result<Void> updateDeptStatus(@RequestParam("deptId") Long deptId,
                                         @RequestParam("status") Integer status) {
        sysDeptService.updateDeptStatus(deptId, status);
        return Result.ok();
    }
}
