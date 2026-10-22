package com.yaya.monitor.service;

import com.yaya.monitor.domain.vo.MonitorOverviewVo;
import com.yaya.monitor.domain.vo.MonitorTrendVo;

/**
 * 系统监控 Service
 */
public interface SysMonitorService {

    /**
     * 监控概览: 资源快照 + 中间件健康 + 最近 60 秒请求指标
     */
    MonitorOverviewVo getMonitorOverview();

    /**
     * 近 60 分钟趋势: QPS/P99/错误率/CPU/内存, 每分钟一个点
     */
    MonitorTrendVo getMonitorTrend();
}
