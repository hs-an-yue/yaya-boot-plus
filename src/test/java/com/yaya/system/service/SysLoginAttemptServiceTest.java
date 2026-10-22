package com.yaya.system.service;

import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.impl.SysLoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录失败次数、ROOT豁免和IP限流单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLoginAttemptServiceTest {

    @Mock
    private RedisClient redisClient;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private YaYaConfig yaYaConfig;

    @InjectMocks
    private SysLoginAttemptServiceImpl loginAttemptService;

    @BeforeEach
    void config() {
        when(yaYaConfig.getPasswordMaxRetry()).thenReturn(3);
        when(yaYaConfig.getPasswordLockMinutes()).thenReturn(15);
        when(yaYaConfig.getLoginIpMaxRetry()).thenReturn(20);
        when(yaYaConfig.getLoginIpWindowMinutes()).thenReturn(5);
        when(yaYaConfig.getLoginIpBlockMinutes()).thenReturn(10);
        when(redisClient.incrementWithExpire("login_ip_fail:1.2.3.4", 300L)).thenReturn(1L);
        when(sysUserMapper.selectActiveUserIdByUsername("alice")).thenReturn(123L);
        when(sysUserMapper.selectActiveUserIdByUsername("root")).thenReturn(1L);
        when(sysUserMapper.countRootRoleByUserId(anyLong())).thenReturn(0);
    }

    @Test
    @DisplayName("临时锁定: 锁定期内提示剩余分钟")
    void checkLockedThrowsWithRemainingMinutes() {
        when(redisClient.getExpire("login_ip_block:1.2.3.4")).thenReturn(-2L);
        when(redisClient.getExpire("login_lock:123")).thenReturn(90L);

        GlobalCommonException error = assertThrows(GlobalCommonException.class,
                () -> loginAttemptService.checkLocked("alice", "1.2.3.4"));

        assertTrue(error.getMessage().contains("2 分钟"));
    }

    @Test
    @DisplayName("临时锁定: 没有锁key时正常放行")
    void checkLockedPassesWhenNoLock() {
        when(redisClient.getExpire("login_ip_block:1.2.3.4")).thenReturn(-2L);
        when(redisClient.getExpire("login_lock:123")).thenReturn(-2L);

        loginAttemptService.checkLocked("alice", "1.2.3.4");
    }

    @Test
    @DisplayName("ROOT豁免: 忽略并清理历史账号锁")
    void checkLockedClearsStaleRootLock() {
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);
        when(redisClient.getExpire("login_ip_block:1.2.3.4")).thenReturn(-2L);
        when(redisClient.getExpire("login_lock:1")).thenReturn(60L);

        loginAttemptService.checkLocked("root", "1.2.3.4");

        verify(redisClient).deleteKey("login_lock:1");
        verify(redisClient).deleteKey("login_fail:1");
    }

    @Test
    @DisplayName("IP限流: 封禁期内提示剩余分钟")
    void checkLockedThrowsWhenIpBlocked() {
        when(redisClient.getExpire("login_ip_block:1.2.3.4")).thenReturn(90L);

        GlobalCommonException error = assertThrows(GlobalCommonException.class,
                () -> loginAttemptService.checkLocked("alice", "1.2.3.4"));

        assertTrue(error.getMessage().contains("频繁"));
    }

    @Test
    @DisplayName("失败计数: 普通账号达到阈值时设置临时锁")
    void recordPasswordFailureLocksNormalAccount() {
        when(redisClient.incrementWithExpire("login_fail:123", 900L)).thenReturn(3L);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("alice", "1.2.3.4");

        assertTrue(result.accountLocked());
        assertFalse(result.ipBlocked());
        verify(redisClient).set("login_lock:123", "1", 15);
        verify(redisClient).deleteKey("login_fail:123");
    }

    @Test
    @DisplayName("失败计数: 未达到阈值不锁定")
    void recordPasswordFailureDoesNotLockBelowThreshold() {
        when(redisClient.incrementWithExpire("login_fail:123", 900L)).thenReturn(2L);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("alice", "1.2.3.4");

        assertFalse(result.accountLocked());
        assertFalse(result.ipBlocked());
        verify(redisClient, never()).set(anyString(), anyString(), any(Integer.class));
    }

    @Test
    @DisplayName("失败计数: 用户名不存在时只累计IP失败")
    void recordPasswordFailureIgnoresUnknownUserForAccountLock() {
        when(sysUserMapper.selectActiveUserIdByUsername("nobody")).thenReturn(null);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("nobody", "1.2.3.4");

        assertFalse(result.accountLocked());
        verify(redisClient).incrementWithExpire("login_ip_fail:1.2.3.4", 300L);
    }

    @Test
    @DisplayName("ROOT豁免: 密码错误不写账号锁, 仍累计IP失败")
    void recordPasswordFailureExemptsRootFromAccountLock() {
        when(sysUserMapper.countRootRoleByUserId(1L)).thenReturn(1);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("root", "1.2.3.4");

        assertFalse(result.accountLocked());
        assertFalse(result.ipBlocked());
        verify(redisClient, never()).incrementWithExpire("login_fail:1", 900L);
        verify(redisClient, never()).set("login_lock:1", "1", 15);
        verify(redisClient).incrementWithExpire("login_ip_fail:1.2.3.4", 300L);
    }

    @Test
    @DisplayName("IP限流: 达到阈值时封禁IP并清理计数")
    void recordPasswordFailureBlocksIpAtThreshold() {
        when(yaYaConfig.getLoginIpMaxRetry()).thenReturn(3);
        when(sysUserMapper.selectActiveUserIdByUsername("nobody")).thenReturn(null);
        when(redisClient.incrementWithExpire("login_ip_fail:1.2.3.4", 300L)).thenReturn(3L);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("nobody", "1.2.3.4");

        assertTrue(result.ipBlocked());
        verify(redisClient).set("login_ip_block:1.2.3.4", "1", 10);
        verify(redisClient).deleteKey("login_ip_fail:1.2.3.4");
    }

    @Test
    @DisplayName("大小写不同的用户名使用不同用户ID锁定Key, 互不影响")
    void caseSensitiveUsernamesUseDifferentLockKeys() {
        when(sysUserMapper.selectActiveUserIdByUsername("Alice")).thenReturn(10L);
        when(sysUserMapper.selectActiveUserIdByUsername("alice")).thenReturn(11L);
        when(sysUserMapper.countRootRoleByUserId(10L)).thenReturn(0);
        when(sysUserMapper.countRootRoleByUserId(11L)).thenReturn(0);
        when(redisClient.incrementWithExpire("login_fail:10", 900L)).thenReturn(3L);

        SysLoginAttemptService.LoginFailureResult result =
                loginAttemptService.recordPasswordFailure("Alice", "1.2.3.4");

        assertTrue(result.accountLocked());
        verify(redisClient).set("login_lock:10", "1", 15);
        verify(redisClient, never()).set("login_lock:11", "1", 15);
    }

    @Test
    @DisplayName("清理失败计数和锁定使用用户ID")
    void clearUsesUserId() {
        loginAttemptService.clear(123L);

        verify(redisClient).deleteKey("login_fail:123");
        verify(redisClient).deleteKey("login_lock:123");
    }

    @Test
    @DisplayName("批量查询自动锁定状态按用户ID返回")
    void getAutoLockRemainingSecondsUsesUserIdKeys() {
        when(redisClient.getExpireBatch(List.of("login_lock:123", "login_lock:456")))
                .thenReturn(Map.of("login_lock:123", 120L));

        Map<Long, Long> result = loginAttemptService.getAutoLockRemainingSeconds(List.of(123L, 456L));

        assertEquals(120L, result.get(123L));
        assertFalse(result.containsKey(456L));
    }

    @Test
    @DisplayName("锁定和IP封禁时长使用配置值")
    void durationsUseConfig() {
        assertEquals(15, loginAttemptService.getLockMinutes());
        assertEquals(10, loginAttemptService.getIpBlockMinutes());
    }
}
