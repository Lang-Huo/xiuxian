package com.xiuxian.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 出题引擎产出的题目（尚未落库）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestion {
    /** JUDGE / SINGLE */
    private String type;
    private String stem;
    private List<String> options;
    private String answer;
    private String explanation;
    private String knowledgePoint;
    private String sourceUrl;
    private String difficulty;
}
