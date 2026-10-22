package com.yaya.system.controller;

import com.yaya.framework.dto.Result;
import com.yaya.system.domain.vo.SysDashboardVo;
import com.yaya.system.service.SysDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页统计
 */
@Tag(name = "首页统计")
@RestController
@RequestMapping("/system/dashboard")
public class SysDashboardController {

    @Resource
    private SysDashboardService sysDashboardService;

    @Operation(summary = "查询首页统计")
    @PostMapping("/getDashboard")
    public Result<SysDashboardVo> getDashboard() {
        return Result.ok(sysDashboardService.getDashboard());
    }
}
