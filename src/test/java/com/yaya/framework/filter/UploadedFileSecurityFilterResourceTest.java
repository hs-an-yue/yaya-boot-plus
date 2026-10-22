package com.yaya.framework.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 真实静态资源解析与安全头的一致性测试：仅使用临时文件和 MockMvc，不启动服务或连接数据库。
 */
class UploadedFileSecurityFilterResourceTest {

    @TempDir
    private Path uploadRoot;

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        Path fileDirectory = Files.createDirectories(uploadRoot.resolve("file"));
        Files.writeString(fileDirectory.resolve("payload.xht"),
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>test</body></html>");
        Files.writeString(fileDirectory.resolve("payload.html"), "<html><body>test</body></html>");
        Files.writeString(fileDirectory.resolve("preview.png"), "preview-test-resource");
        Files.writeString(fileDirectory.resolve("preview.pdf"), "%PDF-1.4\n%%EOF");

        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "upload-filter-test", Map.of("test.upload-root", uploadRoot.toUri().toString())));
        context.register(ResourceConfiguration.class);
        context.refresh();

        StrictHttpFirewall firewall = new StrictHttpFirewall();
        Filter firewallFilter = (request, response, chain) -> {
            FirewalledRequest firewalledRequest = firewall.getFirewalledRequest((HttpServletRequest) request);
            try {
                chain.doFilter(firewalledRequest, response);
            } finally {
                firewalledRequest.reset();
            }
        };
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new UploadedFileSecurityFilter(), firewallFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "/file/payload.x%68t, application/xhtml+xml",
            "/file/payload.%78ht, application/xhtml+xml",
            "/file/payload.h%74ml, text/html",
            "/fi%6ce/payload.xht, application/xhtml+xml",
            "/%66ile/payload.html, text/html"
    })
    @DisplayName("编码路径实际命中主动内容静态资源时，必须强制下载")
    void encodedActiveResourceIsForcedToDownload(String uri, String contentType) throws Exception {
        // URI 重载保留原始百分号编码，避免字符串模板构建器将其再次编码。
        var result = mockMvc.perform(MockMvcRequestBuilders.get(URI.create(uri))).andReturn();
        var response = result.getResponse();

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.parseMediaType(contentType), MediaType.parseMediaType(response.getContentType()));
        assertEquals("attachment", response.getHeader("Content-Disposition"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/file/preview.png",
            "/file/preview.pdf"
    })
    @DisplayName("真实图片和PDF静态资源继续允许预览")
    void safeResourceKeepsPreviewMode(String uri) throws Exception {
        var response = mockMvc.perform(MockMvcRequestBuilders.get(URI.create(uri)))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Content-Disposition"));
        assertNull(response.getHeader("X-Content-Type-Options"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/file/payload.xht;name=x",
            "/file/payload%2exht",
            "/file/payload.x%2568t",
            "/file/payload.x%00ht"
    })
    @DisplayName("分号、编码点号、双编码和空字符仍由默认防火墙拒绝")
    void rejectedPathsRemainRejectedByDefaultFirewall(String uri) {
        assertThrows(RequestRejectedException.class,
                () -> mockMvc.perform(MockMvcRequestBuilders.get(URI.create(uri))));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    static class ResourceConfiguration implements WebMvcConfigurer {

        private final Environment environment;

        ResourceConfiguration(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**")
                    .addResourceLocations(environment.getRequiredProperty("test.upload-root"));
        }
    }
}
