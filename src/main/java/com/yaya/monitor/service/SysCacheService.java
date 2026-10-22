package com.yaya.monitor.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.monitor.domain.query.SysCacheQuery;
import com.yaya.monitor.domain.vo.SysCacheVo;

/**
 * 缓存管理
 */
public interface SysCacheService {

    /**
     * 分页查询缓存KEY
     */
    PageDTO<SysCacheVo> getCachePage(SysCacheQuery query);

    /**
     * 删除单个缓存
     */
    void deleteCache(String key);

    /**
     * 清空当前 Redis DB 的全部缓存
     *
     * @return 删除的KEY数量
     */
    int clearCache();
}
