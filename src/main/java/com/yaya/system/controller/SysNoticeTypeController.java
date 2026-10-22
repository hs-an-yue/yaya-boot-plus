package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.entity.SysNoticeType;
import com.yaya.system.domain.query.SysNoticeTypeQuery;
import com.yaya.system.service.SysNoticeTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公告类型管理")
@RestController
@RequestMapping("/system/noticeType")
public class SysNoticeTypeController {

    @Resource
    private SysNoticeTypeService sysNoticeTypeService;


    @Operation(summary = "查询公告类型列表")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:search')")
    @PostMapping("/getNoticeTypeList")
    public Result<List<SysNoticeType>> getNoticeTypeList() {
        return Result.ok(sysNoticeTypeService.getNoticeTypeList());
    }

    @Operation(summary = "分页查询公告类型")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:search')")
    @PostMapping("/getNoticeTypePage")
    public Result<PageDTO<SysNoticeType>> getNoticeTypePage(@RequestBody @Valid SysNoticeTypeQuery query) {
        return Result.ok(sysNoticeTypeService.getNoticeTypePage(query));
    }

    @Operation(summary = "查询公告类型详情")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:search')")
    @Parameters(value = {
        @Parameter(name = "noticeTypeId",description = "公告类型ID",required = true)
    })
    @PostMapping("/getNoticeTypeDetail")
    public Result<SysNoticeType> getNoticeTypeDetail(@RequestParam("noticeTypeId") Long noticeTypeId) {
        return Result.ok(sysNoticeTypeService.getNoticeTypeDetail(noticeTypeId));
    }

    @Operation(summary = "新增公告类型")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:add')")
    @LogCollect(module = "公告类型-新增",logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addNoticeType")
    public Result<Void> addNoticeType(@RequestBody SysNoticeType noticeType) {
        return sysNoticeTypeService.addNoticeType(noticeType) ? Result.ok() : Result.error("新增公告类型失败");
    }

    @Operation(summary = "更新公告类型")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:update')")
    @LogCollect(module = "公告类型-更新",logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateNoticeType")
    public Result<Void> updateNoticeType(@RequestBody SysNoticeType noticeType) {
        return sysNoticeTypeService.updateNoticeType(noticeType) ? Result.ok() : Result.error("更新公告类型失败");
    }

    @Operation(summary = "删除公告类型")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('noticeType:del')")
    @Parameters(value = {
        @Parameter(name = "noticeTypeId",description = "公告类型ID")
    })
    @LogCollect(module = "公告类型-删除",logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteNoticeType")
    public Result<Void> deleteNoticeType(@RequestParam("noticeTypeId") Long noticeTypeId) {
        return sysNoticeTypeService.deleteNoticeType(noticeTypeId) ? Result.ok() : Result.error("删除公告类型失败");
    }

    @Operation(summary = "启用/停用公告类型")
    @PreAuthorize("hasRole('ROOT') or (#status == 1 ? hasAuthority('noticeType:enable') : hasAuthority('noticeType:disable'))")
    @Parameters(value = {
        @Parameter(name = "noticeTypeId", description = "公告类型ID", required = true),
        @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "公告类型-状态修改",logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateNoticeTypeStatus")
    public Result<Void> updateNoticeTypeStatus(@RequestParam("noticeTypeId") Long noticeTypeId,
                                               @RequestParam("status") Integer status) {
        return sysNoticeTypeService.updateNoticeTypeStatus(noticeTypeId, status)
                ? Result.ok() : Result.error("更新公告类型状态失败");
    }
}
