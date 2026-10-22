package com.yaya.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.MppMultiId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 公告附件关联表
 *
 * @author yaya
 */
@Data
@TableName("sys_notice_file")
@Schema(description = "公告附件关联表")
public class SysNoticeFile implements Serializable {

    @MppMultiId
    @TableField(value = "notice_id")
    @Schema(description = "公告ID")
    private Long noticeId;

    @MppMultiId
    @TableField(value = "file_id")
    @Schema(description = "文件ID")
    private Long fileId;
}
