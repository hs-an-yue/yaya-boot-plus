package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人中心资料更新入参
 */
@Data
@Schema(description = "个人中心资料更新入参")
public class SysPersonalProfileForm {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 100, message = "昵称不能超过100个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 100, message = "手机号不能超过100个字符")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    @Schema(description = "邮箱")
    private String email;

    @Min(value = 0, message = "性别值不正确")
    @Max(value = 2, message = "性别值不正确")
    @Schema(description = "性别(1-男,0-女,2-保密)")
    private Integer sex;
}
