package com.yaya.framework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回状态码
 *
 * @author yaya
 */
@Getter
@AllArgsConstructor
public enum HttpStatus {

    /** 操作成功 */
    SUCCESS(0, "成功"),

    /** 参数错误 */
    BAD_REQUEST(400, "参数错误"),

    /** 未登录 */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /** 无权限访问 */
    FORBIDDEN(403, "权限不足"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),




    //TODO... 在这里自定义你的枚举






    /** 操作失败 */
    ERROR(-1, "系统异常,请稍后再试.");

    /** 状态码 */
    private final int code;

    /** 提示信息 */
    private final String msg;
}
