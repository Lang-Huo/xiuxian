package com.xiuxian.service;

import java.util.List;
import java.util.Map;

/**
 * 背包（储物）策略：容量 = 境界基础格数 + 已装备宝物的格数加成。
 *
 * 与 {@link RealmPolicy} 解耦，只按境界名查表；未知境界按凡人处理，避免脏数据导致崩溃。
 */
public final class BagPolicy {

    /** 凡人（初始境界）的基础格数 */
    public static final int BASE_SLOTS_FANREN = 10;

    /** 境界 -> 基础格数（每提升一个大境界 +5） */
    private static final Map<String, Integer> BASE_SLOTS = Map.of(
            "凡人", 10,
            "练气", 15,
            "筑基", 20,
            "金丹", 25,
            "元婴", 30,
            "化神", 35,
            "炼虚", 40,
            "合体", 45,
            "大乘", 50
    );

    /** 装备槽定义：每个槽位同时只能装备一件宝物 */
    public static final List<SlotDef> SLOTS = List.of(
            new SlotDef("WEAPON", "本命法器"),
            new SlotDef("ARMOR", "护身宝衣"),
            new SlotDef("STORAGE", "储物法宝"),
            new SlotDef("ART", "功法秘籍")
    );

    private BagPolicy() {
    }

    /** 境界对应的基础格数 */
    public static int baseSlots(String realm) {
        if (realm == null) return BASE_SLOTS_FANREN;
        return BASE_SLOTS.getOrDefault(realm.trim(), BASE_SLOTS_FANREN);
    }

    /** 总容量 = 基础格数 + 装备加成（加成不会为负） */
    public static int capacity(String realm, int bonusSlots) {
        return baseSlots(realm) + Math.max(0, bonusSlots);
    }

    /** 突破到下一境界后能拿到的基础格数；已是最高境界则返回当前值 */
    public static int nextRealmBaseSlots(String realm) {
        String next = RealmPolicy.nextRealmName(realm);
        return next == null ? baseSlots(realm) : baseSlots(next);
    }

    /** 装备槽中文名；未知编码原样返回 */
    public static String slotLabel(String slot) {
        if (slot == null) return "";
        return SLOTS.stream()
                .filter(s -> s.code().equals(slot))
                .map(SlotDef::label)
                .findFirst()
                .orElse(slot);
    }

    public record SlotDef(String code, String label) {
    }
}
