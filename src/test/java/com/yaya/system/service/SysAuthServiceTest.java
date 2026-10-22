package com.yaya.system.service;

import cn.hutool.json.JSONUtil;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.framework.utils.JwtUtils;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.form.LoginForm;
import com.yaya.system.domain.vo.LoginResultVo;
import com.yaya.system.service.impl.SysAuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录服务测试: 令牌与会话绑定契约
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAuthServiceTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private YaYaConfig yaYaConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SysLoginAttemptService sysLoginAttemptService;

    @InjectMocks
    private SysAuthServiceImpl sysAuthService;

    @Test
    @DisplayName("登录: token的jti与Redis会话中的会话ID一致")
    void loginBindsTokenToSession() {
        when(redisClient.get(any(String.class))).thenReturn("abcd");
        when(yaYaConfig.getTokenTimeout()).thenReturn(30);
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setDeptId(1L);
        user.setUsername("tom");
        user.setPassword("encoded");
        LoginUserDetails loginUserDetails = new LoginUserDetails(
                user, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new SysDept());
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(loginUserDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        LoginForm form = new LoginForm();
        form.setUsername("tom");
        form.setPassword("whatever");
        form.setCaptcha("ABCD");
        form.setUuid("uuid-1");

        LoginResultVo result = sysAuthService.login(form);

        // token中的jti = 会话对象中的会话ID, 过滤器据此完成令牌与会话的绑定校验
        String jti = JwtUtils.getClaimAsString(
                JwtUtils.validateAndGet(result.getToken()), KeyConstant.AUTH_SESSION_CLAIM.getKey());
        assertNotNull(jti);
        assertEquals(loginUserDetails.getSessionId(), jti);

        // Redis会话JSON中保存同一个会话ID
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisClient).set(eq(KeyConstant.LOGIN_USER_KEY.getKey() + "tom"),
                jsonCaptor.capture(), eq(30));
        LoginUserDetails stored = JSONUtil.toBean(jsonCaptor.getValue(), LoginUserDetails.class);
        assertEquals(jti, stored.getSessionId());
    }
}
