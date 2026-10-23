package com.yaya.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统监控概览(资源 + 中间件健康 + 请求指标)
 */
@Data
@Schema(description = "系统监控概览")
public class MonitorOverviewVo {

    @Schema(description = "系统CPU使用率(0~100)")
    private Double cpuPercent;

    @Schema(description = "系统内存使用率(0~100)")
    private Double memPercent;

    @Schema(description = "系统已用内存(MB)")
    private Double memUsedMb;

    @Schema(description = "系统总内存(MB)")
    private Double memTotalMb;

    @Schema(description = "JVM堆已用(MB)")
    private Double jvmHeapUsedMb;

    @Schema(description = "JVM堆上限(MB)")
    private Double jvmHeapMaxMb;

    @Schema(description = "JVM线程数")
    private Integer threadCount;

    @Schema(description = "可用CPU核数")
    private Integer cpuCoreCount;

    @Schema(description = "1分钟系统负载(Windows恒为负数, 前端显示 '-')")
    private Double loadAverage;

    @Schema(description = "上传/日志所在磁盘使用率(0~100)")
    private Double diskUsedPercent;

    @Schema(description = "磁盘已用(GB)")
    private Double diskUsedGb;

    @Schema(description = "磁盘总量(GB)")
    private Double diskTotalGb;

    @Schema(description = "应用运行时长(毫秒)")
    private Long uptimeMillis;

    @Schema(description = "最近60秒平均QPS")
    private Double qps;

    @Schema(description = "最近60秒5xx错误率(百分比)")
    private Double errRatePercent;

    @Schema(description = "最近一分钟P99耗时(毫秒)")
    private Double p99Millis;

    @Schema(description = "Redis是否可用")
    private Boolean redisOk;

    @Schema(description = "数据库是否可用")
    private Boolean dbOk;
}
