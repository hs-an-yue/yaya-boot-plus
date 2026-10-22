package com.yaya.system.controller;

import com.yaya.system.domain.form.SysUserForm;
import com.yaya.system.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 通过真实 Spring 方法安全代理验证用户更新的角色边界, 不连接数据库。 */
class SysUserControllerSecurityTest {

    private AnnotationConfigApplicationContext context;
    private SysUserController controller;
    private SysUserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        context = new AnnotationConfigApplicationContext(SecurityTestConfig.class);
        controller = context.getBean(SysUserController.class);
        userService = context.getBean(SysUserService.class);
        assertTrue(AopUtils.isAopProxy(controller));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROOT", "OPERATION"})
    void allowedRolesWithUserUpdateAuthorityCanUpdate(String role) {
        // 2026-10-22 起 updateUser 表达式为 角色与 user:update 权限取"与", 两者缺一不可
        authenticate("ROLE_" + role, "user:update");
        SysUserForm form = new SysUserForm();
        when(userService.updateUser(form)).thenReturn(true);

        assertEquals(0, controller.updateUser(form).getCode());

        verify(userService).updateUser(form);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROOT", "OPERATION"})
    void allowedRolesWithoutUserUpdateAuthorityDenied(String role) {
        // 有角色但未授予 user:update 按钮权限 → 拒绝(生产环境 ROOT 登录时自动持有全部启用按钮)
        authenticate("ROLE_" + role);

        assertUpdateDenied();
    }

    @Test
    void userUpdateAuthorityAloneCannotUpdate() {
        authenticate("user:update");

        assertUpdateDenied();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "OPERATIONS"})
    void otherRolesCannotUpdateEvenWithUserUpdateAuthority(String role) {
        authenticate("ROLE_" + role, "user:update");

        assertUpdateDenied();
    }

    @Test
    void anonymousCannotUpdate() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertUpdateDenied();
    }

    @Test
    void missingAuthenticationCannotUpdate() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.updateUser(new SysUserForm()));
        verifyNoInteractions(userService);
    }

    private void assertUpdateDenied() {
        assertThrows(AccessDeniedException.class, () -> controller.updateUser(new SysUserForm()));
        verifyNoInteractions(userService);
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "editor", "unused", AuthorityUtils.createAuthorityList(authorities)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class SecurityTestConfig {

        @Bean
        SysUserService sysUserService() {
            return mock(SysUserService.class);
        }

        @Bean
        SysUserController sysUserController() {
            return new SysUserController();
        }
    }
}
