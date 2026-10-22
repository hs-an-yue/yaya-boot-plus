package com.yaya.monitor.config;

import com.yaya.monitor.support.RequestStatsBuffer;
import com.yaya.monitor.support.RequestStatsInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 系统监控-请求统计拦截器注册
 * <p>
 * 排除项说明:
 * 1. 监控接口自身: 5 秒一次的轮询会自污染 QPS 数据;
 * 2. 验证码/静态文件/文档: 不反映业务接口负载;
 * 3. /error: 异常转发到 BasicErrorController 会重复计数。
 */
@Configuration
public class MonitorWebConfig implements WebMvcConfigurer {

    @Resource
    private RequestStatsBuffer requestStatsBuffer;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestStatsInterceptor(requestStatsBuffer))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/system/monitor/**",
                        "/system/captcha/**",
                        "/file/**",
                        "/favicon.ico",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/error");
    }
}
