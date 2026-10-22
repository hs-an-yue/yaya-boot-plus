package com.yaya.monitor.support;

import com.yaya.framework.config.YaYaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资源监控采样器测试
 * <p>
 * 直接使用 JVM 真实 MXBean 采样; Redis/数据库依赖置空时探测按"不可用"处理, 验证故障隔离。
 */
class MonitorMetricsCollectorTest {

    @TempDir
    private Path tempDir;

    private MonitorMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MonitorMetricsCollector();
        collector.stringRedisTemplate = null;
        collector.dataSource = null;
        collector.yaYaConfig = new YaYaConfig();
        collector.yaYaConfig.setFilePath(tempDir.toString());
    }

    @Test
    @DisplayName("采样: 资源字段完整且在合理范围, 中间件依赖缺失时状态为不可用但不影响其他指标")
    void sampleNowPopulatesSnapshot() {
        MonitorSnapshot snapshot = collector.sampleNow();

        assertTrue(snapshot.cpuPercent() >= 0 && snapshot.cpuPercent() <= 100);
        assertTrue(snapshot.memPercent() >= 0 && snapshot.memPercent() <= 100);
        assertTrue(snapshot.memTotalMb() > 0);
        assertTrue(snapshot.jvmHeapMaxMb() > 0);
        assertTrue(snapshot.jvmHeapUsedMb() > 0 && snapshot.jvmHeapUsedMb() <= snapshot.jvmHeapMaxMb());
        assertTrue(snapshot.threadCount() > 0);
        assertTrue(snapshot.diskTotalGb() > 0);
        assertTrue(snapshot.diskUsedPercent() >= 0 && snapshot.diskUsedPercent() <= 100);
        assertTrue(snapshot.uptimeMillis() >= 0);
        // Redis/数据库依赖为空 → 探测失败 → 状态不可用, 其余指标不受影响
        assertFalse(snapshot.redisOk());
        assertFalse(snapshot.dbOk());
    }

    @Test
    @DisplayName("趋势环: 采样后当前分钟有 CPU/内存点, 未采样分钟返回 null")
    void trendRingAlignedByMinute() {
        MonitorSnapshot snapshot = collector.sampleNow();

        Double cpu = collector.cpuAtMinute(snapshot.epochMinute());
        Double mem = collector.memAtMinute(snapshot.epochMinute());
        assertNotNull(cpu);
        assertNotNull(mem);
        assertTrue(cpu >= 0 && cpu <= 100);
        assertTrue(mem >= 0 && mem <= 100);
        assertNull(collector.cpuAtMinute(snapshot.epochMinute() - 1));
    }

    @Test
    @DisplayName("同一分钟多次采样: 覆盖更新同一点而不是新增")
    void sameMinuteOverwritesTrendPoint() {
        collector.sampleNow();
        MonitorSnapshot second = collector.sampleNow();

        Double cpu = collector.cpuAtMinute(second.epochMinute());
        assertNotNull(cpu);
        // 61 点环内只有当前分钟一个时间戳
        long stamped = 0;
        for (long offset = 0; offset < 61; offset++) {
            if (collector.cpuAtMinute(second.epochMinute() - offset) != null) {
                stamped++;
            }
        }
        assertEquals(1, stamped);
    }
}
