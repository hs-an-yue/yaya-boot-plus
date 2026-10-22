package com.yaya.system.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.vo.CaptchaVo;
import com.yaya.system.service.SysCaptchaService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FastByteArrayOutputStream;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

@Transactional
@Service
public class SysCaptchaServiceImpl implements SysCaptchaService {

    @Resource
    private RedisClient redisClient;
    @Resource
    private YaYaConfig yaYaConfig;

    @Override
    public CaptchaVo captchaImage() throws IOException {

        //创建CircleCaptcha对象(使用hutool工具进行验证码生成)
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(Objects.nonNull(yaYaConfig.getCaptchaWidth())?yaYaConfig.getCaptchaWidth():120, Objects.nonNull(yaYaConfig.getCaptchaHeight())?yaYaConfig.getCaptchaHeight():40, Objects.nonNull(yaYaConfig.getCaptchaLength())?yaYaConfig.getCaptchaLength():4, Objects.nonNull(yaYaConfig.getCaptchaCircleCount())?yaYaConfig.getCaptchaCircleCount():5);
        //获取验证码内容
        String code = captcha.getCode();
        /*
         * 生成验证码的唯一标识.
         * 使用非数字 UUID, 避免前端误把 19 位雪花 ID 当成 Number 处理导致精度丢失,
         * 进而出现 uuid 与 Redis key 对不上、提示"验证码已失效"的问题.
         */
        String uuid = IdUtil.fastSimpleUUID();
        //给redis的key设置一个前缀，方便后续清空验证码
        String key = KeyConstant.CAPTCHA_KEY.getKey() +uuid;
        //将验证码保存到redis中,以便于后面进行登录校验
        redisClient.set(key,code,yaYaConfig.getCaptchaTimeout());//带过期时间,过期时间在yml配置文件中进行配置
        /*
         * 将生成的验证码图片进行base64处理
         */
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        //生成图片的Base64编码
        ImageIO.write(captcha.getImage(), "png", os);
        byte[] bytes = os.toByteArray();
        //base64加密
        String encode = Base64.encode(bytes);
        //创建返回对象
        CaptchaVo  captchaVo = new CaptchaVo();
        //图片
        captchaVo.setCaptchaImg("data:image/png;base64,"+encode);
        //redis后缀
        captchaVo.setUuid(uuid);
        return captchaVo;
    }
}
