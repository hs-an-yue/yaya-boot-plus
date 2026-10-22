package com.yaya.monitor.task;

import com.yaya.monitor.service.SysLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志清理定时任务
 * <p>
 * 每天凌晨 3:30 清理超过保留天数(yaya.log-retention-days, 默认30天)的日志 <p>
 * 数据库日志(sys_log)与日志文件(logback maxHistory)共用同一个保留天数配置 <p>
 * 单机部署场景, 不做分布式锁; 任务失败只记录日志, 第二天同一时间自然重试
 */
@Slf4j
@Component
public class SysLogCleanTask {

    @Resource
    private SysLogService sysLogService;

    /**
     * 自动清理过期日志
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredLogs() {
        try {
            int deleted = sysLogService.cleanExpiredLogs();
            log.info("日志清理-定时任务完成, 删除行数={}", deleted);
        } catch (Exception e) {
            log.error("日志清理-定时任务失败", e);
        }
    }
}
