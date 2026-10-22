package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告类型分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "公告类型分页查询条件")
public class SysNoticeTypeQuery extends BaseQuery {

    @Schema(description = "类型名称(模糊查询)")
    private String noticeTypeName;

    @Schema(description = "状态(1-正常,0-停用)")
    private Integer status;
}
