package com.xiuxian.model.dto;

/**
 * 灵根：属性全部来源于 spirit_roots 表（id/code/name/multiplier/rarity/description），
 *      不再以中文名作业务关联。
 */
public record SpiritRootView(
        Long id,            // 灵根ID（关联 spirit_roots.id）
        String code,        // 灵根编码（FIVE/DOUBLE/SINGLE/VARIANT/SKY/CHAOS）
        String name,        // 灵根名
        double multiplier,  // 道行倍率
        String rarity,      // 稀有度
        String description  // 描述
) {
}
