package com.xiuxian.model.dto;

/**
 * 用户修仙状态视图
 */
public record UserStateView(
        Long id,
        String userNo,        // 仙途编号（6 位，对外用户ID）
        SpiritRootView spiritRoot, // 灵根（null 表示尚未测灵根）
        String nickname,
        String realm,
        int layer,
        int exp,
        int hp,
        int maxHp,
        double progress,
        int answerCount,
        int correctCount,
        double accuracy,
        boolean canAnswer
) {
}
