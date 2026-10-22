package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门表
 *
 * @author yaya
 */
@Data
@TableName("sys_dept")
@Schema(description = "部门表")
public class SysDept implements Serializable {

    @TableId(value = "dept_id")
    @Schema(description = "部门ID,主键")
    private Long deptId;

    @TableField(value = "dept_name")
    @Schema(description = "部门名称")
    private String deptName;

    @TableField(value = "dept_code")
    @Schema(description = "部门编号")
    private String deptCode;

    @TableField(value = "tree_path")
    @Schema(description = "祖宗ID列表,用逗号(,)分割,所有部门的祖宗ID都从最顶部开始,从0开始")
    private String treePath;

    @TableField(value = "parent_id")
    @Schema(description = "父ID,最顶层从0开始")
    private Long parentId;

    @TableField(value = "sort")
    @Schema(description = "排序序号")
    private Integer sort;

    @TableField(value = "status")
    @Schema(description = "状态(1-正常 0-停用)")
    private Integer status;

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(1-已删除 0-未删除)")
    private Integer isDeleted;

    @TableField(value = "remark")
    @Schema(description = "备注")
    private String remark;

    @TableField(value = "create_by")
    @Schema(description = "创建人ID,关联用户表(sys_user)主键")
    private Long createBy;

    @TableField(value = "update_by")
    @Schema(description = "更新人ID,关联用户表(sys_user)主键")
    private Long updateBy;

    @TableField(value = "create_time")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
