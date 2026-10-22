package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告用户关联表
 * <p>
 * 联合主键(notice_id, user_id): 使用 mybatisplus-plus 的 @MppMultiId 标注, Mapper 需继承 MppBaseMapper
 *
 * @author yaya
 */
@Data
@TableName("sys_notice_user")
@Schema(description = "公告用户关联表")
public class SysNoticeUser implements Serializable {

    @MppMultiId
    @TableField(value = "notice_id")
    @Schema(description = "公告ID")
    private Long noticeId;

    @MppMultiId
    @TableField(value = "user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField(value = "dept_id")
    @Schema(description = "部门ID")
    private Long deptId;

    @TableField(value = "is_read")
    @Schema(description = "读取状态(0-未读, 1-已读)")
    private Integer isRead;

    @TableField(value = "read_time")
    @Schema(description = "阅读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
