package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户分页查询条件")
public class SysUserQuery extends BaseQuery {

    @Schema(description = "用户名/账号(模糊查询)")
    private String username;

    @Schema(description = "昵称(模糊查询)")
    private String nickname;

    @Schema(description = "部门ID(包含该部门及所有子部门)")
    private Long deptId;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 2, message = "状态值不正确")
    @Schema(description = "状态(1-正常,0-禁用,2-锁定)")
    private Integer status;
}
