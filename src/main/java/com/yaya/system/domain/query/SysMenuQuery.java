package com.yaya.system.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单查询条件
 */
@Data
@Schema(description = "菜单查询条件")
public class SysMenuQuery {

    @Schema(description = "菜单名称(模糊查询)")
    private String menuTitle;

    @Schema(description = "菜单类型(1-目录,2-菜单,3-按钮)")
    private Integer menuType;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;
}
