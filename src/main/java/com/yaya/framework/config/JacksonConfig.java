package com.yaya.framework.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 全局序列化配置
 * <p>
 * Long类型在前端长度丢失，所以过长的Long类型向前端送的时候，转成string。
 * JS Number 最大安全整数是 2^53-1，雪花 ID 是 19 位 Long，必须按字符串返回。
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局把 Long / long 序列化为字符串
     */
    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("LongToStringModule");
            // 包装类型和基本类型都要注册
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}
