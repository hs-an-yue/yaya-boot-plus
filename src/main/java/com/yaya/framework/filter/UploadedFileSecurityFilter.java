package com.yaya.framework.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 上传目录静态文件安全头
 * <p>
 * 上传目录允许任意业务文件, 但 html/svg/js 等主动内容不能以内联方式在应用同源执行。
 * 对这些扩展名强制下载并禁止 MIME 嗅探; 图片、PDF 等仍可正常预览。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UploadedFileSecurityFilter extends OncePerRequestFilter {

    private static final Set<String> ACTIVE_CONTENT_EXTENSIONS = Set.of(
            "html", "htm", "xhtml", "xht", "svg", "xml", "js", "mjs", "css");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        try {
            // 静态资源按解码后的路径解析文件；安全头也解码一次，避免编码扩展名或 file 段绕过。
            path = UriUtils.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // 非法编码保留原路径，由后续防火墙/资源处理器拒绝，不改写实际请求。
        }
        int fileIndex = path.indexOf("/file/");
        if (fileIndex >= 0 && isActiveContent(path.substring(fileIndex))) {
            response.setHeader("Content-Disposition", "attachment");
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        filterChain.doFilter(request, response);
    }

    private boolean isActiveContent(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == path.length() - 1) {
            return false;
        }
        return ACTIVE_CONTENT_EXTENSIONS.contains(
                path.substring(dotIndex + 1).toLowerCase(Locale.ROOT));
    }
}
