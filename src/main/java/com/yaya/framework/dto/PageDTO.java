package com.yaya.framework.dto;

import com.yaya.framework.utils.LongNumberSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;


@Data
@Schema(description = "统一分页返回封装")
public class PageDTO<T> implements Serializable {

    @Schema(description = "当前页")
    private Integer pageNum;

    @Schema(description = "页容量")
    private Integer pageSize;

    @Schema(description = "总记录数")
    @JsonSerialize(using = LongNumberSerializer.class)
    private long total;

    @Schema(description = "当前页数据列表")
    private List<T> records;
}
