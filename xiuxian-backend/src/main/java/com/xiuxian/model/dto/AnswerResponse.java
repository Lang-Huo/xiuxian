package com.xiuxian.model.dto;

/**
 * 提交答案后的结算结果（含解析与游戏状态变化）
 */
public record AnswerResponse(
        boolean correct,
        String correctAnswer,
        String explanation,
        String knowledgePoint,
        String sourceUrl,
        int expGain,
        int hpDelta,
        int hp,
        int maxHp,
        int exp,
        String realm,
        int layer,
        double progress,     // 当前境界内进度 0~1
        boolean leveledUp,   // 大境界是否突破
        boolean canAnswer,   // 血量>0 才可继续
        TreasureDrop drop,   // 本次掉落的宝物（未掉落为 null）
        boolean bagFull      // 抽中了宝物但背包已满，只能眼睁睁看它溜走
) {
}
