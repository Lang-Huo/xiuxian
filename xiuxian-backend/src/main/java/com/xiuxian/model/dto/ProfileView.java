package com.xiuxian.model.dto;

import java.util.List;

/**
 * 个人详情视图：聚合用户基础信息、修仙进度、答题战绩与修炼统计
 */
public record ProfileView(
        Long id,
        String userNo,            // 仙途编号（6 位，对外用户ID）
        String username,          // 登录手机号（已脱敏，如 138****5678）
        String nickname,          // 道号/昵称
        AvatarView avatar,        // 头像（内置头像库中的一款，绝不为 null）
        SpiritRootView spiritRoot, // 灵根（null 表示尚未测灵根）
        String title,             // 称号（由境界派生）
        String realm,             // 当前境界
        int layer,                // 当前境界层数
        int exp,                  // 累计道行
        int expToNext,            // 距下一境界还需经验（已满级为 0）
        String nextRealm,         // 下一境界名（已满级为 null）
        double progress,          // 当前境界内进度 0~1
        boolean maxRealm,         // 是否已至最高境界
        int hp,
        int maxHp,
        int answerCount,
        int correctCount,
        double accuracy,
        int topicCount,           // 修炼过的主题数
        int questionCount,        // 累计生成的题目数
        String joinedAt,          // 入道日期 yyyy-MM-dd
        int practiceDays,         // 修行天数（含当日）
        List<TopicBrief> recentTopics
) {

    /** 修炼记录摘要 */
    public record TopicBrief(
            Long id,
            String keyword,
            String difficulty,
            int count,
            String createdAt      // yyyy-MM-dd HH:mm
    ) {
    }
}
