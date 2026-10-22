package com.yaya.system.service;

import com.yaya.system.domain.vo.SysDashboardVo;

/**
 * 首页统计 Service
 */
public interface SysDashboardService {

    /**
     * 查询首页聚合统计
     */
    SysDashboardVo getDashboard();
}
