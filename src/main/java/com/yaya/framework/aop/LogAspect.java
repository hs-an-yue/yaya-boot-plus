package com.yaya.framework.aop;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jthinking.common.util.ip.IPInfo;
import com.jthinking.common.util.ip.IPInfoUtils;
import com.yaya.framework.annotation.LogCollect;
import com.yaya.framework.utils.DesensitizeUtils;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.monitor.domain.entity.SysLog;
import com.yaya.monitor.service.SysLogService;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.LoginForm;
import com.yaya.system.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 统一日志处理
 * 日志收集切面管理类
 * 1. 收集的日志是在控制器层带有@LogCollect注解的方法，没有被@LogCollect注解标记的函数不会被收集
 */
@Order(4) //优先级
@Component
@Slf4j
@Aspect
public class LogAspect {

    /** 请求参数中需要脱敏的字段名(统一小写比较) */
    private static final Set<String> SENSITIVE_FIELD_NAMES =
            Set.of("password", "oldpassword", "newpassword", "confirmpassword", "pwd");

    @Resource
    private SysLogService sysLogService;
    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 环绕通知
     */
    @Around(value = "@annotation(com.yaya.framework.annotation.LogCollect)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. 获取 HTTP 请求信息: 非 Web 请求(如异步线程/内部调用)不记录日志, 直接执行目标方法
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogCollect annotation = method.getAnnotation(LogCollect.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        //当前时间戳
        long startTime = System.currentTimeMillis();
        SysLog sysLog = new SysLog();
        String module = annotation.module();
        //模块名称
        sysLog.setBusinessName(module);
        //请求地址
        HttpServletRequest request = attributes.getRequest();
        String requestURI = request.getRequestURI();
        sysLog.setRequestUrl(requestURI);//请求地址
        //判断是否记录请求参数
        boolean logRequest = annotation.logRequest();
        //判断是否记录响应参数
        boolean logResponse = annotation.logResponse();
        //获取请求参数: 序列化失败不影响业务执行
        Object[] args = joinPoint.getArgs();
        String requestJsonStr = serializeArgs(args);
        if(requestURI.contains("/login")){ //登录参数
            sysLog.setLogType(1);//登录
            /*
                针对登录信息，通过账号查询用户信息
             */
            if(StringUtils.isNotEmpty(requestJsonStr)){
                List<LoginForm> loginForms = JSONUtil.toList(requestJsonStr, LoginForm.class);
                if(CollectionUtils.isNotEmpty(loginForms)){
                    LoginForm loginForm = loginForms.getFirst();
                    SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, loginForm.getUsername()));
                    if(sysUser!=null){
                        sysLog.setOperId(sysUser.getUserId());
                        sysLog.setDeptId(sysUser.getDeptId());
                    }
                    //请求参数密码脱敏
                    loginForm.setPassword(DesensitizeUtils.desensitizeStr(loginForm.getPassword()));
                    //重新json化,恢复成原来的json的格式
                    List<LoginForm> loginForms_ = List.of(loginForm);
                    String jsonStr = JSONUtil.toJsonStr(loginForms_);
                    sysLog.setRequestParams(jsonStr);
                }
            }

        }else {
            Long userId = SecurityUtils.getUserId();
            Long deptId = SecurityUtils.getDeptId();
            if(userId!=null){
                sysLog.setOperId(userId);
            }
            if(deptId!=null){
                sysLog.setDeptId(deptId);
            }
            if(logRequest){ //请求参数
                sysLog.setRequestParams(requestJsonStr);//请求参数
            }
            sysLog.setLogType(2);
        }
        //操作人IP地址
        String ip = request.getRemoteAddr();
        sysLog.setIp(ip);
        //IP对应的城市信息: 解析失败不影响业务执行
        String address = null;
        try {
            IPInfo ipInfo = IPInfoUtils.getIpInfo(ip);
            if (ipInfo != null) {
                address = ipInfo.getAddress();
            }
        } catch (Exception e) {
            log.error("日志-IP地址解析失败, ip={}", ip, e);
        }
        sysLog.setAddress(address);
        //客户端浏览器
        String browser = getBrowser(request);
        sysLog.setBrowser(browser);
        //日志链路追踪
        sysLog.setTrackId(MDC.get("trackId"));

