package com.xiuxian.model.dto;

import java.util.List;

/**
 * 背包视图：容量构成（境界基础 + 装备加成）、装备槽、宝物格子
 */
public record BagView(
        int capacity,          // 总格数 = 境界基础 + 装备加成
        int used,              // 已占用格数（同种宝物堆叠只占一格）
        int baseSlots,         // 当前境界的基础格数
        int bonusSlots,        // 已装备宝物带来的额外格数
        String realm,          // 当前境界
        String nextRealm,      // 下一境界（已满级为 null）
        int nextRealmSlots,    // 突破后的基础格数（已满级同当前）
        List<SlotView> slots,  // 装备槽
        List<BagItem> items    // 背包物品
) {

    /** 装备槽：一个槽位最多一件宝物 */
    public record SlotView(
            String code,
            String label,
            BagItem item       // 未装备时为 null
    ) {
    }

    /** 背包中的一格 */
    public record BagItem(
            Long id,             // 背包记录ID（操作装备/使用/丢弃时传它）
            Long treasureId,
            String code,
            String name,
            String type,         // EQUIP / PILL / MATERIAL
            String slot,         // WEAPON / ARMOR / STORAGE / ART，非装备为 null
            String slotLabel,    // 槽位中文名
            String rarity,       // 品阶
            int bagBonus,        // 装备后增加的背包格数
            int expBonus,        // 装备后的经验加成百分比
            int hpRestore,       // 使用后恢复的气血
            String description,
            int count,
            boolean equipped
    ) {
    }
}
