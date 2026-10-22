package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.SysPersonalPasswordForm;
import com.yaya.system.domain.form.SysPersonalProfileForm;
import com.yaya.system.domain.vo.SysPersonalProfileVo;
import com.yaya.system.service.SysPersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心, 只操作当前登录用户
 */
@Tag(name = "个人中心")
@RestController
@RequestMapping("/system/personal")
public class SysPersonalController {

    @Resource
    private SysPersonalService sysPersonalService;

    @Operation(summary = "查询个人资料")
    @PostMapping("/getProfile")
    public Result<SysPersonalProfileVo> getProfile() {
        return Result.ok(sysPersonalService.getProfile());
    }

    @Operation(summary = "更新个人资料(昵称/手机号/邮箱/性别, 部门由管理员维护)")
    @LogCollect(module = "个人中心-资料更新", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updateProfile")
    public Result<SysPersonalProfileVo> updateProfile(@Valid @RequestBody SysPersonalProfileForm form) {
        return Result.ok(sysPersonalService.updateProfile(form));
    }

    @Operation(summary = "修改密码(成功后强制重新登录)")
    @LogCollect(module = "个人中心-密码修改")
    @RepeatSubmit(expireTime = 3)
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@Valid @RequestBody SysPersonalPasswordForm form) {
        sysPersonalService.updatePassword(form);
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @LogCollect(module = "个人中心-头像上传", logRequest = true)
    @RepeatSubmit(expireTime = 3)
    @PostMapping(value = "/uploadAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(sysPersonalService.uploadAvatar(file));
    }
}
