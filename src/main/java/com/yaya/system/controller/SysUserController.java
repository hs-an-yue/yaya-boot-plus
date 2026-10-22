package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.SysUserForm;
import com.yaya.system.domain.form.SysUserPasswordForm;
import com.yaya.system.domain.form.SysUserPostForm;
import com.yaya.system.domain.form.SysUserRoleForm;
import com.yaya.system.domain.query.SysUserQuery;
import com.yaya.system.domain.vo.SysUserImportResultVo;
import com.yaya.system.domain.vo.SysUserVo;
import com.yaya.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Resource
    private SysUserService sysUserService;

    @Operation(summary = "用户分页")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:search')")
    @PostMapping("/getUserPage")
    public Result<PageDTO<SysUserVo>> getUserPage(@RequestBody @Valid SysUserQuery query) {
        return Result.ok(sysUserService.getUserPage(query));
    }

    @Operation(summary = "用户详情")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:search')")
    @Parameters(value = {
        @Parameter(name = "userId", description = "用户ID", required = true)
    })
    @PostMapping("/getUserDetail")
    public Result<SysUserVo> getUserDetail(@RequestParam("userId") Long userId) {
        return Result.ok(sysUserService.getUserDetail(userId));
    }

    @Operation(summary = "用户下拉选择(昵称/用户名模糊)")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:search')")
    @Parameters(value = {
        @Parameter(name = "keyword", description = "昵称或用户名关键字")
    })
    @PostMapping("/getUserOptions")
    public Result<List<SysUserVo>> getUserOptions(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(sysUserService.getUserOptions(keyword));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:add')")
    @LogCollect(module = "用户管理-新增", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/addUser")
    public Result<Long> addUser(@RequestBody @Valid SysUserForm form) {
        return Result.ok(sysUserService.addUser(form));
    }

    @Operation(summary = "更新用户(仅ROOT/OPERATION, 用户名不可修改)")
    @PreAuthorize("hasAnyRole('ROOT', 'OPERATION') and hasAuthority('user:update')")
    @LogCollect(module = "用户管理-更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateUser")
    public Result<Void> updateUser(@RequestBody @Valid SysUserForm form) {
        return sysUserService.updateUser(form) ? Result.ok() : Result.error("更新用户失败");
    }

    @Operation(summary = "删除用户(逻辑删除, ROOT不允许删除)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:del')")
    @Parameters(value = {
        @Parameter(name = "userId", description = "用户ID", required = true)
    })
    @LogCollect(module = "用户管理-删除", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/deleteUser")
    public Result<Void> deleteUser(@RequestParam("userId") Long userId) {
        return sysUserService.deleteUser(userId) ? Result.ok() : Result.error("删除用户失败");
    }

    @Operation(summary = "重置密码(ROOT不允许重置)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:reset')")
    @LogCollect(module = "用户管理-重置密码", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/resetUserPassword")
    public Result<Void> resetUserPassword(@RequestBody @Valid SysUserPasswordForm form) {
        return sysUserService.resetPassword(form.getUserId(), form.getPassword())
                ? Result.ok() : Result.error("重置密码失败");
    }

    @Operation(summary = "修改用户状态(1-正常 0-禁用 2-锁定; ROOT不允许禁用/锁定)")
    @PreAuthorize("hasRole('ROOT') and (#status == 1 ? hasAuthority('user:enable') : hasAuthority('user:disable'))")
    @Parameters(value = {
        @Parameter(name = "userId", description = "用户ID", required = true),
        @Parameter(name = "status", description = "状态(1-正常,0-禁用,2-锁定)", required = true)
    })
    @LogCollect(module = "用户管理-状态修改", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateUserStatus")
    public Result<Void> updateUserStatus(@RequestParam("userId") Long userId,
                                         @RequestParam("status") Integer status) {
        return sysUserService.updateUserStatus(userId, status)
                ? Result.ok() : Result.error("更新用户状态失败");
    }

    @Operation(summary = "解锁用户")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:unlock')")
    @Parameters(value = {
        @Parameter(name = "userId", description = "用户ID", required = true)
    })
    @LogCollect(module = "用户管理-解锁", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/unlockUser")
    public Result<Void> unlockUser(@RequestParam("userId") Long userId) {
        return sysUserService.unlockUser(userId) ? Result.ok() : Result.error("解锁用户失败");
    }

    @Operation(summary = "分配角色(ROOT账户不允许修改, 不允许分配ROOT角色)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:assign')")
    @LogCollect(module = "用户管理-分配角色", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/assignUserRoles")
    public Result<Void> assignUserRoles(@RequestBody @Valid SysUserRoleForm form) {
        return sysUserService.assignUserRoles(form.getUserId(), form.getRoleIds())
                ? Result.ok() : Result.error("分配角色失败");
    }

    @Operation(summary = "分配岗位")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:assign')")
    @LogCollect(module = "用户管理-分配岗位", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/assignUserPosts")
    public Result<Void> assignUserPosts(@RequestBody @Valid SysUserPostForm form) {
        return sysUserService.assignUserPosts(form.getUserId(), form.getPostIds())
                ? Result.ok() : Result.error("分配岗位失败");
    }

    @Operation(summary = "导出用户(按当前筛选条件)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:export')")
    @ApiResponse(responseCode = "200", description = "Excel文件", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @LogCollect(module = "用户管理-导出")
    @PostMapping("/exportUser")
    public ResponseEntity<byte[]> exportUser(@RequestBody(required = false) SysUserQuery query) {
        byte[] data = sysUserService.exportUsers(query);
        String filename = "用户数据_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(EXCEL_MEDIA_TYPE)
                .contentLength(data.length)
                .body(data);
    }

    @Operation(summary = "导入用户(整批校验, 任意一行错误则全部不导入)")
    @PreAuthorize("hasRole('ROOT') and hasAuthority('user:import')")
    @LogCollect(module = "用户管理-导入")
    @RepeatSubmit(expireTime = 3)
    @PostMapping(value = "/importUser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SysUserImportResultVo> importUser(@RequestParam("file") MultipartFile file) {
        return Result.ok(sysUserService.importUsers(file));
    }

    @Operation(summary = "下载用户导入模板")
    @PreAuthorize("hasRole('ROOT') or hasAuthority('user:import')")
    @ApiResponse(responseCode = "200", description = "Excel模板", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @PostMapping("/getUserImportTemplate")
    public ResponseEntity<byte[]> getUserImportTemplate() {
        byte[] data = sysUserService.getUserImportTemplate();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("用户导入模板.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(EXCEL_MEDIA_TYPE)
                .contentLength(data.length)
                .body(data);
    }
}
