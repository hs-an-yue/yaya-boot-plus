package com.yaya.monitor.task;

import com.yaya.monitor.service.SysLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * 日志清理定时任务容错测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLogCleanTaskTest {

    @Mock
    private SysLogService sysLogService;

    @InjectMocks
    private SysLogCleanTask sysLogCleanTask;

    @Test
    @DisplayName("清理失败时任务吞掉异常, 不影响下一次调度")
    void taskSwallowsServiceException() {
        when(sysLogService.cleanExpiredLogs()).thenThrow(new RuntimeException("数据库不可用"));

        assertDoesNotThrow(() -> sysLogCleanTask.cleanExpiredLogs());
    }
}
