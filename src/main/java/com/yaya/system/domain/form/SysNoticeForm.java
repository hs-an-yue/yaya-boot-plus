package com.yaya.system.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 公告新增/更新入参
 */
@Data
@Schema(description = "公告表单(新增/更新通用入参)")
public class SysNoticeForm {

    @Schema(description = "公告ID,更新时必填")
    private Long noticeId;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200个字符")
    @Schema(description = "公告标题")
    private String noticeTitle;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容,支持富文本/多媒体")
    private String noticeContent;

    @NotNull(message = "公告级别不能为空")
    @Min(value = 0, message = "公告级别值不正确")
    @Max(value = 2, message = "公告级别值不正确")
    @Schema(description = "公告级别(0-普通,1-重要,2-紧急)")
    private Integer noticeLevel;

    @NotNull(message = "公告类型不能为空")
    @Schema(description = "公告类型ID")
    private Long noticeTypeId;

    @Min(value = 0, message = "置顶状态值不正确")
    @Max(value = 1, message = "置顶状态值不正确")
    @Schema(description = "是否置顶(0-否,1-是)")
    private Integer isTop;

    @NotNull(message = "发布范围不能为空")
    @Min(value = 1, message = "发布范围值不正确")
    @Max(value = 3, message = "发布范围值不正确")
    @Schema(description = "发布范围(1-全员,2-指定部门,3-指定用户)")
    private Integer publishScope;

    @Schema(description = "指定部门ID列表(publishScope=2时必填)")
    private List<Long> deptIds;

    @Schema(description = "指定用户ID列表(publishScope=3时必填)")
    private List<Long> userIds;

    @Schema(description = "附件文件ID列表(先调用文件上传接口获取fileId)")
    private List<Long> attachmentFileIds;
}
