package com.yaya.system.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的公告分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "我的公告分页查询条件")
public class SysMyNoticeQuery extends BaseQuery {
}
