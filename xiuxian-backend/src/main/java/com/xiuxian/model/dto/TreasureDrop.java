package com.xiuxian.model.dto;

/**
 * 答题掉落的宝物提示（未掉落时为 null）
 */
public record TreasureDrop(
        String name,
        String rarity,
        String description
) {
}
