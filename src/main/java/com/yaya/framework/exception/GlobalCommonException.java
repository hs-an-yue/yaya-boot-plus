package com.yaya.framework.exception;

import com.yaya.framework.enums.HttpStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义异常
 */
@Setter
@Getter
public class GlobalCommonException extends RuntimeException{

    private int code= HttpStatus.ERROR.getCode(); //状态码

    public GlobalCommonException(int code, String message) {
        super(message);
        this.code = code;
    }

    public GlobalCommonException(HttpStatus httpStatus){
        super(httpStatus.getMsg());
        this.code = httpStatus.getCode();
    }

    public GlobalCommonException(String message) {
        super(message);
        this.code = HttpStatus.ERROR.getCode();
    }
}
