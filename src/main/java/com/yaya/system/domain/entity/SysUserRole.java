package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户角色关联表
 * <p>
 * 联合主键(user_id, role_id): 使用 mybatisplus-plus 的 @MppMultiId 标注, Mapper 需继承 MppBaseMapper
 *
 * @author yaya
 */
@Data
@TableName("sys_user_role")
@Schema(description = "用户角色关联表")
public class SysUserRole implements Serializable {

    @MppMultiId
    @TableField(value = "user_id")
    @Schema(description = "用户ID,关联用户表(sys_user)主键")
    private Long userId;

    @MppMultiId
    @TableField(value = "role_id")
    @Schema(description = "角色ID,关联角色表(sys_role)主键")
    private Long roleId;
}
