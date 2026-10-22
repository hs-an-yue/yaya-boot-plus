package com.yaya.system.domain.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户新增/更新入参
 */
@Data
@Schema(description = "用户表单(新增/更新通用入参)")
public class SysUserForm {

    @Schema(description = "用户ID,更新时必填")
    private Long userId;

    @Size(max = 200, message = "用户名不能超过200个字符")
    @Schema(description = "用户名(账号),新增必填,创建后不允许修改")
    private String username;

    @Size(max = 200, message = "密码不能超过200个字符")
    @Schema(description = "密码,新增时必填,更新时忽略")
    private String password;

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

    @NotNull(message = "所属部门不能为空")
    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "过期时间,为空表示长期有效")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredTime;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色ID列表,更新时为空表示不修改")
    private List<Long> roleIds;

    @Schema(description = "岗位ID列表,更新时为空表示不修改")
    private List<Long> postIds;
}
