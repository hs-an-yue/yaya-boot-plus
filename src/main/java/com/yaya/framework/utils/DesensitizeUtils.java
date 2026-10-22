package com.yaya.framework.utils;

import java.util.regex.Pattern;

/**
 * 通用脱敏工具类（支持：手机号、邮箱、身份证号）
 * 全部带正则校验，只有合法才脱敏，不合法直接返回原文
 */
public class DesensitizeUtils {

    // 1. 手机号正则（严格中国大陆手机号）
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    // 2. 邮箱正则（标准邮箱格式）
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    // 3. 身份证号正则（15位 或 18位）
    private static final String ID_CARD_REGEX = "(^\\d{15}$)|(^\\d{17}([0-9]|X|x)$)";

    // ====================== 手机号脱敏 ======================
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        if (!Pattern.matches(PHONE_REGEX, phone)) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // ====================== 邮箱脱敏 ======================
    public static String desensitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            return email;
        }

        String[] split = email.split("@");
        String prefix = split[0];
        String suffix = split[1];

        if (prefix.length() <= 1) {
            return "*@" + suffix;
        }
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + "@" + suffix;
    }

    // ====================== 身份证号脱敏 ======================
    /**
     * 身份证号脱敏规则：
     * 18位：110101********123X
     * 15位：110101*******123
     * 只保留前6位 + 后3/4位，中间全部隐藏
     */
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }
        // 不合法身份证，不脱敏
        if (!Pattern.matches(ID_CARD_REGEX, idCard)) {
            return idCard;
        }

        int length = idCard.length();
        if (length == 18) {
            // 18位：前6位 + ******** + 后4位
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        } else if (length == 15) {
            // 15位：前6位 + ******* + 后3位
            return idCard.substring(0, 6) + "*******" + idCard.substring(12);
        }
        return idCard;
    }
    // ====================== IP脱敏 ======================
    /**
     * 正则脱敏：保留首尾段，中间用星号代替
     * IPv4 示例: 192.168.1.123 -> 192.***.*.123
     */
    public static String desensitizeIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        // 匹配 IPv4：将中间两段替换为 *** 和 *
        if (ip.contains(".")) {
            return ip.replaceAll("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)", "$1.***.*.$4");
        }
        // 匹配 IPv6：将中间部分替换
        if (ip.contains(":")) {
            // 简单处理：保留前两个部分和最后一个部分，中间用 **** 代替
            String[] parts = ip.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":****:" + parts[parts.length - 1];
            }
        }
        return ip;
    }

    /**
     * 针对通用字符串进行脱敏，字符串长度必须大于5位否则不脱敏
     * @param str 要脱敏的字符串
     * @return 脱敏后的结果
     */
    public static String desensitizeStr(String str) {
        if (str == null || str.length() <= 5) {
            return str;
        }
        // 匹配规则：开头的字符 + 中间任意字符 + 结尾的字符
        // $1 代表留下的前半部分，$2 代表留下的后半部分
        return str.replaceAll("^(.{2}).+(.{2})$", "$1***$2");
    }
}
