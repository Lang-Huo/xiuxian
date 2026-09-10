package com.xiuxian.service.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 出题器：不依赖任何外部 API，生成结构合法、可游玩的占位题目。
 * 用于 MVP 本地联调；配置 xiuxian.ai.enabled=true 后切换为真实大模型。
 */
@Component("mockQuestionGenerator")
public class MockQuestionGenerator implements QuestionGenerator {

    @Override
    public List<GeneratedQuestion> generate(String topic, String difficulty, int count) {
        List<GeneratedQuestion> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            if (i % 2 == 1) {
                list.add(buildSingle(topic, difficulty, i));
            } else {
                list.add(buildJudge(topic, difficulty, i));
            }
        }
        return list;
    }

    private GeneratedQuestion buildSingle(String topic, String difficulty, int idx) {
        String stem = String.format("【%s】关于「%s」的第 %d 个核心要点，下列说法正确的是？",
                difficulty, topic, idx);
        return GeneratedQuestion.builder()
                .type("SINGLE")
                .stem(stem)
                .options(List.of(
                        "A. " + topic + " 的基础概念与应用场景",
                        "B. " + topic + " 与完全无关领域直接等价",
                        "C. " + topic + " 仅在特定错误前提下成立",
                        "D. " + topic + " 不存在任何实际价值"))
                .answer("A")
                .explanation("解析：在「" + topic + "」中，A 描述了其基础概念与实际应用场景，符合主流认知；"
                        + "B/C/D 均为常见误解。建议结合权威资料进一步学习。（Mock 数据，配置真实大模型后自动替换为精准题目）")
                .knowledgePoint(topic + " 基础概念")
                .sourceUrl("")
                .difficulty(difficulty)
                .build();
    }

    private GeneratedQuestion buildJudge(String topic, String difficulty, int idx) {
        String stem = String.format("【%s】判断题：学习「%s」时，系统性理解其基本原理比死记硬背更有效。",
                difficulty, topic);
        return GeneratedQuestion.builder()
                .type("JUDGE")
                .stem(stem)
                .options(List.of("正确", "错误"))
                .answer("true")
                .explanation("解析：正确。对「" + topic + "」这类知识，理解原理有助于迁移与长期记忆，"
                        + "死记硬背容易在真实场景中失效。（Mock 数据）")
                .knowledgePoint(topic + " 学习方法")
                .sourceUrl("")
                .difficulty(difficulty)
                .build();
    }
}
