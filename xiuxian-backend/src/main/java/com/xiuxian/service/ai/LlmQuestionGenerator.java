package com.xiuxian.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuxian.config.AiProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 真实大模型出题器（OpenAI 兼容接口：OpenAI / DeepSeek / 通义 / 文心等）
 * 通过配置 xiuxian.ai.enabled=true 与 api-key 启用。
 */
@Slf4j
@Component("llmQuestionGenerator")
public class LlmQuestionGenerator implements QuestionGenerator {

    private final AiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public LlmQuestionGenerator(AiProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public List<GeneratedQuestion> generate(String topic, String difficulty, int count) {
        String prompt = buildPrompt(topic, difficulty, count);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());

            Map<String, Object> req = new HashMap<>();
            req.put("model", props.getModel());
            req.put("temperature", 0.7);
            req.put("messages", List.of(
                    Map.of("role", "system", "content", "你是一位严谨的题库出题专家，只输出 JSON。"),
                    Map.of("role", "user", "content", prompt)
            ));
            req.put("response_format", Map.of("type", "json_object"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            String url = props.getBaseUrl().replaceAll("/$", "") + "/chat/completions";
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);

            return parse(resp.getBody(), topic, difficulty);
        } catch (Exception e) {
            log.error("大模型出题失败，请检查配置或网络：{}", e.getMessage());
            throw new RuntimeException("大模型出题失败：" + e.getMessage(), e);
        }
    }

    private String buildPrompt(String topic, String difficulty, int count) {
        return """
            请围绕主题「%s」（目标难度：%s）生成 %d 道题目，用于帮助用户学习该知识。
            题型混合：判断题(JUDGE)与单选题(SINGLE)。
            以 JSON 对象返回，结构：{"questions":[ ... ]}
            每道题字段：
              - type: "JUDGE" 或 "SINGLE"
              - stem: 题干
              - options: 字符串数组。JUDGE 固定为 ["正确","错误"]；SINGLE 为 2-4 个带 "A. " "B. " 前缀的选项
              - answer: JUDGE 为 "true"/"false"；SINGLE 为该正确选项的完整文本（与 options 中某项完全一致）
              - explanation: 解析（说明为什么对/错，并补充关键概念与延伸）
              - knowledgePoint: 该知识点名称
              - sourceUrl: 如有可靠来源请给出链接，否则留空字符串
            要求：题目准确、无事实错误，解析清晰可教学。只返回 JSON。
            """.formatted(topic, difficulty, count);
    }

    private List<GeneratedQuestion> parse(String body, String topic, String difficulty) throws Exception {
        Map<String, Object> root = mapper.readValue(body, new TypeReference<>() {});
        String content;
        if (root.containsKey("choices")) {
            // OpenAI 标准结构，需再解析 content 中的 JSON
            Map<String, Object> choice = ((List<Map<String, Object>>) root.get("choices")).get(0);
            content = (String) ((Map<String, Object>) choice.get("message")).get("content");
        } else {
            content = body; // 已经是我们期望的 JSON
        }
        Map<String, Object> parsed = mapper.readValue(content, new TypeReference<>() {});
        List<Map<String, Object>> raw = (List<Map<String, Object>>) parsed.get("questions");

        List<GeneratedQuestion> result = new ArrayList<>();
        for (Map<String, Object> q : raw) {
            String type = String.valueOf(q.get("type")).toUpperCase();
            List<String> options = mapper.convertValue(q.get("options"), new TypeReference<>() {});
            String answerRaw = String.valueOf(q.get("answer"));
            String answer = normalizeAnswer(type, options, answerRaw);

            result.add(GeneratedQuestion.builder()
                    .type(type)
                    .stem(String.valueOf(q.get("stem")))
                    .options(options)
                    .answer(answer)
                    .explanation(String.valueOf(q.getOrDefault("explanation", "")))
                    .knowledgePoint(String.valueOf(q.getOrDefault("knowledgePoint", topic)))
                    .sourceUrl(String.valueOf(q.getOrDefault("sourceUrl", "")))
                    .difficulty(difficulty)
                    .build());
        }
        return result;
    }

    private String normalizeAnswer(String type, List<String> options, String answerRaw) {
        if ("JUDGE".equals(type)) {
            return Boolean.parseBoolean(answerRaw) ? "true" : "false";
        }
        // SINGLE：answerRaw 应为 options 中某项的完整文本，转换为字母
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).trim().equals(answerRaw.trim())
                    || options.get(i).contains(answerRaw.trim())) {
                return String.valueOf((char) ('A' + i));
            }
        }
        return answerRaw;
    }
}
