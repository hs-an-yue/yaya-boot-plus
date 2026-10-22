package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 岗位新增/更新入参
 */
@Data
@Schema(description = "岗位表单(新增/更新通用入参)")
public class SysPostForm {

    @Schema(description = "岗位ID,更新时必填")
    private Long postId;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 200, message = "岗位名称不能超过200个字符")
    @Schema(description = "岗位名称")
    private String postName;

    @NotBlank(message = "岗位编号不能为空")
    @Size(max = 200, message = "岗位编号不能超过200个字符")
    @Schema(description = "岗位编号,全局唯一")
    private String postCode;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
