package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "角色分页查询条件")
public class SysRoleQuery extends BaseQuery {

    @Schema(description = "角色名称(模糊查询)")
    private String roleName;

    @Schema(description = "角色编号(模糊查询)")
    private String roleCode;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;
}
