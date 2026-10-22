package com.yaya.monitor.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求统计拦截器
 * <p>
 * 1. preHandle 记录请求开始时间, afterCompletion 统计耗时与 5xx;
 * 2. 只统计到达 Controller 的请求(HandlerMethod), 静态资源/文件流不反映接口负载, 不计入;
 * 3. 未通过安全过滤器的请求(401)不会到达这里, 天然不计入。
 */
public class RequestStatsInterceptor implements HandlerInterceptor {

    private static final String ATTR_START = "monitor.request.startMillis";

    private final RequestStatsBuffer buffer;

    public RequestStatsInterceptor(RequestStatsBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        Object start = request.getAttribute(ATTR_START);
        if (start == null) {
            return;
        }
        buffer.record(System.currentTimeMillis() - (long) start, response.getStatus() >= 500);
    }
}
