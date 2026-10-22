package com.yaya.monitor.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日志记录表
 *
 * @author yaya
 */
@Data
@TableName("sys_log")
@Schema(description = "日志记录表")
public class SysLog implements Serializable {


    @TableId(value = "log_id")
    @Schema(description = "操作日志ID,主键")
    private Long logId;

    @TableField(value = "business_name")
    @Schema(description = "业务/模块名称（如：用户管理-新增用户）")
    private String businessName;

    @TableField(value = "log_type")
    @Schema(description = "日志类型(1-登陆日志 2-其它操作日志)")
    private Integer logType;

    @TableField(value = "dept_id")
    @Schema(description = "操作用户部门ID")
    private Long deptId;

    @TableField(value = "oper_id")
    @Schema(description = "操作用户ID")
    private Long operId;

    @TableField(value = "request_url")
    @Schema(description = "请求地址")
    private String requestUrl;

    @TableField(value = "request_params")
    @Schema(description = "请求参数（JSON字符串）")
    private String requestParams;

    @TableField(value = "response_result")
    @Schema(description = "响应结果（JSON字符串）")
    private String responseResult;

    @TableField(value = "execution_time")
    @Schema(description = "方法执行耗时（毫秒）")
    private Long executionTime;

    @TableField(value = "status")
    @Schema(description = "请求状态(1-成功 0-失败)")
    private Integer status;

    @TableField(value = "error_msg")
    @Schema(description = "错误堆栈信息")
    private String errorMsg;

    @TableField(value = "ip")
    @Schema(description = "操作人IP地址")
    private String ip;

    @TableField(value = "address")
    @Schema(description = "IP对应的地址")
    private String address;

    @TableField(value = "browser")
    @Schema(description = "客户端浏览器")
    private String browser;

    @TableField(value = "track_id")
    @Schema(description = "日志链路ID")
    private String trackId;

    @TableField(value = "oper_time")
    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;
}
