package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "岗位分页查询条件")
public class SysPostQuery extends BaseQuery {

    @Schema(description = "岗位名称(模糊查询)")
    private String postName;

    @Schema(description = "岗位编号(模糊查询)")
    private String postCode;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;
}
