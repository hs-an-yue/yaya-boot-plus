package com.yaya.monitor.service.impl;

import com.yaya.monitor.domain.vo.MonitorOverviewVo;
import com.yaya.monitor.domain.vo.MonitorTrendVo;
import com.yaya.monitor.service.SysMonitorService;
import com.yaya.monitor.support.MonitorMetricsCollector;
import com.yaya.monitor.support.MonitorSnapshot;
import com.yaya.monitor.support.RequestStatsBuffer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统监控 Service 实现
 * <p>
 * 数据全部来自内存采样器与请求统计缓冲, 不落库, 重启后从零累积。
 */
@Service
public class SysMonitorServiceImpl implements SysMonitorService {

    private static final int TREND_MINUTES = 60;
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private MonitorMetricsCollector monitorMetricsCollector;

    @Resource
    private RequestStatsBuffer requestStatsBuffer;

    @Override
    public MonitorOverviewVo getMonitorOverview() {
        MonitorSnapshot snapshot = monitorMetricsCollector.latestOrSample();
        MonitorOverviewVo vo = new MonitorOverviewVo();
        vo.setCpuPercent(round1(snapshot.cpuPercent()));
        vo.setMemPercent(round1(snapshot.memPercent()));
        vo.setMemUsedMb(round1(snapshot.memUsedMb()));
        vo.setMemTotalMb(round1(snapshot.memTotalMb()));
        vo.setJvmHeapUsedMb(round1(snapshot.jvmHeapUsedMb()));
        vo.setJvmHeapMaxMb(round1(snapshot.jvmHeapMaxMb()));
        vo.setThreadCount(snapshot.threadCount());
        vo.setLoadAverage(round2(snapshot.loadAverage()));
        vo.setDiskUsedPercent(round1(snapshot.diskUsedPercent()));
        vo.setDiskUsedGb(round1(snapshot.diskUsedGb()));
        vo.setDiskTotalGb(round1(snapshot.diskTotalGb()));
        vo.setUptimeMillis(snapshot.uptimeMillis());
        vo.setQps(round1(requestStatsBuffer.qps()));
        vo.setErrRatePercent(round3(requestStatsBuffer.errRatePercent()));
        vo.setP99Millis(round1(requestStatsBuffer.p99Millis()));
        vo.setRedisOk(snapshot.redisOk());
        vo.setDbOk(snapshot.dbOk());
        return vo;
    }

    @Override
    public MonitorTrendVo getMonitorTrend() {
        MonitorTrendVo vo = new MonitorTrendVo();
        List<String> timeLabels = new ArrayList<>(TREND_MINUTES);
        List<Double> qps = new ArrayList<>(TREND_MINUTES);
        List<Double> p99 = new ArrayList<>(TREND_MINUTES);
        List<Double> errRate = new ArrayList<>(TREND_MINUTES);
        List<Double> cpu = new ArrayList<>(TREND_MINUTES);
        List<Double> mem = new ArrayList<>(TREND_MINUTES);

        List<RequestStatsBuffer.MinuteStat> minutes = requestStatsBuffer.lastMinutes(TREND_MINUTES);
        for (RequestStatsBuffer.MinuteStat stat : minutes) {
            LocalDateTime minute = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(stat.epochMinute() * 60000),
                    java.time.ZoneId.systemDefault());
            timeLabels.add(MINUTE_FORMATTER.format(minute));
            // 无数据的分钟补 0(CPU/内存补 null, 图表断开显示)
            qps.add(stat.exists() ? round1(stat.count() / 60.0) : 0.0);
            errRate.add(stat.exists() ? round3(stat.count() == 0 ? 0 : stat.err5xx() * 100.0 / stat.count()) : 0.0);
            p99.add(stat.exists() ? round1(stat.p99Millis()) : 0.0);
            cpu.add(round1OrNull(monitorMetricsCollector.cpuAtMinute(stat.epochMinute())));
            mem.add(round1OrNull(monitorMetricsCollector.memAtMinute(stat.epochMinute())));
        }

        vo.setTimeLabels(timeLabels);
        vo.setQps(qps);
        vo.setP99Millis(p99);
        vo.setErrRatePercent(errRate);
        vo.setCpuPercent(cpu);
        vo.setMemPercent(mem);
        return vo;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static double round3(double value) {
        return Math.round(value * 1000) / 1000.0;
    }

    private static Double round1OrNull(Double value) {
        return value == null ? null : round1(value);
    }
}
