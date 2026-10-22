package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色菜单关联表
 * <p>
 * 联合主键(role_id, menu_id): 使用 mybatisplus-plus 的 @MppMultiId 标注, Mapper 需继承 MppBaseMapper
 *
 * @author yaya
 */
@Data
@TableName("sys_role_menu")
@Schema(description = "角色菜单关联表")
public class SysRoleMenu implements Serializable {

    @MppMultiId
    @TableField(value = "role_id")
    @Schema(description = "角色ID,关联角色表(sys_role)主键")
    private Long roleId;

    @MppMultiId
    @TableField(value = "menu_id")
    @Schema(description = "菜单ID,关联菜单表(sys_menu)主键")
    private Long menuId;
}
