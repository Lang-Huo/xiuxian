package com.xiuxian.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 仙途编号（对外用户ID）工具：6 位数字，首位非 0，全站唯一。
 * 规则集中在此，避免生成/校验逻辑在多处漂移。
 */
public final class UserNoUtil {

    /** 编号长度固定 6 位 */
    public static final int LENGTH = 6;

    /** 取值范围：[100000, 999999]，保证首位不为 0 */
    public static final int MIN = 100_000;
    public static final int MAX = 999_999;

    public static final String REGEX = "^[1-9]\\d{5}$";

    public static final String MESSAGE = "仙途编号为 6 位数字，首位不能为 0";

    /** 生成时的最大重试次数（撞号后重新摇号） */
    public static final int MAX_RETRY = 20;

    private UserNoUtil() {
    }

    /** 随机生成一个合法编号（不保证未被占用，需配合唯一性检查） */
    public static String random() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(MIN, MAX + 1));
    }

    /** 是否为合法的 6 位编号（首位非 0） */
    public static boolean isValid(String value) {
        return value != null && value.matches(REGEX);
    }

    /** 校验，非法则抛出参数异常（全局异常处理器映射为 400） */
    public static void requireValid(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
