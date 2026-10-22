package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单新增/更新入参
 */
@Data
@Schema(description = "菜单表单(新增/更新通用入参)")
public class SysMenuForm {

    @Schema(description = "菜单ID,更新时必填")
    private Long menuId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100, message = "菜单名称不能超过100个字符")
    @Schema(description = "菜单标题")
    private String menuTitle;

    @Size(max = 200, message = "菜单图标不能超过200个字符")
    @Pattern(regexp = "^[A-Za-z0-9 _-]*$", message = "菜单图标格式不正确")
    @Schema(description = "菜单图标")
    private String menuIcon;

    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型不正确")
    @Max(value = 3, message = "菜单类型不正确")
    @Schema(description = "菜单类型(1-目录,2-菜单,3-按钮)")
    private Integer menuType;

    @Size(max = 100, message = "菜单权限标识不能超过100个字符")
    @Schema(description = "菜单权限标识,按钮类型时使用")
    private String perms;

    @Size(max = 200, message = "菜单地址不能超过200个字符")
    @Schema(description = "菜单跳转地址")
    private String menuUrl;

    @Min(value = 0, message = "父菜单ID不能小于0")
    @Schema(description = "父菜单ID,顶层为0")
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