        Object proceed = null;
        try {
            //日志打印
            log.info("params:{}",requestJsonStr);//请求参数
            log.info("ip:{}",ip);//客户端IP
            log.info("address:{}",address);//客户端地址
            log.info("browser:{}",browser);//客户端浏览器
            proceed = joinPoint.proceed();
            if(proceed!=null && logResponse){
                try{
                    String responseJsonStr = JSONUtil.toJsonStr(proceed);
                    sysLog.setResponseResult(responseJsonStr);
                }catch (Exception e){
                    log.error("日志-响应结果解析失败:",e);
                }
            }
            sysLog.setStatus(1);//请求状态
        }catch (Exception e){
            log.error("日志-记录发生异常:",e);
            sysLog.setErrorMsg(e.getMessage());//异常消息
            sysLog.setStatus(0);//请求状态
            throw e;
        }finally {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            sysLog.setExecutionTime(executionTime);// 耗时（毫秒）
            try{
                sysLogService.saveSysLog(sysLog);
            } catch (Exception e) {
                log.error("日志-入库异常:",e);
            }
        }
        return proceed;
    }


    /**
     * 请求参数脱敏: 密码类字段统一替换为 ***, 避免操作日志落库明文密码
     */
    private String maskSensitiveFields(String json) {
        try {
            JSONArray array = JSONUtil.parseArray(json);
            maskSensitiveNode(array);
            return JSONUtil.toJsonStr(array);
        } catch (Exception e) {
            log.error("日志-请求参数脱敏失败:", e);
            // 脱敏失败时放弃记录参数, 避免明文密码落库
            return "[请求参数序列化失败]";
        }
    }

    private void maskSensitiveNode(Object node) {
        if (node instanceof JSONObject jsonObject) {
            for (String key : new ArrayList<>(jsonObject.keySet())) {
                if (SENSITIVE_FIELD_NAMES.contains(key.toLowerCase())) {
                    jsonObject.set(key, "***");
                } else {
                    maskSensitiveNode(jsonObject.get(key));
                }
            }
        } else if (node instanceof JSONArray jsonArray) {
            for (Object child : jsonArray) {
                maskSensitiveNode(child);
            }
        }
    }

    /**
     * 安全序列化请求参数
     * 序列化失败只记录日志并返回占位文本，不影响业务方法执行
     * <p>
     * 注意: MultipartFile 不能直接序列化, 否则会读取文件全部内容导致内存溢出, 这里只记录文件名和大小
     */
    private String serializeArgs(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return null;
        }
        try {
            Object[] logArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof MultipartFile multipartFile) {
                    logArgs[i] = "[上传文件: " + multipartFile.getOriginalFilename()
                            + ", 大小: " + multipartFile.getSize() + "字节]";
                } else {
                    logArgs[i] = args[i];
                }
            }
            return maskSensitiveFields(JSONUtil.toJsonStr(logArgs));
        } catch (Exception e) {
            log.error("日志-请求参数序列化失败:", e);
            return "[请求参数序列化失败]";
        }
    }


    /**
     * 从请求头中解析浏览器品牌和版本
     * @return 例如: "Chrome 122.0.0.0" 或 "Safari 17.2"
     */
    public static String getBrowser(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        // 1. 获取 User-Agent 请求头
        String uaString = request.getHeader("User-Agent");
        if (StringUtils.isEmpty(uaString)) {
            return "Unknown";
        }
        try {
            // 2. 使用 Hutool 解析 User-Agent
            UserAgent ua = UserAgentUtil.parse(uaString);

            // 3. 获取浏览器名称和版本
            String browserName = ua.getBrowser().getName(); // 例如: ChromeW
            if ("Unknown".equalsIgnoreCase(browserName)) {
                return "Unknown Browser";
            }
            return browserName;
        } catch (Exception e) {
            log.error("日志-浏览器解析失败:",e);
            return "Parse Error";
        }
    }
}
