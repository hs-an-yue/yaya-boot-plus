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
 * 公告类型表
 *
 * @author yaya
 */
@Data
@TableName("sys_notice_type")
@Schema(description = "公告类型表")
public class SysNoticeType implements Serializable {

    @TableId(value = "notice_type_id")
    @Schema(description = "公告类型ID,主键")
    private Long noticeTypeId;

    @TableField(value = "notice_type_name")
    @Schema(description = "公告类型名称")
    private String noticeTypeName;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID")
    private Long deptId;

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

    @TableField(value = "status")
    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(1-已删除 0-未删除)")
    private Integer isDeleted;

    @TableField(exist = false)
    @Schema(description = "创建人名称")
    private String createByName;
}
