package com.yaya.monitor.service;

import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.monitor.domain.vo.SysLogVo;
import com.yaya.monitor.mapper.SysLogMapper;
import com.yaya.monitor.service.impl.SysLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 日志清理与保留策略 Service 单元测试
 * (清空接口的访问权限由控制器层 @PreAuthorize 控制, 不在 Service 层测试)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLogServiceTest {

    @Mock
    private SysLogMapper sysLogMapper;

    @Mock
    private YaYaConfig yaYaConfig;

    @InjectMocks
    private SysLogServiceImpl sysLogService;

    @Test
    @DisplayName("清空日志：先查行数再TRUNCATE, 返回清空数量")
    void clearSysLogTruncatesAndReturnsCount() {
        when(sysLogMapper.selectCount(null)).thenReturn(128L);

        int deleted = sysLogService.clearSysLog();

        assertEquals(128, deleted);
        verify(sysLogMapper).clearSysLog();
    }

    @Test
    @DisplayName("日志详情：返回请求参数与错误信息")
    void getSysLogDetailReturnsDetail() {
        SysLogVo vo = new SysLogVo();
        vo.setLogId(1L);
        vo.setRequestParams("[{\"keyId\":1}]");
        vo.setErrorMsg("密钥不存在");
        when(sysLogMapper.getSysLogDetail(1L)).thenReturn(vo);

        SysLogVo detail = sysLogService.getSysLogDetail(1L);

        assertEquals("[{\"keyId\":1}]", detail.getRequestParams());
        assertEquals("密钥不存在", detail.getErrorMsg());
    }

    @Test
    @DisplayName("日志详情：不存在时抛出业务异常")
    void getSysLogDetailThrowsWhenMissing() {
        when(sysLogMapper.getSysLogDetail(9L)).thenReturn(null);

        GlobalCommonException e = assertThrows(GlobalCommonException.class,
                () -> sysLogService.getSysLogDetail(9L));
        assertEquals("日志不存在", e.getMessage());
    }

    @Test
    @DisplayName("保留策略：按配置天数计算截止时间")
    void cleanExpiredLogsUsesConfiguredDays() {
        when(yaYaConfig.getLogRetentionDays()).thenReturn(7);
        when(sysLogMapper.deleteBefore(any())).thenReturn(3);

        int deleted = sysLogService.cleanExpiredLogs();

        assertEquals(3, deleted);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sysLogMapper).deleteBefore(captor.capture());
        LocalDateTime cutoff = captor.getValue();
        LocalDateTime lower = LocalDateTime.now().minusDays(7).minusSeconds(5);
        LocalDateTime upper = LocalDateTime.now().minusDays(7).plusSeconds(5);
        assertTrue(cutoff.isAfter(lower) && cutoff.isBefore(upper),
                "截止时间应为当前时间减7天(允许秒级误差)");
    }

    @Test
    @DisplayName("保留策略：未配置保留天数时默认30天")
    void cleanExpiredLogsDefaultsTo30Days() {
        when(yaYaConfig.getLogRetentionDays()).thenReturn(null);
        when(sysLogMapper.deleteBefore(any())).thenReturn(0);

        sysLogService.cleanExpiredLogs();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sysLogMapper).deleteBefore(captor.capture());
        LocalDateTime cutoff = captor.getValue();
        LocalDateTime lower = LocalDateTime.now().minusDays(30).minusSeconds(5);
        LocalDateTime upper = LocalDateTime.now().minusDays(30).plusSeconds(5);
        assertTrue(cutoff.isAfter(lower) && cutoff.isBefore(upper),
                "截止时间应为当前时间减30天(允许秒级误差)");
    }

    @Test
    @DisplayName("保留策略：保留天数配置不合法时跳过清理")
    void cleanExpiredLogsSkipsInvalidDays() {
        when(yaYaConfig.getLogRetentionDays()).thenReturn(0);

        int deleted = sysLogService.cleanExpiredLogs();

        assertEquals(0, deleted);
        verify(sysLogMapper, never()).deleteBefore(any());
    }
}
