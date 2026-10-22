package com.yaya.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义配置文件
 * 作用: 封装配置文件(application-*.yml)中的配置信息
 */
@Component
@Data
@ConfigurationProperties(prefix = "yaya") //配置文件中的配置前缀
public class YaYaConfig {
    /**
     * 文件上传到服务器的地址
     */
    private String filePath;
    /**
     * 应用日志的保存路径
     */
    private String logPath;
    /**
     * 验证码过期时间(单位秒)
     */
    private Long captchaTimeout;
    /**
     * 验证码生成数量
     */
    private Integer captchaLength;
    /**
     * 验证码干扰线数量
     */
    private Integer captchaCircleCount;
    /**
     * 验证码宽度
     */
    private Integer captchaWidth;
    /**
     * 验证码高度
     */
    private Integer captchaHeight;
    /**
     * 访问令牌过期时间(单位分钟) 默认30分钟
     */
    private Integer tokenTimeout=30;
    /**
     * 访问令牌续签阈值(单位分钟): 剩余有效期小于该值时, 自动签发新令牌
     */
    private Integer tokenRenewThreshold=20;
    /**
     * 最大密码错误次数,默认3次
     */
    private Integer passwordMaxRetry=3;
    /**
     * 密码连续错误后的临时锁定时长(单位分钟), 默认15分钟
     */
    private Integer passwordLockMinutes=15;
    /**
     * 同一IP在窗口期内允许的最大登录失败次数, 默认20次
     */
    private Integer loginIpMaxRetry=20;
    /**
     * IP登录失败次数统计窗口(单位分钟), 默认5分钟
     */
    private Integer loginIpWindowMinutes=5;
    /**
     * IP达到失败阈值后的封禁时长(单位分钟), 默认10分钟
     */
    private Integer loginIpBlockMinutes=10;
    /**
     * 日志文件保留天数,默认30天(同时用于数据库日志 sys_log 的定时清理)
     */
    private Integer logRetentionDays;
}
