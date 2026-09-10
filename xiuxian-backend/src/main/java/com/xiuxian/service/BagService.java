package com.xiuxian.service;

import com.xiuxian.mapper.TreasureMapper;
import com.xiuxian.mapper.UserMapper;
import com.xiuxian.mapper.UserTreasureMapper;
import com.xiuxian.model.dto.BagActionResult;
import com.xiuxian.model.dto.BagView;
import com.xiuxian.model.entity.Treasure;
import com.xiuxian.model.entity.User;
import com.xiuxian.model.entity.UserTreasure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 背包（储物）系统：
 * 容量 = 境界基础格数（凡人 10，每进阶 +5）+ 已装备宝物带来的格数加成。
 * 同种宝物堆叠，只占一格；装备槽每槽仅一件。
 */
@Service
public class BagService {

    private final UserTreasureMapper userTreasureMapper;
    private final TreasureMapper treasureMapper;
    private final UserMapper userMapper;

    public BagService(UserTreasureMapper userTreasureMapper, TreasureMapper treasureMapper, UserMapper userMapper) {
        this.userTreasureMapper = userTreasureMapper;
        this.treasureMapper = treasureMapper;
        this.userMapper = userMapper;
    }

    /** 查询背包全貌 */
    public BagView bag(Long userId) {
        User user = requireUser(userId);
        String realm = RealmPolicy.compute(user.getExp()).realm();

        List<Treasure> equipped = userTreasureMapper.findEquippedTreasures(userId);
        int bonus = equipped.stream().mapToInt(Treasure::getBagBonus).sum();

        Map<Long, Treasure> catalog = catalog();
        List<UserTreasure> rows = userTreasureMapper.findByUserId(userId);

        List<BagView.BagItem> items = new ArrayList<>();
        for (UserTreasure row : rows) {
            Treasure t = catalog.get(row.getTreasureId());
            if (t == null) continue;                       // 图鉴已删除的孤儿数据直接跳过
            items.add(toItem(row, t));
        }

        List<BagView.SlotView> slots = new ArrayList<>();
        for (BagPolicy.SlotDef def : BagPolicy.SLOTS) {
            BagView.BagItem worn = items.stream()
                    .filter(i -> i.equipped() && def.code().equals(i.slot()))
                    .findFirst()
                    .orElse(null);
            slots.add(new BagView.SlotView(def.code(), def.label(), worn));
        }

        int base = BagPolicy.baseSlots(realm);
        return new BagView(
                BagPolicy.capacity(realm, bonus),
                items.size(),
                base,
                bonus,
                realm,
                RealmPolicy.nextRealmName(realm),
                BagPolicy.nextRealmBaseSlots(realm),
                slots,
                items
        );
    }

    /** 装备：同槽位已有则自动替换 */
    @Transactional
    public BagActionResult equip(Long userId, Long itemId) {
        UserTreasure row = requireRow(userId, itemId);
        Treasure t = requireTreasure(row.getTreasureId());
        if (t.getSlot() == null || t.getSlot().isBlank()) {
            throw new IllegalArgumentException("「" + t.getName() + "」并非可装备之物");
        }

        String replaced = null;
        for (UserTreasure other : userTreasureMapper.findByUserId(userId)) {
            if (other.getEquipped() == 1 && !other.getId().equals(row.getId())) {
                Treasure ot = catalog().get(other.getTreasureId());
                if (ot != null && t.getSlot().equals(ot.getSlot())) {
                    other.setEquipped(0);
                    userTreasureMapper.updateById(other);
                    replaced = ot.getName();
                }
            }
        }

        row.setEquipped(1);
        userTreasureMapper.updateById(row);

        String msg = replaced == null
                ? "已装备「" + t.getName() + "」"
                : "卸下「" + replaced + "」，改佩「" + t.getName() + "」";
        if (t.getBagBonus() > 0) {
            msg += "，储物格数 +" + t.getBagBonus();
        }
        return new BagActionResult(msg, bag(userId));
    }

    /** 卸下 */
    @Transactional
    public BagActionResult unequip(Long userId, Long itemId) {
        UserTreasure row = requireRow(userId, itemId);
        Treasure t = requireTreasure(row.getTreasureId());
        if (row.getEquipped() == 0) {
            throw new IllegalArgumentException("「" + t.getName() + "」本就未装备");
        }
        row.setEquipped(0);
        userTreasureMapper.updateById(row);
        String msg = "已卸下「" + t.getName() + "」";
        if (t.getBagBonus() > 0) {
            msg += "，储物格数 -" + t.getBagBonus();
        }
        return new BagActionResult(msg, bag(userId));
    }

