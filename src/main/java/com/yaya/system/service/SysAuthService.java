package com.yaya.system.service;

import com.yaya.system.domain.form.LoginForm;
import com.yaya.system.domain.vo.LoginResultVo;

/**
 * 认证管理业务逻辑层
 */
public interface SysAuthService {

    /**
     * 登录
     * @return 登录成功返回信息
     */
    LoginResultVo login(LoginForm loginForm);
}
