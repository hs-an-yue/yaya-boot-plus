package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.entity.SysSecretKey;
import com.yaya.system.domain.form.SysSecretKeyForm;
import com.yaya.system.domain.query.SysSecretKeyQuery;
import com.yaya.system.service.SysSecretKeyService;
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

@Tag(name = "密钥管理")
@RestController
@RequestMapping("/system/secretKey")
public class SysSecretKeyController {

    @Resource
    private SysSecretKeyService sysSecretKeyService;

    @Operation(summary = "分页查询密钥")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('secretKey:search')")
    @PostMapping("/getSecretKeyPage")
    public Result<PageDTO<SysSecretKey>> getSecretKeyPage(@RequestBody @Valid SysSecretKeyQuery query) {
        return Result.ok(sysSecretKeyService.getSecretKeyPage(query));
    }

    @Operation(summary = "查询密钥详情")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('secretKey:search')")
    @Parameters(value = {
        @Parameter(name = "keyId", description = "密钥ID", required = true)
    })
    @PostMapping("/getSecretKeyDetail")
    public Result<SysSecretKey> getSecretKeyDetail(@RequestParam("keyId") Long keyId) {
        return Result.ok(sysSecretKeyService.getSecretKeyDetail(keyId));
    }

    @Operation(summary = "新增密钥")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('secretKey:add')")
    @LogCollect(module = "密钥管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addSecretKey")
    public Result<Void> addSecretKey(@RequestBody @Valid SysSecretKeyForm form) {
        return sysSecretKeyService.addSecretKey(form) ? Result.ok() : Result.error("新增密钥失败");
    }

    @Operation(summary = "更新密钥")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('secretKey:update')")
    @LogCollect(module = "密钥管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateSecretKey")
    public Result<Void> updateSecretKey(@RequestBody @Valid SysSecretKeyForm form) {
        return sysSecretKeyService.updateSecretKey(form) ? Result.ok() : Result.error("更新密钥失败");
    }

    @Operation(summary = "删除密钥")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('secretKey:del')")
    @Parameters(value = {
        @Parameter(name = "keyId", description = "密钥ID", required = true)
    })
    @LogCollect(module = "密钥管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteSecretKey")
    public Result<Void> deleteSecretKey(@RequestParam("keyId") Long keyId) {
        return sysSecretKeyService.deleteSecretKey(keyId) ? Result.ok() : Result.error("删除密钥失败");
    }

    @Operation(summary = "启用/停用密钥")
    @PreAuthorize("hasRole('ROOT') and (#status == 1 ? hasAuthority('secretKey:enable') : hasAuthority('secretKey:disable'))")
    @Parameters(value = {
        @Parameter(name = "keyId", description = "密钥ID", required = true),
        @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "密钥管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateSecretKeyStatus")
    public Result<Void> updateSecretKeyStatus(@RequestParam("keyId") Long keyId,
                                              @RequestParam("status") Integer status) {
        return sysSecretKeyService.updateSecretKeyStatus(keyId, status)
                ? Result.ok() : Result.error("更新密钥状态失败");
    }
}
