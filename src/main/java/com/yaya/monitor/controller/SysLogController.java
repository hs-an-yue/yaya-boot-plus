package com.yaya.monitor.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.monitor.domain.query.SysLogQuery;
import com.yaya.monitor.domain.vo.SysLogVo;
import com.yaya.monitor.service.SysLogService;
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

@Tag(name = "监控管理")
@RestController
@RequestMapping("/system/sysLog")
public class SysLogController {

    @Resource
    private SysLogService sysLogService;

    @Operation(summary = "日志分页")
    @PreAuthorize("hasRole('ROOT') or hasAnyAuthority('loginLog:search','sysLog:search')")
    @PostMapping(value = "/getSysLogPage")
    public Result<PageDTO<SysLogVo>> getSysLogPage(@RequestBody @Valid SysLogQuery query){
        return Result.ok(sysLogService.getSysLogPage(query));
    }

    @Operation(summary = "日志详情")
    @PreAuthorize("hasRole('ROOT') or hasAnyAuthority('loginLog:search','sysLog:search')")
    @Parameters(value = {
        @Parameter(name = "logId", description = "日志ID", required = true)
    })
    @PostMapping("/getSysLogDetail")
    public Result<SysLogVo> getSysLogDetail(@RequestParam("logId") Long logId){
        return Result.ok(sysLogService.getSysLogDetail(logId));
    }

    @Operation(summary = "清空日志表(仅ROOT和运营管理员)")
    @PreAuthorize("hasAnyRole('ROOT','OPERATION')")
    @LogCollect(module = "日志管理-清空", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping(value = "/clearSysLog")
    public Result<Integer> clearSysLog(){
        return Result.ok(sysLogService.clearSysLog());
    }
}
