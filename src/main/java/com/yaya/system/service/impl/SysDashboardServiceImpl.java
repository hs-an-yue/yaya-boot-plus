package com.yaya.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.vo.SysDashboardVo;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysDashboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 首页统计 Service 实现
 * <p>
 * 当前首页只统计用户与在线会话:
 * 1. 用户数据来自 sys_user.create_time, 不新增访问日志表;
 * 2. 当前在线来自 Redis 登录会话;
 * 3. 所有时间边界按服务器时区计算.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SysDashboardServiceImpl implements SysDashboardService {

    private static final String[] WEEK_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private RedisClient redisClient;

    @Override
    public SysDashboardVo getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStartDate = today.withDayOfMonth(1);

        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime lastWeekStart = weekStart.minusWeeks(1);
        // 环比使用上一个完整自然周(上周一至本周一), 不再只取上周同一时段
        LocalDateTime lastWeekEnd = weekStart;
        LocalDateTime monthStart = monthStartDate.atStartOfDay();
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        // 环比使用上一个完整自然月(上月1日至本月1日)
        LocalDateTime lastMonthEnd = monthStart;
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime currentEnd = now.plusNanos(1);

        long totalUsers = countCreated(null, null);
        long thisYearNewUsers = countCreated(yearStart, currentEnd);
        long weekNewUsers = countCreated(weekStart, currentEnd);
        long lastWeekNewUsers = countCreated(lastWeekStart, lastWeekEnd);
        long monthNewUsers = countCreated(monthStart, currentEnd);
        long lastMonthNewUsers = countCreated(lastMonthStart, lastMonthEnd);

        SysDashboardVo vo = new SysDashboardVo();
        vo.setTotalUsers(totalUsers);
        vo.setThisYearNewUsers(thisYearNewUsers);
        vo.setWeekNewUsers(weekNewUsers);
        vo.setWeekCompareRate(compareRate(weekNewUsers, lastWeekNewUsers));
        vo.setMonthNewUsers(monthNewUsers);
        vo.setMonthCompareRate(compareRate(monthNewUsers, lastMonthNewUsers));
        vo.setOnlineUsers(countOnlineSessions());
        vo.setOnlineUpdatedAt(LocalDateTime.now());
        vo.setUserGrowth7d(buildSevenDayGrowth(today));
        vo.setUserGrowth12m(buildTwelveMonthGrowth(YearMonth.from(today)));
        return vo;
    }

    /**
     * 统计最近7天每日新增用户, 按时间升序并补齐0
     */
    private List<SysDashboardVo.TrendPoint> buildSevenDayGrowth(LocalDate today) {
        List<SysDashboardVo.TrendPoint> points = new ArrayList<>(7);
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            long count = countCreated(start, end);
            int weekdayIndex = day.getDayOfWeek().getValue() - 1;
            points.add(new SysDashboardVo.TrendPoint(WEEK_LABELS[weekdayIndex], day.toString(), count));
        }
        return points;
    }

    /**
     * 统计最近12个月每月新增用户, 按时间升序并补齐0
     */
    private List<SysDashboardVo.TrendPoint> buildTwelveMonthGrowth(YearMonth currentMonth) {
        List<SysDashboardVo.TrendPoint> points = new ArrayList<>(12);
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
            long count = countCreated(start, end);
            points.add(new SysDashboardVo.TrendPoint(month.getMonthValue() + "月", month.toString(), count));
        }
        return points;
    }

    /**
     * 统计未删除用户数量; start/end 为空时不限制创建时间
     */
    private long countCreated(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getIsDeleted, 0);
        if (start != null) {
            wrapper.ge(SysUser::getCreateTime, start);
        }
        if (end != null) {
            wrapper.lt(SysUser::getCreateTime, end);
        }
        Long count = sysUserMapper.selectCount(wrapper);
        return Objects.requireNonNullElse(count, 0L);
    }

    /**
     * 当前在线按 Redis 中的登录会话 key 统计; Redis 异常时首页仍可展示其他数据
     */
    private long countOnlineSessions() {
        try {
            return redisClient.findKeys(KeyConstant.LOGIN_USER_KEY.getKey() + "*").size();
        } catch (Exception e) {
            log.error("首页统计-读取在线登录会话失败", e);
            return 0L;
        }
    }

    /**
     * 计算环比; 上一周期为0时不返回百分比, 由前端显示暂无对比
     */
    private Double compareRate(long current, long previous) {
        if (previous <= 0) {
            return null;
        }
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
