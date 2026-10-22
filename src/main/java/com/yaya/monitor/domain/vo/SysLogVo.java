package com.yaya.monitor.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yaya.framework.utils.LongNumberSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysLogVo implements Serializable {

    @Schema(description = "操作日志ID,主键")
    private Long logId;

    @Schema(description = "业务/模块名称（如：用户管理-新增用户）")
    private String businessName;

    @Schema(description = "日志类型(1-登陆日志 2-其它操作日志)")
    private Integer logType;

    @Schema(description = "操作用户部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "操作用户ID")
    private Long operId;

    @Schema(description = "操作人昵称")
    private String operName;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "请求参数（JSON字符串）")
    private String requestParams;

    @Schema(description = "方法执行耗时（毫秒）")
    @JsonSerialize(using = LongNumberSerializer.class)
    private Long executionTime;

    @Schema(description = "请求状态(1-成功 0-失败)")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "操作人IP地址")
    private String ip;

    @Schema(description = "IP对应的地址")
    private String address;

    @Schema(description = "客户端浏览器")
    private String browser;

    @Schema(description = "日志链路ID")
    private String trackId;

    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;

}
