package com.yaya.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置
 */
@Configuration
public class Knife4jConfig {

    @Value("${spring.application.version}")
    private String version;

    /**
     * Knife4j基础信息配置
     */
    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("YaYa-Boot-Plus在线API文档")
                        .version(version)
                        .contact(new Contact()
                                .name("YaYa")
                                .email("hd1611756908@163.com")
                                .url("https://hs-an-yue.github.io"))
                        .termsOfService("https://hs-an-yue.github.io")
                        .description("极简中后台管理系统")
                );
    }

}
