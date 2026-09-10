package com.xiuxian.model.dto;

import java.util.List;

public record GenerateResponse(
        Long topicId,
        String topic,
        List<QuestionView> questions
) {
}
