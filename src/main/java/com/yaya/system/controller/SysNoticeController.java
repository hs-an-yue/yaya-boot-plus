package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.SysNoticeForm;
import com.yaya.system.domain.query.SysMyNoticeQuery;
import com.yaya.system.domain.query.SysNoticeQuery;
import com.yaya.system.domain.vo.SysNoticeVo;
import com.yaya.system.service.SysNoticeService;
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

@Tag(name = "公告管理")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController {

    @Resource
    private SysNoticeService sysNoticeService;

    @Operation(summary = "管理端公告分页(仅管理员)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:search')")
    @PostMapping("/getNoticePage")
    public Result<PageDTO<SysNoticeVo>> getNoticePage(@RequestBody @Valid SysNoticeQuery query) {
        return Result.ok(sysNoticeService.getNoticePage(query));
    }

    @Operation(summary = "我的公告分页(当前用户接收的已发布公告)")
    @PostMapping("/getMyNoticePage")
    public Result<PageDTO<SysNoticeVo>> getMyNoticePage(@RequestBody @Valid SysMyNoticeQuery query) {
        return Result.ok(sysNoticeService.getMyNoticePage(query));
    }

    @Operation(summary = "我的消息详情(仅当前用户接收的已发布公告)")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true)
    })
    @PostMapping("/getMyNoticeDetail")
    public Result<SysNoticeVo> getMyNoticeDetail(@RequestParam("noticeId") Long noticeId) {
        return Result.ok(sysNoticeService.getMyNoticeDetail(noticeId));
    }

    @Operation(summary = "管理端公告详情(仅管理员)")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true)
    })
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:search')")
    @PostMapping("/getNoticeDetail")
    public Result<SysNoticeVo> getNoticeDetail(@RequestParam("noticeId") Long noticeId) {
        return Result.ok(sysNoticeService.getNoticeDetail(noticeId));
    }

    @Operation(summary = "新增公告(草稿)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:add')")
    @LogCollect(module = "公告管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addNotice")
    public Result<Long> addNotice(@RequestBody @Valid SysNoticeForm form) {
        return Result.ok(sysNoticeService.addNotice(form));
    }

    @Operation(summary = "更新公告(已发布公告不允许修改接收范围)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:update')")
    @LogCollect(module = "公告管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateNotice")
    public Result<Void> updateNotice(@RequestBody @Valid SysNoticeForm form) {
        return sysNoticeService.updateNotice(form) ? Result.ok() : Result.error("更新公告失败");
    }

    @Operation(summary = "逻辑删除公告")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true)
    })
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:del')")
    @LogCollect(module = "公告管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteNotice")
    public Result<Void> deleteNotice(@RequestParam("noticeId") Long noticeId) {
        return sysNoticeService.deleteNotice(noticeId) ? Result.ok() : Result.error("删除公告失败");
    }

    @Operation(summary = "发布公告(草稿发布)")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true)
    })
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:publish')")
    @LogCollect(module = "公告管理-发布", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/publishNotice")
    public Result<Void> publishNotice(@RequestParam("noticeId") Long noticeId) {
        return sysNoticeService.publishNotice(noticeId) ? Result.ok() : Result.error("发布公告失败");
    }

    @Operation(summary = "置顶/取消置顶")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true),
        @Parameter(name = "isTop", description = "是否置顶(1-是,0-否)", required = true)
    })
    @PreAuthorize("hasRole('ROOT') or hasAuthority('notice:top')")
    @LogCollect(module = "公告管理-置顶", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateNoticeTop")
    public Result<Void> updateNoticeTop(@RequestParam("noticeId") Long noticeId,
                                        @RequestParam("isTop") Integer isTop) {
        return sysNoticeService.updateNoticeTop(noticeId, isTop)
                ? Result.ok() : Result.error("更新公告置顶状态失败");
    }

    @Operation(summary = "标记当前用户已读")
    @Parameters(value = {
        @Parameter(name = "noticeId", description = "公告ID", required = true)
    })
    @PostMapping("/markNoticeRead")
    public Result<Void> markNoticeRead(@RequestParam("noticeId") Long noticeId) {
        return sysNoticeService.markNoticeRead(noticeId) ? Result.ok() : Result.error("标记已读失败");
    }

    @Operation(summary = "当前用户全部公告标记已读")
    @PostMapping("/markAllNoticeRead")
    public Result<Void> markAllNoticeRead() {
        return sysNoticeService.markAllNoticeRead() ? Result.ok() : Result.error("标记已读失败");
    }

    @Operation(summary = "当前用户未读公告数量")
    @PostMapping("/getUnreadNoticeCount")
    public Result<Long> getUnreadNoticeCount() {
        return Result.ok(sysNoticeService.getUnreadNoticeCount());
    }
}
