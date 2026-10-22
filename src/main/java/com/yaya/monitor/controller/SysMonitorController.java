package com.yaya.monitor.controller;

import com.yaya.framework.dto.Result;
import com.yaya.monitor.domain.vo.MonitorOverviewVo;
import com.yaya.monitor.domain.vo.MonitorTrendVo;
import com.yaya.monitor.service.SysMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控
 * <p>
 * 服务器资源/中间件健康/请求指标, 只读接口; 与缓存管理一致仅 ROOT 可访问。
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/system/monitor")
public class SysMonitorController {

    @Resource
    private SysMonitorService sysMonitorService;

    @Operation(summary = "监控概览(仅ROOT): 资源快照+中间件健康+最近60秒请求指标")
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/getMonitorOverview")
    public Result<MonitorOverviewVo> getMonitorOverview() {
        return Result.ok(sysMonitorService.getMonitorOverview());
    }

    @Operation(summary = "监控趋势(仅ROOT): 近60分钟QPS/P99/错误率/CPU/内存")
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/getMonitorTrend")
    public Result<MonitorTrendVo> getMonitorTrend() {
        return Result.ok(sysMonitorService.getMonitorTrend());
    }
}
