package com.yaya.framework.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 上传文件主动内容安全头测试
 */
class UploadedFileSecurityFilterTest {

    private final UploadedFileSecurityFilter filter = new UploadedFileSecurityFilter();
    private final FilterChain chain = mock(FilterChain.class);

    @ParameterizedTest
    @CsvSource({
            "/file/2026/10/21/payload.svg, ''",
            "/file/2026/10/21/payload.html, ''",
            "/file/2026/10/21/payload.xhtml, ''",
            "/file/2026/10/21/payload.xht, ''",
            "/file/2026/10/21/payload.XHT, ''",
            "/api/file/2026/10/21/payload.xHt, ''",
            "/app/file/2026/10/21/payload.xht, /app",
            "/app/api/file/2026/10/21/payload.XhTmL, /app",
            "/file/2026/10/21/payload.x%68t, ''",
            "/file/2026/10/21/payload.%78html, ''",
            "/fi%6ce/2026/10/21/payload.xht, ''",
            "/app/api/%66ile/2026/10/21/payload.h%74ml, /app"
    })
    @DisplayName("XHT/XHTML等主动内容兼容大小写和代理/上下文前缀，强制下载并禁止MIME嗅探")
    void activeContentIsForcedToDownload(String uri, String contextPath) throws Exception {
        MockHttpServletRequest request = request(uri);
        request.setContextPath(contextPath);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals("attachment", response.getHeader("Content-Disposition"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        verify(chain).doFilter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/file/2026/10/21/avatar.png",
            "/api/file/2026/10/21/photo.JPG",
            "/file/2026/10/21/document.pdf",
            "/app/file/2026/10/21/video.mp4"
    })
    @DisplayName("图片、PDF和视频保持可预览")
    void safeContentKeepsPreviewMode(String uri) throws Exception {
        MockHttpServletRequest request = request(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader("Content-Disposition"));
        assertNull(response.getHeader("X-Content-Type-Options"));
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("非文件路径不添加安全头")
    void normalApiPathIsIgnored() throws Exception {
        MockHttpServletRequest request = request("/system/file/downloadFile?fileId=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader("Content-Disposition"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/file/2026/10/21/payload.x%",
            "/file/2026/10/21/payload.x%6",
            "/file/2026/10/21/payload.x%GG"
    })
    @DisplayName("非法百分号编码不导致过滤器异常，交由后续框架拒绝")
    void malformedEncodingIsDelegatedToFilterChain(String uri) throws Exception {
        MockHttpServletRequest request = request(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(response.getHeader("Content-Disposition"));
        verify(chain).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
