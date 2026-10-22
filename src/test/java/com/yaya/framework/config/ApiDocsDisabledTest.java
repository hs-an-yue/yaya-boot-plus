package com.yaya.framework.config;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 生产环境关闭在线API文档的回归测试 <br/>
 * 模拟 application-prod.yml 中的 springdoc 关闭配置,
 * 验证 /v3/api-docs 不再返回 OpenAPI 元数据(404), 防止接口结构在生产环境被匿名枚举 <br/>
 * 注意: knife4j 的 production 开关只屏蔽 /doc.html 入口, 挡不住 springdoc 自身的端点
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class ApiDocsDisabledTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    @DisplayName("springdoc关闭后: /v3/api-docs 返回404")
    void apiDocsDisabledReturns404() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("springdoc关闭后: /v3/api-docs/swagger-config 返回404")
    void swaggerConfigDisabledReturns404() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isNotFound());
    }
}
