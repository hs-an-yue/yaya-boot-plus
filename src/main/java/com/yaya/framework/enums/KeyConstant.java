package com.yaya.framework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 前缀或者key的常量
 * 1. redis的key常量
 * 2. 认证相关的key常量
 * 3. 其它key常量
 */
@Getter
@AllArgsConstructor
public enum KeyConstant {

    CAPTCHA_KEY("login_captcha:","登录-验证码前缀"),
    LOGIN_USER_KEY("login_user:","登录-用户信息前缀"),
    LOGIN_FAIL_KEY("login_fail:","登录-密码失败次数前缀"),
    LOGIN_LOCK_KEY("login_lock:","登录-临时锁定前缀"),
    LOGIN_IP_FAIL_KEY("login_ip_fail:","登录-IP失败次数前缀"),
    LOGIN_IP_BLOCK_KEY("login_ip_block:","登录-IP封禁前缀"),
    REPEAT_SUBMIT_KEY("repeat_submit","防止重复提交前缀"),
    AUTH_TOKEN_HEADER("Authorization","令牌请求头"),
    AUTH_TOKEN_PREFIX("Bearer ","令牌前缀(注意 Bearer 后有一个空格)"),
    AUTH_RENEW_TOKEN_HEADER("Authorization-Refresh","访问令牌续签响应头"),
    AUTH_SESSION_CLAIM("jti","令牌与会话绑定的声明名(JWT标准字段, 值为登录会话ID)")








    ;
    /**
     * key
     */
    private final String key;

    /**
     * 功能描述
     */
    private final String desc;
}
