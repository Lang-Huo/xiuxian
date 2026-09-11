package com.xiuxian.model.dto;

/**
 * 测灵根结果：判定所得灵根 + 答题统计
 */
public record SpiritRootResult(
        SpiritRootView root,    // 判定得到的灵根
        int correct,            // 答对题数
        int total,              // 测灵根总题数
        double accuracy         // 正确率 0~1
) {
}