package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "公告分页查询条件")
public class SysNoticeQuery extends BaseQuery {

    @Schema(description = "公告标题(模糊查询)")
    private String noticeTitle;

    @Schema(description = "公告类型ID")
    private Long noticeTypeId;

    @Min(value = 0, message = "公告级别值不正确")
    @Max(value = 2, message = "公告级别值不正确")
    @Schema(description = "公告级别(0-普通,1-重要,2-紧急)")
    private Integer noticeLevel;

    @Min(value = 0, message = "发布状态值不正确")
    @Max(value = 1, message = "发布状态值不正确")
    @Schema(description = "发布状态(0-草稿,1-已发布)")
    private Integer status;

}
