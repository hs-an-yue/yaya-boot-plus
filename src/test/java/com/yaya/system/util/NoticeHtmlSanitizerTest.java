package com.yaya.system.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公告富文本白名单清洗测试
 */
class NoticeHtmlSanitizerTest {

    @Test
    @DisplayName("保留正常的公告富文本")
    void keepsSafeRichText() {
        String html = "<h2>标题</h2><p style=\"text-align:center\">正文<strong>加粗</strong></p>"
                + "<img src=\"/api/file/2026/10/21/a.png\" alt=\"图片\">"
                + "<video src=\"/api/file/2026/10/21/a.mp4\" controls></video>";

        String sanitized = NoticeHtmlSanitizer.sanitize(html);

        assertTrue(sanitized.contains("<h2>标题</h2>"), sanitized);
        assertTrue(sanitized.contains("<strong>加粗</strong>"), sanitized);
        assertTrue(sanitized.contains("src=\"/api/file/2026/10/21/a.png\""), sanitized);
        assertTrue(sanitized.contains("src=\"/api/file/2026/10/21/a.mp4\""), sanitized);
        assertTrue(sanitized.contains("controls"), sanitized);
    }

    @Test
    @DisplayName("移除script/style/iframe等危险标签")
    void removesDangerousTags() {
        String html = "<p>正常</p><script>alert(1)</script>"
                + "<style>body{color:red}</style>"
                + "<iframe src=\"https://evil.example\"></iframe>";

        String sanitized = NoticeHtmlSanitizer.sanitize(html);

        assertTrue(sanitized.contains("正常"), sanitized);
        assertFalse(sanitized.contains("script"), sanitized);
        assertFalse(sanitized.contains("alert(1)"), sanitized);
        assertFalse(sanitized.contains("iframe"), sanitized);
        assertFalse(sanitized.contains("evil.example"), sanitized);
    }

    @Test
    @DisplayName("移除事件属性和javascript协议")
    void removesEventHandlersAndDangerousProtocols() {
        String html = "<img src=\"x\" onerror=\"alert(1)\">"
                + "<a href=\"javascript:alert(2)\">点我</a>"
                + "<a href=\"jav&#x61;script:alert(3)\">点我2</a>";

        String sanitized = NoticeHtmlSanitizer.sanitize(html);

        assertFalse(sanitized.contains("onerror"), sanitized);
        assertFalse(sanitized.contains("javascript:"), sanitized);
        assertFalse(sanitized.contains("alert("), sanitized);
        assertTrue(sanitized.contains("点我"), sanitized);
    }

    @Test
    @DisplayName("移除SVG/MathML及xlink:href注入")
    void removesSvgAndXlink() {
        String html = "<svg><a xlink:href=\"javascript:alert(1)\">恶意</a></svg>"
                + "<math><mtext>m</mtext></math><p>正常</p>";

        String sanitized = NoticeHtmlSanitizer.sanitize(html);

        assertFalse(sanitized.contains("svg"), sanitized);
        assertFalse(sanitized.contains("math"), sanitized);
        assertFalse(sanitized.contains("xlink"), sanitized);
        assertFalse(sanitized.contains("javascript"), sanitized);
        assertTrue(sanitized.contains("正常"), sanitized);
    }

    @Test
    @DisplayName("移除危险样式和data协议资源")
    void removesDangerousStyleAndDataProtocol() {
        String html = "<p style=\"background:url(javascript:alert(1))\">正文</p>"
                + "<img src=\"data:image/svg+xml,<svg onload=alert(1)>\">";

        String sanitized = NoticeHtmlSanitizer.sanitize(html);

        assertFalse(sanitized.contains("background"), sanitized);
        assertFalse(sanitized.contains("data:image"), sanitized);
        assertFalse(sanitized.contains("onload"), sanitized);
        assertTrue(sanitized.contains("正文"), sanitized);
    }

    @Test
    @DisplayName("空内容返回空串")
    void blankHtmlReturnsEmpty() {
        assertTrue(NoticeHtmlSanitizer.sanitize(null).isEmpty());
        assertTrue(NoticeHtmlSanitizer.sanitize("  ").isEmpty());
    }
}
