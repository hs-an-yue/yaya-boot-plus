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
 * 岗位表
 *
 * @author yaya
 */
@Data
@TableName("sys_post")
@Schema(description = "岗位表")
public class SysPost implements Serializable {

    @TableId(value = "post_id")
    @Schema(description = "岗位ID,主键")
    private Long postId;

    @TableField(value = "post_name")
    @Schema(description = "岗位名称")
    private String postName;

    @TableField(value = "post_code")
    @Schema(description = "岗位编号")
    private String postCode;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID,关联部门表(sys_dept)主键")
    private Long deptId;

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
