package com.yaya.system.controller;

import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.dto.Result;
import com.yaya.system.domain.form.LoginForm;
import com.yaya.system.domain.vo.LoginResultVo;
import com.yaya.system.service.SysAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/system/auth")
public class SysAuthController {

    @Resource
    private SysAuthService sysAuthService;


    @LogCollect(module = "认证管理-登录",logRequest = true)
    @Operation(summary = "登录")
    @PostMapping(value = "/login")
    public Result<LoginResultVo> login(@Valid @RequestBody LoginForm  loginForm){
        return  Result.ok(sysAuthService.login(loginForm));
    }

    @LogCollect(module = "认证管理-Token验证")
    @Operation(summary = "验证token")
    @PostMapping(value = "/checkToken")
    public Result<Object> checkToken(){
        return Result.ok();
    }
}
