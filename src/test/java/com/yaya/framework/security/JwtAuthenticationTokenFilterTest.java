package com.yaya.framework.security;

import cn.hutool.json.JSONUtil;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.utils.JwtUtils;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysUser;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT过滤器测试: 账号过期校验 + 令牌与会话绑定校验
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationTokenFilterTest {

    private static final String USERNAME = "tom";
    private static final String SESSION_ID = "testsid0001";

    @Mock
    private RedisClient redisClient;

    @Mock
    private YaYaConfig yaYaConfig;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationTokenFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("账号自然过期: 清理会话且不写入认证上下文")
    void expiredAccountClearsSessionAndDoesNotAuthenticate() throws Exception {
        stubSession(LocalDateTime.now().minusSeconds(1));

        filter.doFilter(request(), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(redisClient).deleteKey(loginKey());
        verify(redisClient, never()).expire(eq(loginKey()), anyInt());
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("账号未过期: 正常认证并续期会话")
    void activeAccountAuthenticatesAndRenewsSession() throws Exception {
        stubSession(LocalDateTime.now().plusDays(1));

        filter.doFilter(request(), new MockHttpServletResponse(), filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(redisClient, never()).deleteKey(loginKey());
        verify(redisClient).expire(loginKey(), 30);
    }

    @Test
    @DisplayName("账号长期有效: 正常认证")
    void accountWithoutExpiryAuthenticates() throws Exception {
        stubSession(null);

        filter.doFilter(request(), new MockHttpServletResponse(), filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(redisClient, never()).deleteKey(loginKey());
    }

    @Test
    @DisplayName("会话绑定: jti与会话ID不一致(重登后的旧token)时拒绝认证")
    void mismatchedSessionIdDoesNotAuthenticate() throws Exception {
        stubSession(LocalDateTime.now().plusDays(1));

        // 会话中是 SESSION_ID, token 携带的是另一个会话ID, 模拟用户重新登录后旧token再次请求
        filter.doFilter(requestWithSessionId("another-sid"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(redisClient, never()).expire(eq(loginKey()), anyInt());
        verify(redisClient, never()).deleteKey(loginKey());
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("会话绑定: token缺少jti声明时拒绝认证")
    void missingSessionIdClaimDoesNotAuthenticate() throws Exception {
        stubSession(LocalDateTime.now().plusDays(1));

        // 旧版本/伪造token不带会话声明, 不能仅凭用户名匹配就通过
        String token = JwtUtils.createToken(Map.of("username", USERNAME), 30);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeyConstant.AUTH_TOKEN_HEADER.getKey(),
                KeyConstant.AUTH_TOKEN_PREFIX.getKey() + token);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(redisClient, never()).expire(eq(loginKey()), anyInt());
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private void stubSession(LocalDateTime expiredTime) {
        when(yaYaConfig.getTokenTimeout()).thenReturn(30);
        when(yaYaConfig.getTokenRenewThreshold()).thenReturn(20);

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setDeptId(1L);
        user.setUsername(USERNAME);
        user.setPassword("password");
        user.setExpiredTime(expiredTime);
        user.setIsEnabled(1);
        user.setIsLocked(0);

        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        dept.setDeptName("YaYa");
        dept.setStatus(1);
        dept.setIsDeleted(0);

        LoginUserDetails loginUserDetails = new LoginUserDetails(
                user, List.of(), List.of(), List.of(), dept, List.of());
        loginUserDetails.setSessionId(SESSION_ID);
        when(redisClient.get(loginKey())).thenReturn(JSONUtil.toJsonStr(loginUserDetails));
    }

    private MockHttpServletRequest request() {
        return requestWithSessionId(SESSION_ID);
    }

    private MockHttpServletRequest requestWithSessionId(String sessionId) {
        String token = JwtUtils.createToken(Map.of(
                "username", USERNAME,
                KeyConstant.AUTH_SESSION_CLAIM.getKey(), sessionId), 30);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(KeyConstant.AUTH_TOKEN_HEADER.getKey(),
                KeyConstant.AUTH_TOKEN_PREFIX.getKey() + token);
        return request;
    }

    private String loginKey() {
        return KeyConstant.LOGIN_USER_KEY.getKey() + USERNAME;
    }
}
