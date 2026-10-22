package com.yaya.monitor.support;

/**
 * 一次资源采样的快照(只读值对象)
 *
 * @param cpuPercent     系统 CPU 使用率(0~100)
 * @param memPercent     系统内存使用率(0~100)
 * @param memUsedMb      系统已用内存(MB)
 * @param memTotalMb     系统总内存(MB)
 * @param jvmHeapUsedMb  JVM 堆已用(MB)
 * @param jvmHeapMaxMb   JVM 堆上限(MB)
 * @param threadCount    JVM 线程数
 * @param loadAverage    1 分钟系统负载(Windows 恒为负数, 前端显示 '-')
 * @param diskUsedPercent 上传/日志所在磁盘使用率(0~100)
 * @param diskUsedGb     磁盘已用(GB)
 * @param diskTotalGb    磁盘总量(GB)
 * @param uptimeMillis   应用运行时长(毫秒)
 * @param redisOk        Redis 是否可用
 * @param dbOk           数据库是否可用
 * @param epochMinute    采样时刻的 epoch 分钟(用于趋势环对齐)
 */
public record MonitorSnapshot(
        double cpuPercent,
        double memPercent,
        double memUsedMb,
        double memTotalMb,
        double jvmHeapUsedMb,
        double jvmHeapMaxMb,
        int threadCount,
        double loadAverage,
        double diskUsedPercent,
        double diskUsedGb,
        double diskTotalGb,
        long uptimeMillis,
        boolean redisOk,
        boolean dbOk,
        long epochMinute) {
}
