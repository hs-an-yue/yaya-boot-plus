package com.yaya.framework.filter;

import cn.hutool.core.util.IdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 生成日志链路ID的过滤器
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) //优先级最高
public class TrackIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "trackId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 如果前端传了X-Track-Id，就沿用
            String trackId = request.getHeader("X-Track-Id");
            if (trackId == null || trackId.isEmpty()) {
                //如果前端没传就自己生成
                trackId = IdUtil.getSnowflakeNextIdStr();
            }
            MDC.put(TRACE_ID_KEY, trackId);
            // 返回给调用方
            response.setHeader("X-Track-Id", trackId);
            long start = System.currentTimeMillis();
            log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(),response.getStatus(), System.currentTimeMillis() - start);
            filterChain.doFilter(request, response);
        }finally {
            MDC.clear();
        }
    }
}
