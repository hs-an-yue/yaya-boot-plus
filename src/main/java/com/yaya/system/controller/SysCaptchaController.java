package com.yaya.system.controller;

import com.yaya.framework.dto.Result;
import com.yaya.system.domain.vo.CaptchaVo;
import com.yaya.system.service.SysCaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "验证码管理")
@RestController
@RequestMapping("/system/captcha")
public class SysCaptchaController {

    @Resource
    private SysCaptchaService sysCaptchaService;


    @Operation(summary = "验证码生成")
    @PostMapping(value = "/captchaImage")
    public Result<CaptchaVo> captchaImage() throws IOException {
        return Result.ok(sysCaptchaService.captchaImage());
    }


}
