package com.yaya.framework.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Long 数字序列化器
 * <p>
 * 全局 Jackson 配置把 Long 序列化为字符串（避免雪花 ID 精度丢失），
 * 但分页 total 这类计数字段需要保持数字，使用本序列化器单独覆盖。
 */
public class LongNumberSerializer extends StdSerializer<Long> {

    public LongNumberSerializer() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value);
        }
    }
}
