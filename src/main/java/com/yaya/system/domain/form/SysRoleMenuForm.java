package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色菜单授权入参
 */
@Data
@Schema(description = "角色菜单授权入参")
public class SysRoleMenuForm {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "菜单ID列表, 空列表表示清空授权")
    private List<Long> menuIds;
}
