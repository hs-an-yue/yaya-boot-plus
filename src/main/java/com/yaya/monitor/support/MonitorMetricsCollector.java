package com.yaya.monitor.support;

import cn.hutool.core.util.StrUtil;
import com.yaya.framework.config.YaYaConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 资源监控采样器
 * <p>
 * 1. 每 5 秒后台采样一次(CPU/内存/JVM/磁盘/中间件健康), 结果放原子引用供所有请求共享,
 *    避免每个前端请求都做一次采样(getSystemCpuLoad 是距上次调用以来的平均值, 集中采样才准确);
 * 2. 每分钟追加一个 CPU/内存趋势点(61 点环形), 供趋势图使用;
 * 3. Redis/数据库健康探测随采样进行, 单项失败只影响自身状态, 不中断其他指标。
 * <p>
 * 全部基于 JDK 内置 MXBean 与 java.io.File, 无第三方依赖。
 * 依赖字段为包级私有以便单元测试直接赋值。
 */
@Slf4j
@Component
public class MonitorMetricsCollector {

    /** 趋势环点数(61 = 最近 60 个完整分钟 + 当前部分分钟) */
    private static final int TREND_POINTS = 61;
    private static final double MB = 1024.0 * 1024.0;
    private static final double GB = 1024.0 * 1024.0 * 1024.0;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    DataSource dataSource;

    @Resource
    YaYaConfig yaYaConfig;

    private final com.sun.management.OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final RuntimeMXBean runtimeBean;

    private final AtomicReference<MonitorSnapshot> latest = new AtomicReference<>();

    private final long[] trendStamps = new long[TREND_POINTS];
    private final double[] trendCpu = new double[TREND_POINTS];
    private final double[] trendMem = new double[TREND_POINTS];

    public MonitorMetricsCollector() {
        this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
    }

    /** 每 5 秒采样一次 */
    @Scheduled(fixedDelay = 5000)
    public void sampleScheduled() {
        sampleNow();
    }

    /**
     * 返回最新快照; 尚未采样过(应用刚启动)时同步采一次
     */
    public MonitorSnapshot latestOrSample() {
        MonitorSnapshot snapshot = latest.get();
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (latest.get() == null) {
                latest.set(sampleNow());
            }
            return latest.get();
        }
    }

    /**
     * 执行一次采样并写入快照与趋势环
     */
    public synchronized MonitorSnapshot sampleNow() {
        double cpu = clampPercent(osBean.getSystemCpuLoad() * 100);
        long totalMem = osBean.getTotalMemorySize();
        long freeMem = osBean.getFreeMemorySize();
        double memPercent = totalMem > 0 ? clampPercent((totalMem - freeMem) * 100.0 / totalMem) : 0;
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();

        File diskRoot = resolveDiskRoot();
        long diskTotal = diskRoot.getTotalSpace();
        long diskUsable = diskRoot.getUsableSpace();
        double diskPercent = diskTotal > 0 ? clampPercent((diskTotal - diskUsable) * 100.0 / diskTotal) : 0;

        long epochMinute = System.currentTimeMillis() / 60000;
        MonitorSnapshot snapshot = new MonitorSnapshot(
                cpu, memPercent,
                (totalMem - freeMem) / MB, totalMem / MB,
                heap.getUsed() / MB, heap.getMax() / MB,
                threadBean.getThreadCount(),
                osBean.getSystemLoadAverage(),
                diskPercent, (diskTotal - diskUsable) / GB, diskTotal / GB,
                runtimeBean.getUptime(),
                checkRedis(), checkDb(),
                epochMinute);
        latest.set(snapshot);
        appendTrend(epochMinute, snapshot.cpuPercent(), snapshot.memPercent());
        return snapshot;
    }

    /**
     * 取指定分钟的 CPU 趋势点, 无该分钟数据返回 null
     */
    public synchronized Double cpuAtMinute(long epochMinute) {
        return valueAtMinute(epochMinute, trendCpu);
    }

    /**
     * 取指定分钟的内存趋势点, 无该分钟数据返回 null
     */
    public synchronized Double memAtMinute(long epochMinute) {
        return valueAtMinute(epochMinute, trendMem);
    }

    private Double valueAtMinute(long epochMinute, double[] values) {
        int index = (int) (epochMinute % TREND_POINTS);
        return trendStamps[index] == epochMinute ? values[index] : null;
    }

    private void appendTrend(long epochMinute, double cpu, double mem) {
        int index = (int) (epochMinute % TREND_POINTS);
        trendStamps[index] = epochMinute;
        trendCpu[index] = cpu;
        trendMem[index] = mem;
    }

    private boolean checkRedis() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.debug("系统监控-Redis健康探测失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkDb() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.debug("系统监控-数据库健康探测失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 磁盘取上传目录所在盘(应用日志与上传文件规划在同一磁盘), 未配置时回退工作目录
     */
    private File resolveDiskRoot() {
        String configuredPath = yaYaConfig == null ? null : yaYaConfig.getFilePath();
        if (StrUtil.isBlank(configuredPath)) {
            return Paths.get("").toAbsolutePath().toFile();
        }
        return Paths.get(configuredPath).toAbsolutePath().getRoot().toFile();
    }

    private static double clampPercent(double value) {
        return Math.max(0, Math.min(100, value));
    }
}
