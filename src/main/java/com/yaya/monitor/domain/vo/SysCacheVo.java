package com.yaya.monitor.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 缓存管理列表项
 */
@Data
@Schema(description = "缓存管理列表项")
public class SysCacheVo implements Serializable {

    @Schema(description = "缓存KEY")
    private String key;

    @Schema(description = "缓存类型(string/list/set/zset/hash/stream)")
    private String type;

    @Schema(description = "剩余过期时间(秒), -1-永久 -2-已不存在")
    private Long ttl;

    @Schema(description = "大小(字节), 非string类型无法轻量计算时为null")
    private Long size;
}
