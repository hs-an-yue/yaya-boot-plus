package com.yaya.framework.security;

import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

/**
 * SpringSecurity核心配置
 */
@Configuration
@EnableMethodSecurity //开启SpringSecurity角色+权限注解开启
public class SecurityConfig {

    @Resource
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Resource
    private LoginUnAuthenticationEntryPointHandler loginUnAuthenticationEntryPointHandler;

    @Resource
    private LoginUnAccessDeniedHandler loginUnAccessDeniedHandler;

    @Resource
    private LogoutStatusSuccessHandler logoutStatusSuccessHandler;


    /**
     * SpringSecurity过滤器
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);//防止跨站请求伪造
        http.sessionManagement(t->t.sessionCreationPolicy(SessionCreationPolicy.STATELESS));//取消session
        http.authorizeHttpRequests(t->t.requestMatchers(
                "/system/captcha/captchaImage",
                "/system/auth/login",
                "/system/file/downloadFile",//文件下载不需要鉴权
                "/file/**",
                "/actuator/health",//健康检查
                "/actuator/health/liveness",//健康检查 - 存活探针
                "/actuator/health/readiness",//健康检查 - 就绪探针
                "/webjars/**",
                "/v3/api-docs/**",//在线文档元数据(仅开发用), 生产环境由 application-prod.yml 关闭 springdoc 后返回404
                "/favicon.ico",
                "/doc.html"//在线文档入口(仅开发用), 生产环境由 knife4j.production 屏蔽
                )
                .permitAll().dispatcherTypeMatchers(DispatcherType.ASYNC) //配置 AI 模型 Stream 流式对话
                .permitAll().anyRequest().authenticated()); //配置拦截规则

        //把token过滤器放到注销过滤器之前, 这样注销处理器执行时安全上下文中已有当前登录用户,
        //才能按用户名删除Redis中的登录会话; 若放在UsernamePasswordAuthenticationFilter之前,
        //LogoutFilter会先执行, 处理器里取不到用户, 注销不会真正失效token
        http.addFilterBefore(jwtAuthenticationTokenFilter, LogoutFilter.class);
        //注册自定义匿名访问私有资源处理器
        http.exceptionHandling(t->t.authenticationEntryPoint(loginUnAuthenticationEntryPointHandler));
        //注册自定义权限不足处理器
        http.exceptionHandling(t->t.accessDeniedHandler(loginUnAccessDeniedHandler));
        //将自定义注册成功的处理器注册给SpringSecurity
        http.logout(t->t.logoutSuccessHandler(logoutStatusSuccessHandler));
        return http.build();
    }

    /**
     * 配置加密工具
     */
    @Bean
    public PasswordEncoder encoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
