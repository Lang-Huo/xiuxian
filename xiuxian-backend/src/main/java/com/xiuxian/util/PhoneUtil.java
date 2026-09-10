package com.xiuxian.util;

import java.util.regex.Pattern;

/**
 * 手机号校验工具：用户名即手机号（中国大陆 11 位）
 * 正则集中在此，供 DTO 注解（@Pattern）与服务层兜底校验共用，避免多处漂移。
 */
public final class PhoneUtil {

    /** 中国大陆手机号：1 开头，第二位 3-9，共 11 位 */
    public static final String REGEX = "^1[3-9]\\d{9}$";

    /** 统一的错误提示 */
    public static final String MESSAGE = "请输入正确的 11 位手机号";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PhoneUtil() {
    }

    /** 是否为合法手机号（忽略首尾空格） */
    public static boolean isValid(String value) {
        return value != null && PATTERN.matcher(value.trim()).matches();
    }

    /**
     * 校验手机号，非法则抛出参数异常（由全局异常处理器映射为 400）
     * 用于服务层兜底，防止绕过 Controller 校验的内部调用写入脏数据。
     */
    public static void requireValid(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
