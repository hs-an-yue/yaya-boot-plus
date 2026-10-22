package com.yaya.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 系统监控趋势(近 60 分钟, 每分钟一个点, 平行数组)
 * <p>
 * 数据只保留在内存环形缓冲中, 应用重启后从零累积。
 */
@Data
@Schema(description = "系统监控趋势(近60分钟)")
public class MonitorTrendVo {

    @Schema(description = "时间标签(HH:mm, 升序, 最后一个点为当前部分分钟)")
    private List<String> timeLabels;

    @Schema(description = "每分钟平均QPS")
    private List<Double> qps;

    @Schema(description = "每分钟P99耗时(毫秒)")
    private List<Double> p99Millis;

    @Schema(description = "每分钟5xx错误率(百分比)")
    private List<Double> errRatePercent;

    @Schema(description = "每分钟CPU使用率(0~100)")
    private List<Double> cpuPercent;

    @Schema(description = "每分钟内存使用率(0~100)")
    private List<Double> memPercent;
}
