package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录表单数据封装
 */
@Data
public class LoginForm {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String captcha;

    @NotBlank(message = "uuid不能为空")
    @Schema(description = "uuid")
    private String uuid;
}
