package com.yaya.monitor.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 请求统计环形缓冲测试
 */
class RequestStatsBufferTest {

    @Test
    @DisplayName("记录请求后: QPS/错误率正确, 最近60分钟含当前分钟")
    void recordAndRead() {
        RequestStatsBuffer buffer = new RequestStatsBuffer();
        // 100 个请求, 10 个 5xx, 耗时 1~100ms
        for (long cost = 1; cost <= 100; cost++) {
            buffer.record(cost, cost % 10 == 0);
        }

        assertTrue(buffer.qps() > 0 && buffer.qps() <= 100.0 / 60);
        assertEquals(10.0, buffer.errRatePercent(), 0.001);

        // 耗时 1~100 升序, P99 = 第 99 个样本 = 99
        assertEquals(99.0, buffer.p99Millis(), 0.001);

        List<RequestStatsBuffer.MinuteStat> minutes = buffer.lastMinutes(60);
        assertEquals(60, minutes.size());
        // 记录与读取可能跨分钟边界, 时间跨度允许 59 或 60
        long span = minutes.get(minutes.size() - 1).epochMinute() - minutes.get(0).epochMinute();
        assertTrue(span == 59 || span == 60);
        // 全部 100 个请求都落在有数据的分钟里(可能跨两个桶)
        long totalCount = minutes.stream().filter(RequestStatsBuffer.MinuteStat::exists)
                .mapToLong(RequestStatsBuffer.MinuteStat::count).sum();
        assertEquals(100, totalCount);
    }

    @Test
    @DisplayName("空缓冲: 各指标为 0, 分钟序列全部标记无数据")
    void emptyBufferReturnsZeros() {
        RequestStatsBuffer buffer = new RequestStatsBuffer();

        assertEquals(0, buffer.qps());
        assertEquals(0, buffer.errRatePercent());
        assertEquals(0, buffer.p99Millis());

        List<RequestStatsBuffer.MinuteStat> minutes = buffer.lastMinutes(60);
        assertEquals(60, minutes.size());
        assertTrue(minutes.stream().noneMatch(RequestStatsBuffer.MinuteStat::exists));
    }

    @Test
    @DisplayName("分钟桶环形复用: 同一分钟反复记录不重复建桶")
    void sameMinuteReusesBucket() {
        RequestStatsBuffer buffer = new RequestStatsBuffer();
        for (int i = 0; i < 50; i++) {
            buffer.record(5, false);
        }

        List<RequestStatsBuffer.MinuteStat> minutes = buffer.lastMinutes(60);
        long existsCount = minutes.stream().filter(RequestStatsBuffer.MinuteStat::exists).count();
        assertEquals(1, existsCount);
        assertEquals(50, minutes.stream().filter(RequestStatsBuffer.MinuteStat::exists)
                .findFirst().orElseThrow().count());
    }
}
