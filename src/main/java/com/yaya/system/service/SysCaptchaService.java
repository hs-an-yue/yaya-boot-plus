package com.yaya.system.service;

import com.yaya.system.domain.vo.CaptchaVo;

import java.io.IOException;

/**
 * 验证码管理业务逻辑层
 */
public interface SysCaptchaService {

    /**
     * 验证码生成
     * @return 验证码信息
     */
    CaptchaVo captchaImage() throws IOException;
}
