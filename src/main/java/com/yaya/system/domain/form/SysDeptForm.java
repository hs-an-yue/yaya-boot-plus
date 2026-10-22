package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门表单(新增/更新通用入参)
 */
@Data
@Schema(description = "部门表单(新增/更新通用入参)")
public class SysDeptForm {

    @Schema(description = "部门ID,更新时必填")
    private Long deptId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 200, message = "部门名称不能超过200个字符")
    @Schema(description = "部门名称")
    private String deptName;

    @NotBlank(message = "部门编号不能为空")
    @Size(max = 200, message = "部门编号不能超过200个字符")
    @Schema(description = "部门编号")
    private String deptCode;

    @Min(value = 0, message = "父部门ID不能小于0")
    @Schema(description = "父部门ID,顶层为0")
    private Long parentId;

    @Min(value = 0, message = "排序序号不能小于0")
    @Schema(description = "排序序号")
    private Integer sort;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
