package com.yaya.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 封装返回给前端的验证码信息类
 */
@Data
public class CaptchaVo {

    @Schema(description = "redis中存储的key后缀")
    private String uuid;

    @Schema(description = "验证码图片(base64)")
    private String captchaImg;

}
