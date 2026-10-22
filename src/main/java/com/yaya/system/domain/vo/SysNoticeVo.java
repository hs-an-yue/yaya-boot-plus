package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告列表/详情返回对象
 */
@Data
@Schema(description = "公告信息")
public class SysNoticeVo implements Serializable {

    @Schema(description = "公告ID")
    private Long noticeId;

    @Schema(description = "公告标题")
    private String noticeTitle;

    @Schema(description = "公告内容(列表不返回)")
    private String noticeContent;

    @Schema(description = "公告内容纯文本摘要(我的消息列表)")
    private String noticeSummary;

    @Schema(description = "公告级别(0-普通,1-重要,2-紧急)")
    private Integer noticeLevel;

    @Schema(description = "公告类型ID")
    private Long noticeTypeId;

    @Schema(description = "公告类型名称")
    private String noticeTypeName;

    @Schema(description = "是否置顶(0-否,1-是)")
    private Integer isTop;

    @Schema(description = "发布状态(0-草稿,1-已发布)")
    private Integer status;

    @Schema(description = "发布范围(1-全员,2-指定部门,3-指定用户)")
    private Integer publishScope;

    @Schema(description = "发布时间,草稿为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @Schema(description = "创建人部门ID快照")
    private Long deptId;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人名称")
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "当前用户是否已读(普通用户查询有效)")
    private Integer isRead;

    @Schema(description = "当前用户阅读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @Schema(description = "指定部门ID列表(管理端草稿回显)")
    private List<Long> deptIds;

    @Schema(description = "指定用户ID列表(管理端草稿回显)")
    private List<Long> userIds;

    @Schema(description = "附件列表")
    private List<SysNoticeAttachmentVo> attachments;
}
