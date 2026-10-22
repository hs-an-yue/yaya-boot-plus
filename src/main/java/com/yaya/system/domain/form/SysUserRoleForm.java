package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户角色分配入参
 */
@Data
@Schema(description = "用户角色分配入参")
public class SysUserRoleForm {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID列表, 空列表表示清空角色")
    private List<Long> roleIds;
}