    /** 使用（目前仅丹药）：恢复气血，数量 -1，用尽则销毁该格 */
    @Transactional
    public BagActionResult use(Long userId, Long itemId) {
        UserTreasure row = requireRow(userId, itemId);
        Treasure t = requireTreasure(row.getTreasureId());
        if (!"PILL".equals(t.getType()) || t.getHpRestore() <= 0) {
            throw new IllegalArgumentException("「" + t.getName() + "」无法服用");
        }

        User user = requireUser(userId);
        int before = user.getHp();
        int after = Math.min(user.getMaxHp(), before + t.getHpRestore());
        user.setHp(after);
        userMapper.updateById(user);

        consume(row, 1);
        int gained = after - before;
        String msg = gained > 0
                ? "服下「" + t.getName() + "」，气血 +" + gained
                : "服下「" + t.getName() + "」，气血已然圆满";
        return new BagActionResult(msg, bag(userId));
    }

    /** 丢弃（默认 1 个，可指定数量）；整格丢弃时若已装备则一并卸下 */
    @Transactional
    public BagActionResult discard(Long userId, Long itemId, Integer count) {
        UserTreasure row = requireRow(userId, itemId);
        Treasure t = requireTreasure(row.getTreasureId());
        int n = count == null || count <= 0 ? 1 : Math.min(count, row.getCount());

        consume(row, n);
        return new BagActionResult("已丢弃「" + t.getName() + "」×" + n, bag(userId));
    }

    /**
     * 发放宝物（答题掉落 / 后台发放共用）。
     * @return 是否成功入包；背包已满且包内无同种宝物时返回 false（由调用方决定提示语）
     */
    @Transactional
    public boolean grant(Long userId, Treasure t, int count) {
        UserTreasure exist = userTreasureMapper.findOne(userId, t.getId());
        if (exist != null) {
            exist.setCount(exist.getCount() + Math.max(1, count));
            userTreasureMapper.updateById(exist);
            return true;
        }
        if (isFull(userId)) {
            return false;
        }
        UserTreasure row = new UserTreasure();
        row.setUserId(userId);
        row.setTreasureId(t.getId());
        row.setCount(Math.max(1, count));
        row.setEquipped(0);
        userTreasureMapper.insert(row);
        return true;
    }

    /** 背包是否已满 */
    public boolean isFull(Long userId) {
        User user = requireUser(userId);
        int used = userTreasureMapper.findByUserId(userId).size();
        return used >= BagPolicy.capacity(RealmPolicy.compute(user.getExp()).realm(), bonusSlots(userId));
    }

    /** 已装备宝物提供的额外格数 */
    public int bonusSlots(Long userId) {
        return userTreasureMapper.findEquippedTreasures(userId)
                .stream().mapToInt(Treasure::getBagBonus).sum();
    }

    /** 已装备宝物提供的经验加成百分比（供答题结算使用） */
    public int expBonusPercent(Long userId) {
        return userTreasureMapper.findEquippedTreasures(userId)
                .stream().mapToInt(Treasure::getExpBonus).sum();
    }

    // ---------- 内部工具 ----------

    private void consume(UserTreasure row, int n) {
        if (row.getCount() > n) {
            row.setCount(row.getCount() - n);
            userTreasureMapper.updateById(row);
        } else {
            userTreasureMapper.deleteById(row.getId());
        }
    }

    private BagView.BagItem toItem(UserTreasure row, Treasure t) {
        return new BagView.BagItem(
                row.getId(),
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getType(),
                t.getSlot(),
                BagPolicy.slotLabel(t.getSlot()),
                t.getRarity(),
                t.getBagBonus(),
                t.getExpBonus(),
                t.getHpRestore(),
                t.getDescription(),
                row.getCount(),
                row.getEquipped() == 1
        );
    }

    /** 宝物图鉴（图鉴条目很少，一次全量取回做映射，避免 N+1） */
    private Map<Long, Treasure> catalog() {
        return treasureMapper.selectList(null).stream()
                .collect(Collectors.toMap(Treasure::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + userId);
        return user;
    }

    private UserTreasure requireRow(Long userId, Long itemId) {
        UserTreasure row = userTreasureMapper.selectById(itemId);
        if (row == null || !row.getUserId().equals(userId)) {
            throw new IllegalArgumentException("背包中没有这件宝物");
        }
        return row;
    }

    private Treasure requireTreasure(Long treasureId) {
        Treasure t = treasureMapper.selectById(treasureId);
        if (t == null) throw new IllegalArgumentException("宝物图鉴缺失: " + treasureId);
        return t;
    }
}
