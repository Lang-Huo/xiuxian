package com.xiuxian.service;

import java.util.List;

/**
 * 境界（等级）策略：根据累计经验计算当前境界、层数、进阶进度。
 * 阈值与计划书一致，可在此统一调参。
 */
public final class RealmPolicy {

    /** 有序境界表：[境界名, 进入该境界所需累计经验] */
    private static final List<Realm> REALMS = List.of(
            new Realm("凡人", 0),
            new Realm("练气", 100),
            new Realm("筑基", 500),
            new Realm("金丹", 1500),
            new Realm("元婴", 4000),
            new Realm("化神", 9000),
            new Realm("炼虚", 20000),
            new Realm("合体", 45000),
            new Realm("大乘", 100000)
    );

    private RealmPolicy() {}

    public static RealmInfo compute(int exp) {
        int e = Math.max(0, exp);
        Realm current = REALMS.get(0);
        int idx = 0;
        for (int i = 0; i < REALMS.size(); i++) {
            if (e >= REALMS.get(i).threshold) {
                current = REALMS.get(i);
                idx = i;
            }
        }
        boolean isMax = idx == REALMS.size() - 1;
        int nextThreshold = isMax ? current.threshold : REALMS.get(idx + 1).threshold;
        int span = nextThreshold - current.threshold;
        double progress = span <= 0 ? 1.0 : (double) (e - current.threshold) / span;
        int layer = isMax ? 9 : Math.min(9, 1 + (int) (progress * 9));
        return new RealmInfo(current.name, layer, current.threshold, nextThreshold, progress, isMax);
    }

    /** 下一境界名；已是最高境界则返回 null */
    public static String nextRealmName(String realm) {
        for (int i = 0; i < REALMS.size(); i++) {
            if (REALMS.get(i).name.equals(realm)) {
                return i == REALMS.size() - 1 ? null : REALMS.get(i + 1).name;
            }
        }
        return null;
    }

    public record Realm(String name, int threshold) {}
    public record RealmInfo(
            String realm,
            int layer,
            int currentThreshold,
            int nextThreshold,
            double progress,   // 0~1，当前境界内进度
            boolean isMax
    ) {}
}
