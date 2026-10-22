package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.form.SysPostForm;
import com.yaya.system.domain.query.SysPostQuery;
import com.yaya.system.domain.vo.SysPostVo;
import com.yaya.system.service.SysPostService;
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

@Tag(name = "岗位管理")
@RestController
@RequestMapping("/system/post")
public class SysPostController {

    @Resource
    private SysPostService sysPostService;

    @Operation(summary = "岗位分页")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:search')")
    @PostMapping("/getPostPage")
    public Result<PageDTO<SysPostVo>> getPostPage(@RequestBody @Valid SysPostQuery query) {
        return Result.ok(sysPostService.getPostPage(query));
    }

    @Operation(summary = "岗位列表(下拉选择)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:search')")
    @PostMapping("/getPostList")
    public Result<List<SysPost>> getPostList() {
        return Result.ok(sysPostService.getPostList());
    }

    @Operation(summary = "岗位详情")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:search')")
    @Parameters(value = {
        @Parameter(name = "postId", description = "岗位ID", required = true)
    })
    @PostMapping("/getPostDetail")
    public Result<SysPostVo> getPostDetail(@RequestParam("postId") Long postId) {
        return Result.ok(sysPostService.getPostDetail(postId));
    }

    @Operation(summary = "新增岗位")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:add')")
    @LogCollect(module = "岗位管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addPost")
    public Result<Void> addPost(@RequestBody @Valid SysPostForm form) {
        return sysPostService.addPost(form) ? Result.ok() : Result.error("新增岗位失败");
    }

    @Operation(summary = "更新岗位")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:update')")
    @LogCollect(module = "岗位管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updatePost")
    public Result<Void> updatePost(@RequestBody @Valid SysPostForm form) {
        return sysPostService.updatePost(form) ? Result.ok() : Result.error("更新岗位失败");
    }

    @Operation(summary = "删除岗位(关联用户未删除时不允许删除)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('post:del')")
    @Parameters(value = {
        @Parameter(name = "postId", description = "岗位ID", required = true)
    })
    @LogCollect(module = "岗位管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deletePost")
    public Result<Void> deletePost(@RequestParam("postId") Long postId) {
        return sysPostService.deletePost(postId) ? Result.ok() : Result.error("删除岗位失败");
    }

    @Operation(summary = "启用/停用岗位(关联用户未删除时不允许停用)")
    @PreAuthorize("hasRole('ROOT') or (#status == 1 ? hasAuthority('post:enable') : hasAuthority('post:disable'))")
    @Parameters(value = {
        @Parameter(name = "postId", description = "岗位ID", required = true),
        @Parameter(name = "status", description = "状态(1-正常,0-停用)", required = true)
    })
    @LogCollect(module = "岗位管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updatePostStatus")
    public Result<Void> updatePostStatus(@RequestParam("postId") Long postId,
                                         @RequestParam("status") Integer status) {
        return sysPostService.updatePostStatus(postId, status)
                ? Result.ok() : Result.error("更新岗位状态失败");
    }
}
