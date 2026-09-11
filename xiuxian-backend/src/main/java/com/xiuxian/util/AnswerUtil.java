package com.xiuxian.util;

import com.xiuxian.model.entity.Question;

/**
 * 判题工具：答题结算与测灵根共用同一套判定，避免两处规则漂移。
 */
public final class AnswerUtil {

    private AnswerUtil() {
    }

    /** 用户答案是否正确；题目或答案为空一律判错，不抛异常 */
    public static boolean isCorrect(Question q, String userAnswer) {
        if (q == null || userAnswer == null || q.getAnswer() == null) {
            return false;
        }
        String ua = userAnswer.trim();
        if ("JUDGE".equals(q.getType())) {
            return Boolean.parseBoolean(ua) == Boolean.parseBoolean(q.getAnswer().trim());
        }
        return ua.equalsIgnoreCase(q.getAnswer().trim());
    }
}
