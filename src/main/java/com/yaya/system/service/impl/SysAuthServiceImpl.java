package com.yaya.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.JwtUtils;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysRole;
import com.yaya.system.domain.form.LoginForm;
import com.yaya.system.domain.vo.LoginResultVo;
import com.yaya.system.service.SysAuthService;
import com.yaya.system.service.SysLoginAttemptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Transactional
@Service
public class SysAuthServiceImpl implements SysAuthService {

    @Resource
    private RedisClient redisClient;
    @Resource
    private YaYaConfig yaYaConfig;
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private SysLoginAttemptService sysLoginAttemptService;


    @Override
    public LoginResultVo login(LoginForm loginForm) {
        /*
         * 第一步: 验证码校验
         * 1. 从缓存中获取验证码
         * 2. 缓存中的验证码和前端用户输入的验证码进行校验
         */
        String uuidKey = KeyConstant.CAPTCHA_KEY.getKey() + loginForm.getUuid();
        String captcha_ = redisClient.get(uuidKey);
        String captcha = loginForm.getCaptcha();
        if(StringUtils.isEmpty(captcha_)){
            throw new GlobalCommonException("验证码已失效,请重新生成");
        }
        if(!captcha_.equalsIgnoreCase(captcha)){
            throw new GlobalCommonException("验证码输入错误");
        }
        //验证码验证通过后，删除验证码,防止未过期的验证码被反复使用
        redisClient.del(uuidKey);
        // 验证码通过后再检查账号/ IP 限制, 避免完全绕过验证码探测账号状态
        String clientIp = getClientIp();
        sysLoginAttemptService.checkLocked(loginForm.getUsername(), clientIp);
        /*
         * 第二步: 将用户名和密码送给SpringSecurity上线文中,将给权限框架判断对不对。
         */
        //将用户名和密码封装到UsernamePasswordAuthenticationToken对象中
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginForm.getUsername(),loginForm.getPassword());
        try{
            //将封带有用户名和密码的对象authenticationToken送到SpringSecurity框架
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            /*
             * 如果认证成功,SpringSecurity会将用户信息封装到Authentication对象中
             */
            Object principal = authenticate.getPrincipal();//获取用户信息
            if(principal==null){
                throw new GlobalCommonException("登录失败,凭据错误");
            }
            //强转,拿到在SpringSecurity中封装的用户信息
            LoginUserDetails loginUserDetails = (LoginUserDetails) principal;
            // 登录成功说明密码正确, 清空失败次数和临时锁定
            try {
                if (loginUserDetails.getSysUser() != null) {
                    sysLoginAttemptService.clear(loginUserDetails.getSysUser().getUserId());
                }
            } catch (Exception e) {
                log.warn("清理登录失败次数失败, username={}", loginUserDetails.getUsername(), e);
            }
            //生成登录会话ID, 同时写入token载荷(jti)和Redis登录会话, 令牌与会话一一绑定:
            //注销/重登/管理员清理会话后, 旧token因jti与会话不一致立即失效, 不会随新登录复活
            String sessionId = IdUtil.fastSimpleUUID();
            loginUserDetails.setSessionId(sessionId);
            //基于账户(手机号)+会话ID生成token
            String token = JwtUtils.createToken(Map.of(
                    "username",loginUserDetails.getUsername(),
                    KeyConstant.AUTH_SESSION_CLAIM.getKey(),sessionId),yaYaConfig.getTokenTimeout());
            //校验token是否生成成功
            if(StringUtils.isEmpty(token)){
                throw new GlobalCommonException("TOKEN生成失败");
            }
            //以token作为key将用户信息json化,保存到redis中
            String json = JSONUtil.toJsonStr(loginUserDetails);
            //将用户信息保存到redis中
            redisClient.set(KeyConstant.LOGIN_USER_KEY.getKey()+loginUserDetails.getUsername(),json,yaYaConfig.getTokenTimeout());
            //登录成功后给前端发回信息
            LoginResultVo loginResultVo = new LoginResultVo();
            loginResultVo.setToken(token);//令牌
            loginResultVo.setUser(loginUserDetails.getSysUser()); //用户信息
            loginResultVo.setDept(loginUserDetails.getSysDept()); //部门信息
            List<SysRole> roles = loginUserDetails.getSysRoles();
            loginResultVo.setRoles(roles); //角色信息
            loginResultVo.setPermCodes(loginUserDetails.getPermCodes()); //权限编号
            //是否是超级管理员
            loginResultVo.setIsSuper(false);
            if(CollectionUtils.isNotEmpty(roles)){
                List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).distinct().toList();
                if(CollectionUtils.isNotEmpty(roleCodes)){
                    loginResultVo.setIsSuper(roleCodes.stream().anyMatch("ROOT"::equalsIgnoreCase));
                }
            }
            return  loginResultVo;
        }catch (BadCredentialsException e){
            SysLoginAttemptService.LoginFailureResult failure = new SysLoginAttemptService.LoginFailureResult(false, false);
            try {
                failure = sysLoginAttemptService.recordPasswordFailure(loginForm.getUsername(), clientIp);
            } catch (Exception countError) {
                log.error("记录密码错误次数失败, username={}", loginForm.getUsername(), countError);
            }
            if (failure.ipBlocked()) {
                throw new GlobalCommonException("登录尝试过于频繁，请 "
                        + sysLoginAttemptService.getIpBlockMinutes() + " 分钟后再试");
            }
            if (failure.accountLocked()) {
                throw new GlobalCommonException("密码错误次数过多，账号已锁定，请 "
                        + sysLoginAttemptService.getLockMinutes() + " 分钟后再试");
            }
            throw new GlobalCommonException("用户名或密码错误");
        }catch (RuntimeException e){
            log.error("登陆异常:",e);
            switch (e) {
                case AccountExpiredException ignored -> throw new GlobalCommonException("账号已过期");
                case LockedException ignored -> throw new GlobalCommonException("账号被锁定");
                case CredentialsExpiredException ignored -> throw new GlobalCommonException("用户名或密码错误");
                case DisabledException ignored -> throw new GlobalCommonException("账号不可用");
                default -> throw new GlobalCommonException(e.getMessage());
            }
        }
    }

    /**
     * 获取客户端IP; 项目已开启 forward-headers-strategy=native, 代理场景由容器解析转发头
     */
    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? "" : attributes.getRequest().getRemoteAddr();
    }
}
