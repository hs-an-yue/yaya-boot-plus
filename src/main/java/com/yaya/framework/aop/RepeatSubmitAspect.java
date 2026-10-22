package com.yaya.framework.aop;

import com.yaya.framework.annotation.RepeatSubmit;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防止接口重复提交的aop
 */
@Order(5) //优先级
@Component
@Slf4j
@Aspect
public class RepeatSubmitAspect {

    @Resource
    private RedisClient redisClient;

    @Around(value = "@annotation(com.yaya.framework.annotation.RepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // 1. 构建唯一 Key：用户Token/IP + 请求URI
        String token = request.getHeader("Authorization"); // 假设Token在Header中
        if (token == null) {
            token = request.getRemoteAddr(); // 没登录的用IP兜底
        }
        String requestURI = request.getRequestURI();
        String redisKey = KeyConstant.REPEAT_SUBMIT_KEY.getKey() + token + ":" + requestURI;

        //获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RepeatSubmit repeatSubmit = method.getAnnotation(RepeatSubmit.class);

        // 2. 尝试加锁 (利用 Redis 的 setIfAbsent，即 SETNX)
        int expireTime = repeatSubmit.expireTime();
        Boolean success =  redisClient.setIfAbsent(redisKey,"1",expireTime,TimeUnit.SECONDS);
        // 3. 判断是否重复提交
        if (Boolean.FALSE.equals(success)) {
            // 扔出自定义异常，或者直接返回统一响应对象
            throw new GlobalCommonException(repeatSubmit.message());
        }
        // 4. 执行目标方法
        return joinPoint.proceed();
    }

}
