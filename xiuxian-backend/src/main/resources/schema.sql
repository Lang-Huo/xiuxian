-- 修仙学习应用 MVP 建表脚本（MySQL 与 H2 MySQL 兼容模式通用）
-- 由 Spring Boot 启动时通过 spring.sql.init 自动执行（含 IF NOT EXISTS，可重复运行）
-- 每个字段均附中文注释（MySQL / H2 列级 COMMENT）

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID（主键，内部自增）',
    user_no       VARCHAR(6)   NOT NULL DEFAULT '' UNIQUE   COMMENT '仙途编号（6位数字，首位非0，全站唯一，对外展示的用户ID）',
    username      VARCHAR(11)  NOT NULL DEFAULT '' UNIQUE   COMMENT '登录手机号（11位，唯一登录凭证）',
    nickname      VARCHAR(64)  NOT NULL DEFAULT '道友'       COMMENT '道号/昵称（展示用，可重复）',
    avatar        VARCHAR(32)  NOT NULL DEFAULT ''           COMMENT '头像编码（内置头像库编码，见 AvatarPolicy；空则按默认处理）',
    password      VARCHAR(128) NOT NULL DEFAULT ''            COMMENT '登录密码（BCrypt 哈希存储）',
    realm         VARCHAR(32)  NOT NULL DEFAULT '凡人'        COMMENT '修仙境界（凡人/练气/筑基/金丹/元婴/化神…）',
    layer         INT          NOT NULL DEFAULT 1             COMMENT '当前境界层数',
    exp           INT          NOT NULL DEFAULT 0             COMMENT '累计道行/经验值',
    hp            INT          NOT NULL DEFAULT 100           COMMENT '当前气血值（耗尽则不可答题）',
    max_hp        INT          NOT NULL DEFAULT 100           COMMENT '气血上限',
    correct_count INT          NOT NULL DEFAULT 0             COMMENT '累计答对题数',
    answer_count  INT          NOT NULL DEFAULT 0             COMMENT '累计作答题数',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP      COMMENT '更新时间'
);

CREATE TABLE IF NOT EXISTS topics (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY          COMMENT '主题ID（主键）',
    user_id     BIGINT       NOT NULL DEFAULT 1                   COMMENT '所属用户ID',
    keyword     VARCHAR(512) NOT NULL                            COMMENT '修炼主题/想学的知识',
    difficulty  VARCHAR(32)  NOT NULL DEFAULT '入门'              COMMENT '难度（入门/进阶/精通）',
    count       INT          NOT NULL DEFAULT 5                   COMMENT '生成题目数量',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间'
);

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
);

CREATE TABLE IF NOT EXISTS user_treasures (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY   COMMENT '背包记录ID（主键）',
    user_id     BIGINT       NOT NULL DEFAULT 0           COMMENT '所属用户ID',
    treasure_id BIGINT       NOT NULL DEFAULT 0           COMMENT '宝物ID（关联 treasures.id）',
    count       INT          NOT NULL DEFAULT 1           COMMENT '持有数量（同种宝物堆叠，仍只占一格）',
    equipped    INT          NOT NULL DEFAULT 0           COMMENT '是否已装备（0 未装备 / 1 已装备）',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '获得时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP    COMMENT '更新时间',
    UNIQUE KEY uk_user_treasure (user_id, treasure_id)
);

CREATE TABLE IF NOT EXISTS questions (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY      COMMENT '题目ID（主键）',
    topic_id        BIGINT       NOT NULL                        COMMENT '所属主题ID',
    type            VARCHAR(16)  NOT NULL DEFAULT 'SINGLE'       COMMENT '题型（SINGLE 单选 / JUDGE 判断）',
    stem            TEXT         NOT NULL                        COMMENT '题干',
    options         TEXT                                          COMMENT '选项（JSON 数组文本，如 ["A","B","C","D"]）',
    answer          VARCHAR(16)  NOT NULL                        COMMENT '正确答案（单选为字母；判断为 true/false）',
    explanation     TEXT                                          COMMENT '答案解析',
    knowledge_point VARCHAR(256)                                  COMMENT '知识点',
    source_url      VARCHAR(1024)                                 COMMENT '知识来源链接',
    difficulty      VARCHAR(16)  DEFAULT '入门'                   COMMENT '难度（入门/进阶/精通）'
);

-- 初始宝物图鉴（幂等：按 code 判存，重复执行不会新增）
-- 掉落权重越大越常见；答题结算时按权重随机抽取
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
