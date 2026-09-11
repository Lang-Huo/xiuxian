package com.xiuxian.service;

import com.xiuxian.model.dto.SpiritRootView;
import com.xiuxian.model.entity.SpiritRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 灵根策略：所有属性来源于 spirit_roots 表（启动时由 SpiritRootService 灌入缓存），
 * 不再用中文名（"天灵根"）做业务关联 —— 一律按灵根 code 走。
 *
 * 测灵根固定 5 题，按正确率判定；调阈值或调整判定规则只改这里。
 */
public final class SpiritRootPolicy {

    /** 测灵根题目固定 5 道 */
    public static final int TEST_COUNT = 5;

    /** 测灵根主题关键字前缀（与 TopicMapper 两条 SQL 的过滤前缀保持一致） */
    public static final String TEST_TOPIC_PREFIX = "【测灵根】";

    /** 未测灵根时的兜底倍率（不让玩家吃亏也不占便宜） */
    public static final double DEFAULT_MULTIPLIER = 1.0;

    /** 满分时的混沌灵根概率 */
    private static final double CHAOS_RATE = 0.15;

    /** 灵根编码（与 spirit_roots.code 对应，方便业务按品级写死） */
    public static final String CODE_FIVE    = "FIVE";
    public static final String CODE_DOUBLE  = "DOUBLE";
    public static final String CODE_SINGLE  = "SINGLE";
    public static final String CODE_VARIANT = "VARIANT";
    public static final String CODE_SKY     = "SKY";
    public static final String CODE_CHAOS   = "CHAOS";

    /** 缓存：id -> SpiritRootView（启动时由 SpiritRootService 灌入） */
    private static final Map<Long, SpiritRootView> BY_ID = new HashMap<>();
    /** 缓存：code -> SpiritRootView */
    private static final Map<String, SpiritRootView> BY_CODE = new HashMap<>();
    /** 兜底（双灵根，倍率 1.0）id —— 找不到/未测时回退 */
    private static volatile long defaultId = -1L;

    private SpiritRootPolicy() {
    }

    /** 启动时一次性灌入缓存（由 SpiritRootService @PostConstruct 调用） */
    public static void loadFromDatabase(List<SpiritRoot> rows) {
        Map<Long, SpiritRootView> byId = new HashMap<>();
        Map<String, SpiritRootView> byCode = new HashMap<>();
        for (SpiritRoot r : rows) {
            SpiritRootView v = toView(r);
            byId.put(r.getId(), v);
            byCode.put(r.getCode(), v);
        }
        synchronized (SpiritRootPolicy.class) {
            BY_ID.clear();
            BY_ID.putAll(byId);
            BY_CODE.clear();
            BY_CODE.putAll(byCode);
            // 兜底优先 DOUBLE（倍率 1.0），找不到则取第一条
            SpiritRootView fallback = byCode.getOrDefault(CODE_DOUBLE, byId.values().stream().findFirst().orElse(null));
            defaultId = fallback == null ? -1L : fallback.id();
        }
    }

    /** 当前缓存里的所有灵根（前端展示用） */
    public static List<SpiritRootView> all() {
        return List.copyOf(BY_ID.values());
    }

    /** 按 id 取灵根；id 为空或不在表里时返回 null（区别于"未测灵根"语义） */
    public static SpiritRootView byId(Long id) {
        if (id == null) return null;
        return BY_ID.get(id);
    }

    /** 按编码取灵根（如 "SKY"），找不到返回 null */
    public static SpiritRootView byCode(String code) {
        if (code == null || code.isBlank()) return null;
        return BY_CODE.get(code.trim().toUpperCase());
    }

    /** 道行倍率：未测为 1.0；code 找不到/未加载也返回 1.0，不影响结算 */
    public static double multiplier(String code) {
        SpiritRootView v = byCode(code);
        return v == null ? DEFAULT_MULTIPLIER : v.multiplier();
    }

    /** 是否已测灵根 */
    public static boolean tested(String code) {
        return byCode(code) != null;
    }

    /** 默认灵根（双灵根）—— 仅启动后缓存里有数据才有意义，否则返回 null */
    public static SpiritRootView defaultRoot() {
        return defaultId > 0 ? BY_ID.get(defaultId) : null;
    }

    /**
     * 按测灵根的正确率判定灵根（5 题）：
     * ≤20% 五灵根 · 40% 双灵根 · 60% 单灵根 · 80% 变异灵根 · 100% 天灵根（15% 概率出混沌灵根）
     */
    public static SpiritRootView judge(int correct, int total) {
        if (total <= 0) {
            return byCode(CODE_FIVE);
        }
        double rate = (double) Math.max(0, correct) / total;
        if (rate >= 1.0) {
            return ThreadLocalRandom.current().nextDouble() < CHAOS_RATE
                    ? byCode(CODE_CHAOS)
                    : byCode(CODE_SKY);
        }
        if (rate >= 0.8) return byCode(CODE_VARIANT);
        if (rate >= 0.6) return byCode(CODE_SINGLE);
        if (rate >= 0.4) return byCode(CODE_DOUBLE);
        return byCode(CODE_FIVE);
    }

    private static SpiritRootView toView(SpiritRoot r) {
        return new SpiritRootView(
                r.getId(),
                r.getCode(),
                r.getName(),
                r.getMultiplier() == null ? 1.0 : r.getMultiplier().doubleValue(),
                r.getRarity(),
                r.getDescription()
        );
    }
}
