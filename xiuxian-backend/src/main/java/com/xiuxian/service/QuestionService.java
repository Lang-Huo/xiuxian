package com.xiuxian.service;

import com.xiuxian.config.AiProperties;
import com.xiuxian.mapper.QuestionMapper;
import com.xiuxian.mapper.TopicMapper;
import com.xiuxian.model.dto.GenerateResponse;
import com.xiuxian.model.dto.QuestionView;
import com.xiuxian.model.entity.Question;
import com.xiuxian.model.entity.Topic;
import com.xiuxian.service.ai.GeneratedQuestion;
import com.xiuxian.service.ai.LlmQuestionGenerator;
import com.xiuxian.service.ai.MockQuestionGenerator;
import com.xiuxian.service.ai.QuestionGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 出题编排：接收主题 -> 调用出题引擎 -> 落库 -> 返回视图
 */
@Slf4j
@Service
public class QuestionService {

    private final AiProperties aiProperties;
    private final MockQuestionGenerator mockGenerator;
    private final LlmQuestionGenerator llmGenerator;
    private final TopicMapper topicMapper;
    private final QuestionMapper questionMapper;

    public QuestionService(AiProperties aiProperties,
                           MockQuestionGenerator mockGenerator,
                           @Qualifier("llmQuestionGenerator") LlmQuestionGenerator llmGenerator,
                           TopicMapper topicMapper,
                           QuestionMapper questionMapper) {
        this.aiProperties = aiProperties;
        this.mockGenerator = mockGenerator;
        this.llmGenerator = llmGenerator;
        this.topicMapper = topicMapper;
        this.questionMapper = questionMapper;
    }

    @Transactional
    public GenerateResponse generate(Long userId, String topic, String difficulty, int count) {
        String diff = (difficulty == null || difficulty.isBlank()) ? "入门" : difficulty;
        int n = Math.max(1, Math.min(20, count));

        Topic topicEntity = new Topic();
        topicEntity.setKeyword(topic);
        topicEntity.setDifficulty(diff);
        topicEntity.setCount(n);
        topicEntity.setUserId(userId);
        topicMapper.insert(topicEntity);
        final Long topicId = topicEntity.getId();

        QuestionGenerator generator = aiProperties.isEnabled() ? llmGenerator : mockGenerator;
        List<GeneratedQuestion> generated;
        try {
            generated = generator.generate(topic, diff, n);
        } catch (Exception e) {
            log.warn("出题引擎异常，回退至 Mock：{}", e.getMessage());
            generated = mockGenerator.generate(topic, diff, n);
        }

        List<Question> questions = generated.stream().map(g -> {
            Question q = new Question();
            q.setTopicId(topicId);
            q.setType(g.getType());
            q.setStem(g.getStem());
            q.setOptions(g.getOptions());
            q.setAnswer(g.getAnswer());
            q.setExplanation(g.getExplanation());
            q.setKnowledgePoint(g.getKnowledgePoint());
            q.setSourceUrl(g.getSourceUrl());
            q.setDifficulty(diff);
            return q;
        }).toList();

        List<Question> saved = new ArrayList<>();
        for (Question q : questions) {
            questionMapper.insert(q);
            saved.add(q);
        }

        List<QuestionView> views = saved.stream().map(q -> new QuestionView(
                q.getId(), q.getType(), q.getStem(), q.getOptions(), q.getDifficulty()
        )).toList();

        return new GenerateResponse(topicEntity.getId(), topic, views);
    }
}
