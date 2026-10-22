package com.yaya.system.domain.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件分页查询条件
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "文件分页查询条件")
public class SysFileQuery extends BaseQuery {

    @Schema(description = "文件名称(模糊查询)")
    private String fileName;

    @Schema(description = "上传时间-开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "上传时间-结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
