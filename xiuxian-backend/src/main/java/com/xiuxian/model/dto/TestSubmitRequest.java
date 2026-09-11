package com.xiuxian.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 测灵根答题提交：一次性提交 5 道题的答案
 */
public record TestSubmitRequest(
        @NotNull
        List<AnswerItem> answers
) {
    /** 单道题的作答 */
    public record AnswerItem(
            Long questionId,
            String userAnswer
    ) {}
}