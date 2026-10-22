package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色自定义部门数据权限入参
 */
@Data
@Schema(description = "角色自定义部门入参")
public class SysRoleDeptForm {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "部门ID列表")
    private List<Long> deptIds;
}
