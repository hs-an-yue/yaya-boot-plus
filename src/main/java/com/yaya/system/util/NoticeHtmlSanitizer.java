package com.yaya.system.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.Locale;

/**
 * 公告富文本白名单清洗
 * <p>
 * 仅保留展示所需的标签/属性, 移除脚本、事件属性、危险协议和 SVG/MathML 等可执行内容。
 */
public final class NoticeHtmlSanitizer {

    private static final String DANGEROUS_TAGS = String.join(",",
            "script", "style", "iframe", "object", "embed", "form", "input", "button",
            "svg", "math", "template", "noscript", "link", "meta", "base",
            "frame", "frameset", "textarea", "select", "option");

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source")
            .addAttributes(":all", "class", "style")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addAttributes("source", "src", "type")
            // 协议由下方 isSafeUrl 统一校验, 避免 Jsoup 把相对地址改写成绝对地址
            .removeProtocols("a", "href", "http", "https", "ftp", "mailto")
            .removeProtocols("img", "src", "http", "https");

    private NoticeHtmlSanitizer() {
    }

    /**
     * 清洗公告 HTML; 输入为空时返回空串
     */
    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().prettyPrint(false);
        // 危险标签连同内容一起删除, 避免 script/style 中的文本被保留
        document.select(DANGEROUS_TAGS).remove();

        for (Element element : document.select("[style]")) {
            String style = element.attr("style").toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            if (style.contains("url(") || style.contains("expression(")
                    || style.contains("javascript:") || style.contains("vbscript:")
                    || style.contains("behavior:")) {
                element.removeAttr("style");
            }
        }
        for (Element element : document.select("[href],[src],[poster]")) {
            if (element.hasAttr("href") && !isSafeUrl(element.attr("href"), true)) {
                element.removeAttr("href");
            }
            if (element.hasAttr("src") && !isSafeUrl(element.attr("src"), false)) {
                element.removeAttr("src");
            }
            if (element.hasAttr("poster") && !isSafeUrl(element.attr("poster"), false)) {
                element.removeAttr("poster");
            }
        }
        for (Element link : document.select("a[href]")) {
            if ("_blank".equalsIgnoreCase(link.attr("target"))) {
                link.attr("rel", "noopener noreferrer");
            }
        }

        return Jsoup.clean(document.body().html(), "", SAFELIST, document.outputSettings());
    }

    private static boolean isSafeUrl(String value, boolean allowContact) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return false;
        }
        // 去掉控制字符/空白后再判断协议, 防止 "java\nscript:" 之类绕过
        String normalized = text.replaceAll("[\\u0000-\\u0020]", "").toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#") || normalized.startsWith("/")
                || normalized.startsWith("./") || normalized.startsWith("../")) {
            return true;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return true;
        }
        if (allowContact && (normalized.startsWith("mailto:") || normalized.startsWith("tel:"))) {
            return true;
        }
        // 没有协议的相对地址(如 file/2026/10/21/a.png)允许
        return !normalized.matches("^[a-z][a-z0-9+.-]*:.*");
    }
}
