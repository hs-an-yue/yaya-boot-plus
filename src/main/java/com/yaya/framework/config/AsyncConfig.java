package com.yaya.framework.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 1. 开启 @EnableAsync <p>
 * 2. 提供有界线程池，避免默认无界线程池导致资源耗尽 <p>
 * 3. 通过 TaskDecorator 把 MDC 中的 trackId 传递到异步线程 <p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 日志异步线程池
     * <p>
     * 业务日志写入使用该线程池；核心线程 2、最大线程 4、队列 1000。<p>
     * 队列满时使用 CallerRunsPolicy，让调用线程执行任务，避免日志被静默丢弃。<p>
     */
    @Primary
    @Bean("logExecutor")
    public ThreadPoolTaskExecutor logExecutor(TaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("log-async-");
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 把当前线程的 MDC 上下文复制到异步线程，执行完后恢复异步线程原有上下文
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
