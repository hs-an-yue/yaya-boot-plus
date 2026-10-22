package com.yaya.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.vo.SysDashboardVo;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysDashboardServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 首页统计 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDashboardServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private SysDashboardServiceImpl sysDashboardService;

    @BeforeAll
    static void initEntityMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    @Test
    @DisplayName("首页统计: 返回用户KPI、环比、在线会话和趋势数据")
    void getDashboardReturnsAggregatedStats() {
        // 顺序: 总数、今年、本周、上周、本月、上月、7天、12个月
        when(sysUserMapper.selectCount(any())).thenReturn(
                100L, 10L, 5L, 4L, 12L, 10L,
                1L, 2L, 3L, 4L, 5L, 6L, 7L,
                1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        when(redisClient.findKeys(anyString())).thenReturn(List.of("login_user:a", "login_user:b", "login_user:c"));

        SysDashboardVo result = sysDashboardService.getDashboard();

        assertEquals(100L, result.getTotalUsers());
        assertEquals(10L, result.getThisYearNewUsers());
        assertEquals(5L, result.getWeekNewUsers());
        assertEquals(25.0, result.getWeekCompareRate());
        assertEquals(12L, result.getMonthNewUsers());
        assertEquals(20.0, result.getMonthCompareRate());
        assertEquals(3L, result.getOnlineUsers());
        assertNotNull(result.getOnlineUpdatedAt());
        assertEquals(7, result.getUserGrowth7d().size());
        assertEquals(12, result.getUserGrowth12m().size());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                result.getUserGrowth7d().stream().map(SysDashboardVo.TrendPoint::getValue).toList());
        assertEquals(12L, result.getUserGrowth12m().get(11).getValue());
        assertNotNull(result.getUserGrowth7d().get(0).getKey());
    }

    @Test
    @DisplayName("首页统计: 上一周期为0时环比返回null")
    void compareRateIsNullWhenPreviousIsZero() {
        when(sysUserMapper.selectCount(any())).thenReturn(
                100L, 10L, 5L, 0L, 12L, 0L,
                0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        when(redisClient.findKeys(anyString())).thenReturn(List.of());

        SysDashboardVo result = sysDashboardService.getDashboard();

        assertNull(result.getWeekCompareRate());
        assertNull(result.getMonthCompareRate());
        assertEquals(0L, result.getOnlineUsers());
    }

    @Test
    @DisplayName("首页统计: Redis异常时在线数返回0, 不影响其他统计")
    void onlineCountFallsBackToZeroWhenRedisFails() {
        when(sysUserMapper.selectCount(any())).thenReturn(
                10L, 1L, 1L, 1L, 1L, 1L,
                0L, 0L, 0L, 0L, 0L, 0L, 1L,
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L);
        when(redisClient.findKeys(anyString())).thenThrow(new RuntimeException("redis unavailable"));

        SysDashboardVo result = sysDashboardService.getDashboard();

        assertEquals(0L, result.getOnlineUsers());
        assertEquals(10L, result.getTotalUsers());
        assertEquals(7, result.getUserGrowth7d().size());
    }
}
