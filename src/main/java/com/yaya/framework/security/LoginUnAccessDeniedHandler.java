package com.yaya.framework.security;

import com.yaya.framework.dto.Result;
import com.yaya.framework.enums.HttpStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 登录成功,但是访问某些权限较高的资源时，权限不足的处理器
 */
@Component
public class LoginUnAccessDeniedHandler implements AccessDeniedHandler {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        Result result = Result.error(HttpStatus.FORBIDDEN);
        //将消息json化(与业务接口一致的 Jackson 序列化)
        String json = objectMapper.writeValueAsString(result);
        //送到客户端
        response.getWriter().print(json);
    }
}
