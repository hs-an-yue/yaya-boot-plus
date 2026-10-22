package com.yaya.monitor.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 请求统计环形缓冲
 * <p>
 * 1. 60 个秒槽: 滚动窗口计算最近 60 秒 QPS 与 5xx 错误率;
 * 2. 61 个分钟桶: 最近 60 分钟趋势(QPS/P99/错误率), 桶内保留耗时样本用于分位计算;
 * 3. 纯内存, 不落库, 应用重启后从零累积。
 * <p>
 * 线程安全: 拦截器多线程写入, 统一 synchronized; 内部管理系统量级小, 无性能问题。
 */
@Component
public class RequestStatsBuffer {

    /** 秒槽个数(最近 60 秒滚动窗口) */
    private static final int SECOND_SLOTS = 60;
    /** 分钟桶个数(61 = 最近 60 个完整分钟 + 当前部分分钟) */
    private static final int MINUTE_BUCKETS = 61;
    /** 每分钟保留的耗时样本上限, 超出后丢弃(分位计算用不完这么多) */
    private static final int MAX_COST_SAMPLES = 20000;

    private final long[] secondStamps = new long[SECOND_SLOTS];
    private final long[] secondCounts = new long[SECOND_SLOTS];
    private final long[] secondErrors = new long[SECOND_SLOTS];

    private final MinuteBucket[] minuteBuckets = new MinuteBucket[MINUTE_BUCKETS];

    /** 单个分钟桶的统计 */
    public record MinuteStat(long epochMinute, boolean exists, long count, long err5xx, double p99Millis) {
    }

    public synchronized void record(long costMillis, boolean serverError) {
        long epochSecond = System.currentTimeMillis() / 1000;
        int secondIndex = (int) (epochSecond % SECOND_SLOTS);
        if (secondStamps[secondIndex] != epochSecond) {
            secondStamps[secondIndex] = epochSecond;
            secondCounts[secondIndex] = 0;
            secondErrors[secondIndex] = 0;
        }
        secondCounts[secondIndex]++;
        if (serverError) {
            secondErrors[secondIndex]++;
        }

        long epochMinute = epochSecond / 60;
        MinuteBucket bucket = bucketFor(epochMinute);
        bucket.count++;
        if (serverError) {
            bucket.err5xx++;
        }
        if (bucket.costs.size() < MAX_COST_SAMPLES) {
            bucket.costs.add(costMillis);
        }
    }

    /** 最近 60 秒平均 QPS */
    public synchronized double qps() {
        long count = 0;
        long nowSecond = System.currentTimeMillis() / 1000;
        for (int i = 0; i < SECOND_SLOTS; i++) {
            if (nowSecond - secondStamps[i] < SECOND_SLOTS) {
                count += secondCounts[i];
            }
        }
        return count / (double) SECOND_SLOTS;
    }

    /** 最近 60 秒 5xx 错误率(百分比), 无请求时返回 0 */
    public synchronized double errRatePercent() {
        long count = 0;
        long err = 0;
        long nowSecond = System.currentTimeMillis() / 1000;
        for (int i = 0; i < SECOND_SLOTS; i++) {
            if (nowSecond - secondStamps[i] < SECOND_SLOTS) {
                count += secondCounts[i];
                err += secondErrors[i];
            }
        }
        return count == 0 ? 0 : err * 100.0 / count;
    }

    /** 最近一分钟桶的 P99 耗时(毫秒), 无样本返回 0 */
    public synchronized double p99Millis() {
        MinuteBucket bucket = minuteBuckets[(int) ((System.currentTimeMillis() / 60000) % MINUTE_BUCKETS)];
        if (bucket == null || bucket.costs.isEmpty()) {
            return 0;
        }
        return percentile(bucket.costs, 0.99);
    }

    /**
     * 最近 minutes 个分钟的统计(含当前部分分钟, 升序)
     *
     * @param minutes 分钟数, 建议 60
     */
    public synchronized List<MinuteStat> lastMinutes(int minutes) {
        long nowMinute = System.currentTimeMillis() / 60000;
        List<MinuteStat> result = new ArrayList<>(minutes);
        for (long offset = minutes - 1L; offset >= 0; offset--) {
            long epochMinute = nowMinute - offset;
            MinuteBucket bucket = minuteBuckets[(int) (epochMinute % MINUTE_BUCKETS)];
            boolean exists = bucket != null && bucket.epochMinute == epochMinute;
            result.add(new MinuteStat(epochMinute, exists, exists ? bucket.count : 0,
                    exists ? bucket.err5xx : 0, exists ? percentile(bucket.costs, 0.99) : 0));
        }
        return result;
    }

    private MinuteBucket bucketFor(long epochMinute) {
        int index = (int) (epochMinute % MINUTE_BUCKETS);
        MinuteBucket bucket = minuteBuckets[index];
        if (bucket == null || bucket.epochMinute != epochMinute) {
            bucket = new MinuteBucket(epochMinute);
            minuteBuckets[index] = bucket;
        }
        return bucket;
    }

    private static double percentile(List<Long> samples, double quantile) {
        if (samples.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(samples);
        sorted.sort(Long::compare);
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    /** 分钟桶 */
    private static class MinuteBucket {
        private final long epochMinute;
        private final List<Long> costs = new ArrayList<>();
        private long count;
        private long err5xx;

        private MinuteBucket(long epochMinute) {
            this.epochMinute = epochMinute;
        }
    }
}
