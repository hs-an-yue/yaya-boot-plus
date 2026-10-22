package com.yaya.monitor.domain.query;

import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 缓存管理分页查询
 */
@Data
@Schema(description = "缓存管理分页查询")
public class SysCacheQuery extends BaseQuery {

    @Size(max = 200, message = "缓存KEY关键字不能超过200字符")
    @Schema(description = "缓存KEY关键字, 支持包含匹配")
    private String cacheKey;
}
