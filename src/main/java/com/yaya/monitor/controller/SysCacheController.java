package com.yaya.monitor.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.monitor.domain.query.SysCacheQuery;
import com.yaya.monitor.domain.vo.SysCacheVo;
import com.yaya.monitor.service.SysCacheService;
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

/**
 * 缓存管理(仅ROOT)
 */
@Tag(name = "缓存管理")
@RestController
@RequestMapping("/system/cache")
public class SysCacheController {

    @Resource
    private SysCacheService sysCacheService;

    @Operation(summary = "缓存分页")
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/getCachePage")
    public Result<PageDTO<SysCacheVo>> getCachePage(@RequestBody @Valid SysCacheQuery query) {
        return Result.ok(sysCacheService.getCachePage(query));
    }

    @Operation(summary = "删除单个缓存")
    @PreAuthorize("hasRole('ROOT')")
    @Parameters(value = {
            @Parameter(name = "key", description = "缓存KEY", required = true)
    })
    @LogCollect(module = "缓存管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteCache")
    public Result<Void> deleteCache(@RequestParam("key") String key) {
        sysCacheService.deleteCache(key);
        return Result.ok();
    }

    @Operation(summary = "清空当前Redis DB全部缓存")
    @PreAuthorize("hasRole('ROOT')")
    @LogCollect(module = "缓存管理-清空", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/clearCache")
    public Result<Integer> clearCache() {
        return Result.ok(sysCacheService.clearCache());
    }
}
