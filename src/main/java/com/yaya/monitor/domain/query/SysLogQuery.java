package com.yaya.monitor.domain.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yaya.framework.dto.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class SysLogQuery extends BaseQuery {

    @Schema(description = "日志类型(1-登陆日志 2-其它操作日志)")
    private Integer logType;

    @Schema(description = "操作人昵称")
    private String operName;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "请求状态(1-成功 0-失败)")
    private Integer status;

    @Schema(description = "操作人IP地址")
    private String ip;

    @Schema(description = "日志链路ID")
    private String trackId;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
