package com.yaya.framework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 封装分页条件查询基类
 */
@Data
public class BaseQuery {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "当前页")
    private Integer pageNum;

    @NotNull(message = "页容量不能为空")
    @Min(value = 1, message = "页容量必须大于0")
    @Max(value = 1000, message = "页容量不能超过1000")
    @Schema(description = "页容量")
    private Integer pageSize;

}
