package com.yaya.monitor.service.impl;

import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import com.yaya.monitor.domain.query.SysCacheQuery;
import com.yaya.monitor.domain.vo.SysCacheVo;
import com.yaya.monitor.service.SysCacheService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 缓存管理
 */
@Service
public class SysCacheServiceImpl implements SysCacheService {

    @Resource
    private RedisClient redisClient;

    @Override
    public PageDTO<SysCacheVo> getCachePage(SysCacheQuery query) {
        List<String> keys = new ArrayList<>(redisClient.findKeys(buildPattern(query.getCacheKey())));
        // SCAN 返回顺序不固定, 排序后再分页, 保证翻页稳定
        Collections.sort(keys);

        int from = (int) Math.min((long) (query.getPageNum() - 1) * query.getPageSize(), keys.size());
        int to = Math.min(from + query.getPageSize(), keys.size());
        List<SysCacheVo> records = new ArrayList<>(Math.max(to - from, 0));
        for (String key : keys.subList(from, to)) {
            SysCacheVo vo = new SysCacheVo();
            vo.setKey(key);
            vo.setType(redisClient.getType(key));
            vo.setTtl(redisClient.getExpire(key));
            vo.setSize(redisClient.getValueSize(key));
            records.add(vo);
        }

        PageDTO<SysCacheVo> page = new PageDTO<>();
        page.setPageNum(query.getPageNum());
        page.setPageSize(query.getPageSize());
        page.setTotal(keys.size());
        page.setRecords(records);
        return page;
    }

    @Override
    public void deleteCache(String key) {
        if (key == null || key.isBlank()) {
            throw new GlobalCommonException("缓存KEY不能为空");
        }
        if (!redisClient.deleteKey(key)) {
            throw new GlobalCommonException("缓存不存在或已过期");
        }
    }

    @Override
    public int clearCache() {
        List<String> keys = redisClient.findKeys("*");
        redisClient.del(keys);
        return keys.size();
    }

    /**
     * 关键字按字面量做包含匹配, 避免用户输入的 * ? [ ] 被当作 Redis glob 通配符
     */
    private String buildPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "*";
        }
        StringBuilder pattern = new StringBuilder("*");
        for (char c : keyword.trim().toCharArray()) {
            if (c == '*' || c == '?' || c == '[' || c == ']' || c == '\\') {
                pattern.append('\\');
            }
            pattern.append(c);
        }
        return pattern.append('*').toString();
    }
}
