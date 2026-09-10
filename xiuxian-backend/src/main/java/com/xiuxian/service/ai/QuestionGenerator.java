package com.xiuxian.service.ai;

import java.util.List;

/**
 * 出题引擎接口：给定主题/难度/数量，产出结构化题目
 */
public interface QuestionGenerator {
    List<GeneratedQuestion> generate(String topic, String difficulty, int count);
}
