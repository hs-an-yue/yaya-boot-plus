package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户密码重置入参
 */
@Data
@Schema(description = "用户密码重置入参")
public class SysUserPasswordForm {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "新密码")
    private String password;
}
