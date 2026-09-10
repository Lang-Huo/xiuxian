package com.xiuxian.model.dto;

import java.util.List;

/**
 * 生成题目时返回给前端的视图（不含答案与解析，避免提前偷看）
 */
public record QuestionView(
        Long id,
        String type,
        String stem,
        List<String> options,
        String difficulty
) {
}
