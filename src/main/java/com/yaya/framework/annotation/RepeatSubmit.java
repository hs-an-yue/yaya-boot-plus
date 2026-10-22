package com.yaya.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防止重复提交注解
 */
@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 防重限流过期时间（单位：秒）默认2秒内不允许重复提交
     */
    int expireTime() default 2;

    /**
     * 提示信息
     */
    String message() default "请勿重复提交";

}
