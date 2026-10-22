package com.yaya.system.service;

import java.util.List;
import java.util.Map;

/**
 * 登录失败次数与临时锁定
 */
public interface SysLoginAttemptService {

    /**
     * 检查账号是否处于临时锁定期, 或当前IP是否被登录限流
     */
    void checkLocked(String username, String ip);

    /**
     * 记录一次密码错误
     */
    LoginFailureResult recordPasswordFailure(String username, String ip);

    /**
     * 清理失败次数和临时锁定
     */
    void clear(Long userId);

    /**
     * 批量查询账号自动锁定剩余秒数
     *
     * @return 用户ID -> 剩余秒数, 只包含仍在锁定期的账号
     */
    Map<Long, Long> getAutoLockRemainingSeconds(List<Long> userIds);

    /**
     * 临时锁定时长(分钟)
     */
    int getLockMinutes();

    /**
     * IP封禁时长(分钟)
     */
    int getIpBlockMinutes();

    /**
     * 一次失败尝试触发的限制结果
     */
    record LoginFailureResult(boolean accountLocked, boolean ipBlocked) {
    }
}
