package com.yaya.system.service.impl;

import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import com.yaya.system.mapper.SysUserMapper;
import com.yaya.system.service.SysLoginAttemptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 登录失败次数与临时锁定
 * 1. 普通账号按用户ID连续失败锁定, 避免用户名大小写不同导致Key冲突
 * 2. ROOT 角色豁免账号锁定, 但同样受IP限流保护
 * 3. 同一IP的登录失败次数超过阈值后临时封禁
 */
@Slf4j
@Service
public class SysLoginAttemptServiceImpl implements SysLoginAttemptService {

    private static final int DEFAULT_MAX_RETRY = 3;
    private static final int DEFAULT_LOCK_MINUTES = 15;
    private static final int DEFAULT_IP_MAX_RETRY = 20;
    private static final int DEFAULT_IP_WINDOW_MINUTES = 5;
    private static final int DEFAULT_IP_BLOCK_MINUTES = 10;

    @Resource
    private RedisClient redisClient;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private YaYaConfig yaYaConfig;

    @Override
    public void checkLocked(String username, String ip) {
        String normalizedIp = normalizeIp(ip);
        if (StringUtils.isNotBlank(normalizedIp)) {
            Long ipTtl = redisClient.getExpire(KeyConstant.LOGIN_IP_BLOCK_KEY.getKey() + normalizedIp);
            if (ipTtl != null && ipTtl != -2) {
                throw new GlobalCommonException("登录尝试过于频繁，请 " + remainingMinutes(ipTtl, ipBlockMinutes()) + " 分钟后再试");
            }
        }

        Long userId = activeUserId(username);
        if (userId == null) {
            return;
        }
        String lockKey = KeyConstant.LOGIN_LOCK_KEY.getKey() + userId;
        Long ttl = redisClient.getExpire(lockKey);
        if (ttl == null || ttl == -2) {
            return;
        }
        if (isRootAccount(userId)) {
            // 兼容升级前已经写入的 ROOT 自动锁, 发现后直接清理, 避免继续拦截
            redisClient.deleteKey(lockKey);
            redisClient.deleteKey(KeyConstant.LOGIN_FAIL_KEY.getKey() + userId);
            log.warn("清理ROOT账号的历史自动锁定, userId={}, username={}", userId, username);
            return;
        }
        throw new GlobalCommonException("账号已临时锁定，请 " + remainingMinutes(ttl, lockMinutes()) + " 分钟后再试");
    }

    @Override
    public LoginFailureResult recordPasswordFailure(String username, String ip) {
        boolean ipBlocked = recordIpFailure(ip);
        boolean accountLocked = false;
        Long userId = activeUserId(username);
        if (userId == null) {
            return new LoginFailureResult(false, ipBlocked);
        }
        if (isRootAccount(userId)) {
            // ROOT 不写账号锁, 但仍计入IP失败次数
            log.warn("ROOT账号密码错误, 跳过账号自动锁定, userId={}, username={}, ip={}", userId, username, ip);
            return new LoginFailureResult(false, ipBlocked);
        }

        String failKey = KeyConstant.LOGIN_FAIL_KEY.getKey() + userId;
        Long attempts = redisClient.incrementWithExpire(failKey, lockMinutes() * 60L);
        if (attempts != null && attempts >= maxRetry()) {
            redisClient.set(KeyConstant.LOGIN_LOCK_KEY.getKey() + userId, "1", lockMinutes());
            redisClient.deleteKey(failKey);
            log.warn("用户 {} (ID={}) 密码连续错误 {} 次, 临时锁定 {} 分钟", username, userId, attempts, lockMinutes());
            accountLocked = true;
        }
        return new LoginFailureResult(accountLocked, ipBlocked);
    }

    @Override
    public void clear(Long userId) {
        if (userId == null) {
            return;
        }
        redisClient.deleteKey(KeyConstant.LOGIN_FAIL_KEY.getKey() + userId);
        redisClient.deleteKey(KeyConstant.LOGIN_LOCK_KEY.getKey() + userId);
    }

    @Override
    public Map<Long, Long> getAutoLockRemainingSeconds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> keyToUserId = new LinkedHashMap<>();
        for (Long userId : userIds) {
            if (userId != null) {
                keyToUserId.put(KeyConstant.LOGIN_LOCK_KEY.getKey() + userId, userId);
            }
        }
        if (keyToUserId.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, Long> expirations = redisClient.getExpireBatch(new ArrayList<>(keyToUserId.keySet()));
            Map<Long, Long> result = new LinkedHashMap<>();
            keyToUserId.forEach((key, userId) -> {
                Long seconds = expirations.get(key);
                if (seconds != null && seconds > 0) {
                    result.put(userId, seconds);
                }
            });
            return result;
        } catch (Exception e) {
            log.warn("批量查询自动锁定状态失败, 用户列表不展示临时锁定", e);
            return Map.of();
        }
    }

    @Override
    public int getLockMinutes() {
        return lockMinutes();
    }

    @Override
    public int getIpBlockMinutes() {
        return ipBlockMinutes();
    }

    /**
     * 记录一次IP失败；达到阈值后写入IP封禁标记
     */
    private boolean recordIpFailure(String ip) {
        String normalizedIp = normalizeIp(ip);
        if (StringUtils.isBlank(normalizedIp)) {
            return false;
        }
        String failKey = KeyConstant.LOGIN_IP_FAIL_KEY.getKey() + normalizedIp;
        Long attempts = redisClient.incrementWithExpire(failKey, ipWindowMinutes() * 60L);
        if (attempts == null || attempts < ipMaxRetry()) {
            return false;
        }
        redisClient.set(KeyConstant.LOGIN_IP_BLOCK_KEY.getKey() + normalizedIp, "1", ipBlockMinutes());
        redisClient.deleteKey(failKey);
        log.warn("IP {} 登录失败达到 {} 次, 封禁 {} 分钟", ip, attempts, ipBlockMinutes());
        return true;
    }

    private Long activeUserId(String username) {
        return StringUtils.isBlank(username)
                ? null
                : sysUserMapper.selectActiveUserIdByUsername(username);
    }

    private boolean isRootAccount(Long userId) {
        return sysUserMapper.countRootRoleByUserId(userId) > 0;
    }

    private int maxRetry() {
        Integer value = yaYaConfig.getPasswordMaxRetry();
        return value == null || value <= 0 ? DEFAULT_MAX_RETRY : value;
    }

    private int lockMinutes() {
        Integer value = yaYaConfig.getPasswordLockMinutes();
        return value == null || value <= 0 ? DEFAULT_LOCK_MINUTES : value;
    }

    private int ipMaxRetry() {
        Integer value = yaYaConfig.getLoginIpMaxRetry();
        return value == null || value <= 0 ? DEFAULT_IP_MAX_RETRY : value;
    }

    private int ipWindowMinutes() {
        Integer value = yaYaConfig.getLoginIpWindowMinutes();
        return value == null || value <= 0 ? DEFAULT_IP_WINDOW_MINUTES : value;
    }

    private int ipBlockMinutes() {
        Integer value = yaYaConfig.getLoginIpBlockMinutes();
        return value == null || value <= 0 ? DEFAULT_IP_BLOCK_MINUTES : value;
    }

    private String normalizeIp(String ip) {
        return StringUtils.isBlank(ip) ? "" : ip.trim().toLowerCase(Locale.ROOT);
    }

    private long remainingMinutes(Long ttlSeconds, int fallbackMinutes) {
        long seconds = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : fallbackMinutes * 60L;
        return Math.max(1L, (seconds + 59L) / 60L);
    }
}
