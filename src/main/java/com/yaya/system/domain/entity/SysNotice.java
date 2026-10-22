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
 * 公告表
 *
 * @author yaya
 */
@Data
@TableName("sys_notice")
@Schema(description = "公告表")
public class SysNotice implements Serializable {

    @TableId(value = "notice_id")
    @Schema(description = "公告ID,主键")
    private Long noticeId;

    @TableField(value = "notice_title")
    @Schema(description = "公告标题")
    private String noticeTitle;

    @TableField(value = "notice_content")
    @Schema(description = "公告内容,可以是文字，图片，视频，音频，表格等多媒体内容")
    private String noticeContent;

    @TableField(value = "notice_level")
    @Schema(description = "公告级别(0-普通, 1-重要, 2-紧急)")
    private Integer noticeLevel;

    @TableField(value = "notice_type_id")
    @Schema(description = "公告类型ID关联(sys_notice_type)表主键")
    private Long noticeTypeId;

    @TableField(value = "is_top")
    @Schema(description = "是否置顶(0-否, 1-是)")
    private Integer isTop;

    @TableField(value = "status")
    @Schema(description = "发布状态(0-草稿,1-已发布)")
    private Integer status;

    @TableField(value = "publish_scope")
    @Schema(description = "发布范围(1-全员,2-指定部门,3-指定用户)")
    private Integer publishScope;

    @TableField(value = "publish_time")
    @Schema(description = "发布时间,草稿为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

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

    @TableField(value = "is_deleted")
    @Schema(description = "逻辑删除标识(1-已删除 0-未删除)")
    private Integer isDeleted;
}
