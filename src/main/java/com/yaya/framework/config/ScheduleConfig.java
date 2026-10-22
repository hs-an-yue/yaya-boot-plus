package com.yaya.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * <p>
 * 开启 @EnableScheduling 后, 各模块 task 包下的 @Scheduled 任务生效 <p>
 * 任务逻辑统一放在各业务模块的 task 包中(如 com.yaya.monitor.task), 本类只负责开关
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
