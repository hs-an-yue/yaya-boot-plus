package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 公告部门范围关联表
 *
 * @author yaya
 */
@Data
@TableName("sys_notice_dept")
@Schema(description = "公告部门范围关联表")
public class SysNoticeDept implements Serializable {

    @MppMultiId
    @TableField(value = "notice_id")
    @Schema(description = "公告ID")
    private Long noticeId;

    @MppMultiId
    @TableField(value = "dept_id")
    @Schema(description = "部门ID")
    private Long deptId;
}
