-- ============================================================
-- 仙途 · 已有 MySQL 库「新增背包系统（宝物图鉴 + 用户背包）」脚本
-- 文件：db/add-bag-tables-to-existing-db.sql
--
-- 背景：新增背包系统（储物格），需要两张新表：
--   treasures       宝物图鉴（静态配置：储物法宝 / 丹药 / 法器 / 宝衣 / 功法）
--   user_treasures  用户背包（按 treasure_id 堆叠，同种只占一格）
--
-- 说明：
--   1. 两张表都是新增表，用 CREATE TABLE IF NOT EXISTS，对新库无副作用
--   2. 初始宝物数据按 code 判存（FROM DUAL + NOT EXISTS），重复执行不会新增
--   3. 若你还想调整掉落权重或格数加成，直接 UPDATE treasures 即可，无需改代码
--
-- 执行方式：
--   mysql -u root -p xiuxian < add-bag-tables-to-existing-db.sql
--   （如客户端不在目标库，请先执行： USE xiuxian; ）
--
-- 注：本文件内容与 schema.sql 中的 treasures / user_treasures 段落保持一致，
--     后续如新增宝物，请两边同步（或直接从 schema.sql 复制该段落重跑）。
-- ============================================================

USE xiuxian;

-- ---------- 宝物图鉴 ----------
CREATE TABLE IF NOT EXISTS treasures (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '宝物ID（主键）',
    code        VARCHAR(32)  NOT NULL DEFAULT '' UNIQUE   COMMENT '宝物编码（唯一，初始数据与掉落去重用）',
    name        VARCHAR(64)  NOT NULL DEFAULT ''          COMMENT '宝物名称',
    type        VARCHAR(16)  NOT NULL DEFAULT 'EQUIP'     COMMENT '宝物类型（EQUIP 装备 / PILL 丹药 / MATERIAL 材料）',
    slot        VARCHAR(16)               COMMENT '装备槽（WEAPON 法器 / ARMOR 宝衣 / STORAGE 储物 / ART 功法；非装备类为空）',
    rarity      VARCHAR(16)  NOT NULL DEFAULT '凡品'      COMMENT '品阶（凡品/灵品/宝品/仙品/神品）',
    bag_bonus   INT          NOT NULL DEFAULT 0           COMMENT '装备后额外增加的背包格数',
    exp_bonus   INT          NOT NULL DEFAULT 0           COMMENT '装备后的经验加成百分比（如 10 表示 +10%）',
    hp_restore  INT          NOT NULL DEFAULT 0           COMMENT '使用后恢复的气血值（丹药类生效）',
    drop_weight INT          NOT NULL DEFAULT 0           COMMENT '掉落权重（0 表示不参与掉落，仅后台发放）',
    description VARCHAR(255)                              COMMENT '宝物描述',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间'
) COMMENT '宝物图鉴：储物法宝/丹药/法器/宝衣/功法的静态配置';

-- ---------- 用户背包 ----------
CREATE TABLE IF NOT EXISTS user_treasures (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY   COMMENT '背包记录ID（主键）',
    user_id     BIGINT       NOT NULL DEFAULT 0           COMMENT '所属用户ID',
    treasure_id BIGINT       NOT NULL DEFAULT 0           COMMENT '宝物ID（关联 treasures.id）',
    count       INT          NOT NULL DEFAULT 1           COMMENT '持有数量（同种宝物堆叠，仍只占一格）',
    equipped    INT          NOT NULL DEFAULT 0           COMMENT '是否已装备（0 未装备 / 1 已装备）',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '获得时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '更新时间',
    UNIQUE KEY uk_user_treasure (user_id, treasure_id)
) COMMENT '用户背包：持有与已装备的宝物';

-- ---------- 初始宝物数据（幂等） ----------
INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'BAG_CLOTH', '粗布囊', 'EQUIP', 'STORAGE', '凡品', 5, 0, 0, 300, '村头货郎缝的粗布口袋，虽不体面，却能多塞几样杂物。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'BAG_CLOTH');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'BAG_QIANKUN', '乾坤袋', 'EQUIP', 'STORAGE', '灵品', 12, 0, 0, 120, '内有乾坤，可纳百物，寻常修士梦寐以求的储物法宝。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'BAG_QIANKUN');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'BAG_XUMI', '须弥芥子', 'EQUIP', 'STORAGE', '宝品', 25, 0, 0, 40, '芥子纳须弥，方寸之间自有天地。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'BAG_XUMI');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'BAG_SHANHE', '山河社稷图', 'EQUIP', 'STORAGE', '仙品', 50, 0, 0, 8, '图中自有山河社稷，收藏之丰，几无止境。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'BAG_SHANHE');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'PILL_HUIQI', '回气丹', 'PILL', NULL, '凡品', 0, 0, 30, 300, '最寻常的疗伤丹药，服下可恢复 30 点气血。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'PILL_HUIQI');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'PILL_NINGSHEN', '凝神丹', 'PILL', NULL, '灵品', 0, 0, 60, 120, '凝神静气，恢复 60 点气血，并令心神清明。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'PILL_NINGSHEN');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'PILL_JIUZHUAN', '九转还魂丹', 'PILL', NULL, '宝品', 0, 0, 100, 30, '九转功成，可还魂续命，恢复满身气血。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'PILL_JIUZHUAN');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'WEAPON_QINGZHU', '青竹剑', 'EQUIP', 'WEAPON', '凡品', 0, 5, 0, 200, '削青竹为剑，轻灵趁手，修行所得道行 +5%。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'WEAPON_QINGZHU');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'WEAPON_XUANTIE', '玄铁重剑', 'EQUIP', 'WEAPON', '灵品', 0, 10, 0, 60, '重剑无锋，大巧不工，修行所得道行 +10%。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'WEAPON_XUANTIE');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'ARMOR_YUNWEN', '云纹道袍', 'EQUIP', 'ARMOR', '凡品', 0, 3, 0, 200, '云纹暗织，穿着安稳，修行所得道行 +3%。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'ARMOR_YUNWEN');

INSERT INTO treasures (code, name, type, slot, rarity, bag_bonus, exp_bonus, hp_restore, drop_weight, description)
SELECT 'ART_WUXING', '五行遁法', 'EQUIP', 'ART', '灵品', 0, 8, 0, 60, '通达五行变化，悟道更快，修行所得道行 +8%。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM treasures WHERE code = 'ART_WUXING');

-- ============================================================
-- 【可选】调参示例（改完立即生效，无需重启，无需改代码）
--   调掉落概率（整体概率在 GameService.DROP_RATE，当前 12%）；
--   单件宝物的相对概率由 drop_weight 决定：
--     UPDATE treasures SET drop_weight = 500 WHERE code = 'BAG_CLOTH';
--   调某个储物法宝的加格数：
--     UPDATE treasures SET bag_bonus = 20 WHERE code = 'BAG_QIANKUN';
--   临时下架某件宝物（不再掉落，已获得的不受影响）：
--     UPDATE treasures SET drop_weight = 0 WHERE code = 'BAG_SHANHE';
--
-- 【校验】
--   SHOW TABLES LIKE '%treasures';
--   SELECT code, name, slot, rarity, bag_bonus, drop_weight FROM treasures ORDER BY id;
--   SELECT u.id, u.nickname, COUNT(ut.id) AS 占用格数 FROM users u
--     LEFT JOIN user_treasures ut ON ut.user_id = u.id GROUP BY u.id, u.nickname;
-- ============================================================
