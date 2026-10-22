package com.yaya.system.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门查询条件
 */
@Data
@Schema(description = "部门查询条件")
public class SysDeptQuery {

    @Schema(description = "部门名称(模糊查询)")
    private String deptName;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;
}
