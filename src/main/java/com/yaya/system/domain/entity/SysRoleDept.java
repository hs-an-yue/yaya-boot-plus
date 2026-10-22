package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色部门关联表(自定义数据权限)
 * <p>
 * 联合主键(role_id, dept_id): 使用 mybatisplus-plus 的 @MppMultiId 标注, Mapper 需继承 MppBaseMapper
 *
 * @author yaya
 */
@Data
@TableName("sys_role_dept")
@Schema(description = "角色部门关联表(自定义数据权限)")
public class SysRoleDept implements Serializable {

    @MppMultiId
    @TableField(value = "role_id")
    @Schema(description = "角色ID,关联角色表(sys_role)主键")
    private Long roleId;

    @MppMultiId
    @TableField(value = "dept_id")
    @Schema(description = "部门ID,关联部门表(sys_dept)主键")
    private Long deptId;
}
