package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增/更新入参
 */
@Data
@Schema(description = "角色表单(新增/更新通用入参)")
public class SysRoleForm {

    @Schema(description = "角色ID,更新时必填")
    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 200, message = "角色名称不能超过200个字符")
    @Schema(description = "角色名称")
    private String roleName;

    @Size(max = 200, message = "角色编号不能超过200个字符")
    @Schema(description = "角色编号,新增必填,创建后不允许修改")
    private String roleCode;

    @Min(value = 1, message = "数据权限值不正确")
    @Max(value = 5, message = "数据权限值不正确")
    @Schema(description = "数据权限(1-所有数据 2-部门及子部门 3-本部门 4-本人 5-自定义部门)")
    private Integer dataScope;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
