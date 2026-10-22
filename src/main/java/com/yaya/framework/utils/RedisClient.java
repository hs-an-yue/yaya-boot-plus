package com.yaya.framework.utils;

import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redis客户端
 */
@Component
public class RedisClient {

    /** INCR 与首次 EXPIRE 原子执行, 避免计数 key 没有过期时间 */
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRE_SCRIPT =
            new DefaultRedisScript<>("""
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 保存数据
     */
    public void set (String key,String value){
        stringRedisTemplate.opsForValue().set(key,value);
    }
    /**
     * 保存数据-过期时间
     * @param key    键
     * @param value  值
     * @param time   过期时间,单位是 秒
     */
    public void set (String key,String value,Long time){
        stringRedisTemplate.opsForValue().set(key,value, Duration.ofSeconds(time));
    }

    /**
     * 保存数据-过期时间
     * @param key    键
     * @param value  值
     * @param time   过期时间,单位是 分钟
     */
    public void set (String key,String value,Integer time){
        stringRedisTemplate.opsForValue().set(key,value, Duration.ofMinutes(time));
    }

    /**
     * 刷新过期时间(只更新 TTL, 不重写 value)
     * 场景: 滑动续期, 避免重新传输整个 value
     *
     * @param key  键
     * @param time 过期时间,单位是 分钟
     */
    public void expire(String key, Integer time){
        stringRedisTemplate.expire(key, Duration.ofMinutes(time));
    }

    /**
     * 通过键获取对应的值
     * @param key 键
     * @return    值
     */
    public String get(String key){
        return stringRedisTemplate.opsForValue().get(key);
    }
    /**
     * 删除单个缓存
     * @param key 键
     */
    public void del(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除批量缓存
     */
    public void del(List<String> keys) {
        if (CollectionUtils.isNotEmpty(keys)) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 删除单个缓存并返回是否实际删除
     */
    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    /**
     * 获取缓存类型(string/list/set/zset/hash/stream)
     */
    public String getType(String key) {
        DataType type = stringRedisTemplate.type(key);
        return type == null || type == DataType.NONE ? null : type.code();
    }

    /**
     * 获取剩余过期时间(秒): -1-永久, -2-key不存在
     */
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 批量获取剩余过期时间(秒), 只返回存在且设置了过期时间的Key
     * 使用 Pipeline 避免用户列表逐行查询 Redis
     */
    public Map<String, Long> getExpireBatch(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Map.of();
        }
        List<String> distinctKeys = new ArrayList<>(new LinkedHashSet<>(keys));
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : distinctKeys) {
                connection.keyCommands().ttl(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        Map<String, Long> expires = new HashMap<>();
        for (int i = 0; i < distinctKeys.size() && i < results.size(); i++) {
            Object result = results.get(i);
            if (result instanceof Number number && number.longValue() > 0) {
                expires.put(distinctKeys.get(i), number.longValue());
            }
        }
        return expires;
    }

    /**
     * 获取缓存大小(字节)
     * 使用 STRLEN 仅能精确计算 string 类型; 其它类型无通用且跨 Redis 版本的轻量计算方式, 返回 null
     */
    public Long getValueSize(String key) {
        if (!"string".equals(getType(key))) {
            return null;
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().strLen(keyBytes));
    }

    /**
     * 判断key是否存在
     */
    public Boolean exists(String key){
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 判断锁是否存在
     */
    public Boolean setIfAbsent(String key,String value,int expireTime,TimeUnit timeUnit){
        return stringRedisTemplate.opsForValue().setIfAbsent(key,value, Duration.ofMillis(timeUnit.toMillis(expireTime)));
    }

    /**
     * 原子递增计数, 并在首次创建时设置过期时间
     *
     * @param key          计数key
     * @param expireSeconds 过期时间(秒)
     * @return 递增后的计数
     */
    public Long incrementWithExpire(String key, long expireSeconds) {
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("过期时间必须大于0");
        }
        return stringRedisTemplate.execute(
                INCREMENT_WITH_EXPIRE_SCRIPT, List.of(key), String.valueOf(expireSeconds));
    }

    public List<String> findKeys(String pattern) {
        List<String> result = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build();
        // 循环迭代游标，把所有分页数据读完
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        return result;
    }

    /**
     * 分页模糊查询key
     * 基于 SCAN 游标扫描: 跳过前 (pageNum-1)*pageSize 个匹配 key 后, 收集本页数据
     * 注意: 页码较深时需扫过前面所有 key, 性能随之下降; key 频繁增删时翻页结果可能有漂移
     * @param pattern  匹配模式, 如 "login:*"; 查询全部传 "*"
     * @param pageNum  页码, 从 1 开始
     * @param pageSize 每页数量
     * @return 本页 key 列表
     */
    public List<String> findKeyPage(String pattern,int pageNum,int pageSize){
        List<String> result = new ArrayList<>();
        if (pageNum < 1 || pageSize < 1) {
            return result;
        }
        long skip = (long) (pageNum - 1) * pageSize;
        long index = 0;
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(Math.max(pageSize, 100))
                .build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext() && result.size() < pageSize) {
                String key = cursor.next();
                if (index++ >= skip) {
                    result.add(key);
                }
            }
        }
        return result;
    }
}
