package com.yaya.framework.security;

import com.yaya.framework.dto.Result;
import com.yaya.framework.enums.HttpStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 匿名访问私有资源时的处理器
 * 未登录的用户访问了需要登录的资源，由此处理器处理
 */
@Component
public class LoginUnAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException{
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        Result error = Result.error(HttpStatus.UNAUTHORIZED);
        //使用与业务接口一致的 Jackson 序列化(@JsonFormat 等注解才会生效)
        String json = objectMapper.writeValueAsString(error);
        response.getWriter().print(json);
    }
}
