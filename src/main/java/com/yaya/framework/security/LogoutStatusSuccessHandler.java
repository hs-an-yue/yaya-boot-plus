package com.yaya.framework.security;

import com.yaya.framework.dto.Result;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.utils.RedisClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 注销成功处理器
 */
@Component
public class LogoutStatusSuccessHandler implements LogoutSuccessHandler {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedisClient redisClient;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException{
        //注销操作,需要删redis中的登录信息
        //注意: LogoutFilter 在调用本方法前已通过 SecurityContextLogoutHandler 清空了 SecurityContext,
        //因此这里不能用 SecurityUtils 从上下文取用户, 必须使用入参 authentication(清空前已捕获)
        if(authentication!=null && authentication.getPrincipal() instanceof LoginUserDetails loginUserDetails){
            String username = loginUserDetails.getUsername();
            if(StringUtils.isNotEmpty(username)){
                redisClient.del(KeyConstant.LOGIN_USER_KEY.getKey()+username);
            }
        }

        //返回给客户端注销成功的提示
        response.setCharacterEncoding("utf-8");
        response.setContentType("application/json");
        Result<?> result = Result.ok("注销成功");
        //使用与业务接口一致的 Jackson 序列化(@JsonFormat 等注解才会生效)
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().print(json);
    }
}
