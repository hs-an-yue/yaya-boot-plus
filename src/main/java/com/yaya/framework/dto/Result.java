package com.yaya.framework.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yaya.framework.enums.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一返回结果
 *
 * @param <T> 数据类型
 * @author yaya
 */
@Data
@Schema(description = "统一返回结果")
public class Result<T> implements Serializable {
    /**
     * 统一响应提示状态码
     */
    @Schema(description = "响应状态码")
    private int code= HttpStatus.SUCCESS.getCode();
    /**
     * 统一响应提示消息
     */
    @Schema(description = "响应提示消息")
    private String msg=HttpStatus.SUCCESS.getMsg();
    /**
     * 统一响应结果
     */
    @Schema(description = "响应数据")
    private T data;
    /**
     * 服务器响应时间戳
     */
    @Schema(description = "响应时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * MDC链路追踪码
     */
    @Schema(description = "日志链路追踪码")
    private String trackId = MDC.get("trackId");

    public static <T> Result<T> ok(){
        return new Result<>();
    }

    public static <T> Result<T> ok(T data){
        return new Result<>(data);
    }

    public static <T> Result<T> error(HttpStatus httpStatus){
        return new Result<>(httpStatus.getCode(),httpStatus.getMsg());
    }

    public static <T> Result<T> error(String msg){
        return new Result<>(HttpStatus.ERROR.getCode(),msg);
    }

    public Result() {
    }

    public Result(T data) {
        this.data = data;
    }

    public Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
