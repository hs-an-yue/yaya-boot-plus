package com.yaya.framework.utils;

import java.util.regex.Pattern;

/**
 * 账号密码校验工具
 */
public class AccountAndPasswordMatchUtils {

    /**
     * 校验账号只能是英文或者数字或者英文+数字的组合
     * @param account   账号
     * @return true 合法；false 不合法
     */
    public static boolean checkAccount(String account) {
        // 无长度限制
        String reg = "^[a-zA-Z0-9]+$";
        return account.matches(reg);
    }

    /**
     * 校验密码：必须由英文字母 + 数字组成 长度至少6位
     * @param  password 密码
     * @return true 合法；false 不合法
     */
    public static boolean checkPassword(String password) {
        // 正则说明：
        // [a-zA-Z0-9]+$ 只能是字母数字
        String regex = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]{6,}$";
        return Pattern.matches(regex, password);
    }
}
