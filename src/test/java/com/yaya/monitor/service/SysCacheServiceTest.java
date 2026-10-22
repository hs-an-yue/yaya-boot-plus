package com.yaya.monitor.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.RedisClient;
import com.yaya.monitor.domain.query.SysCacheQuery;
import com.yaya.monitor.domain.vo.SysCacheVo;
import com.yaya.monitor.service.impl.SysCacheServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 缓存管理 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysCacheServiceTest {

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private SysCacheServiceImpl sysCacheService;

    @Test
    @DisplayName("缓存管理: 按KEY排序并分页, 返回类型/TTL/大小")
    void getCachePageReturnsSortedPage() {
        when(redisClient.findKeys("*")).thenReturn(List.of("b", "c", "a"));
        when(redisClient.getType(anyString())).thenReturn("string");
        when(redisClient.getExpire(anyString())).thenReturn(120L);
        when(redisClient.getValueSize(anyString())).thenReturn(8L);

        SysCacheQuery query = new SysCacheQuery();
        query.setPageNum(1);
        query.setPageSize(2);
        PageDTO<SysCacheVo> page = sysCacheService.getCachePage(query);

        assertEquals(3, page.getTotal());
        assertEquals(List.of("a", "b"),
                page.getRecords().stream().map(SysCacheVo::getKey).toList());
        assertEquals("string", page.getRecords().get(0).getType());
        assertEquals(120L, page.getRecords().get(0).getTtl());
        assertEquals(8L, page.getRecords().get(0).getSize());
    }

    @Test
    @DisplayName("缓存管理: 搜索结果按字面量转义Redis通配符")
    void getCachePageEscapesGlobCharacters() {
        when(redisClient.findKeys("*a\\*b\\?\\[\\*\\]*")).thenReturn(List.of());
        SysCacheQuery query = new SysCacheQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setCacheKey("a*b?[*]");

        sysCacheService.getCachePage(query);

        verify(redisClient).findKeys("*a\\*b\\?\\[\\*\\]*");
    }

    @Test
    @DisplayName("缓存管理: 删除不存在的缓存抛出业务异常")
    void deleteCacheThrowsWhenMissing() {
        when(redisClient.deleteKey("missing")).thenReturn(false);

        assertThrows(GlobalCommonException.class, () -> sysCacheService.deleteCache("missing"));
    }

    @Test
    @DisplayName("缓存管理: 清空缓存返回删除KEY数量")
    void clearCacheReturnsDeletedCount() {
        when(redisClient.findKeys("*")).thenReturn(List.of("a", "b"));

        assertEquals(2, sysCacheService.clearCache());
        verify(redisClient).del(List.of("a", "b"));
    }

    @Test
    @DisplayName("缓存管理: 清空空DB时删除列表为空")
    void clearCacheHandlesEmptyKeyspace() {
        when(redisClient.findKeys("*")).thenReturn(List.of());

        assertEquals(0, sysCacheService.clearCache());
        verify(redisClient).del(List.of());
        verify(redisClient, never()).deleteKey(anyString());
    }
}
