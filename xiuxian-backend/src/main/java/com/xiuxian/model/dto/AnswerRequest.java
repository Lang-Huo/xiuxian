package com.xiuxian.model.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotNull Long questionId,
        String userAnswer
) {
}
