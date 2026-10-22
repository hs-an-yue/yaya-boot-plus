package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人中心密码修改入参
 */
@Data
@Schema(description = "个人中心密码修改入参")
public class SysPersonalPasswordForm {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 200, message = "原密码不能超过200个字符")
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 200, message = "新密码不能超过200个字符")
    @Schema(description = "新密码")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(max = 200, message = "确认密码不能超过200个字符")
    @Schema(description = "确认新密码")
    private String confirmPassword;
}
