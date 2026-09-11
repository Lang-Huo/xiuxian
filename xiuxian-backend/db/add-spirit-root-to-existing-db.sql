-- ============================================================
-- 仙途 · 已有 MySQL 库「灵根外置为配置表」脚本
-- 文件：db/add-spirit-root-to-existing-db.sql
--
-- 背景：灵根由 users.spirit_root(中文名) 改为 spirit_roots 配置表 + users.spirit_root_code 关联，
--       后续调整灵根属性/新增灵根只需 UPDATE/INSERT 灵根表，代码逻辑零改动。
--
-- 步骤（顺序不可颠倒）：
--   1) 建 spirit_roots 表
--   2) 灌 6 款灵根初始数据（按 code 判存，幂等）
--   3) users 加 spirit_root_code 列（允许 NULL）
--   4) 回填：把老数据 users.spirit_root(中文名) → spirit_root_code
--   5) 校验
--
-- 说明：users.spirit_root 旧列保留（兼容排查），代码已切换为 spirit_root_code；确认无问题后可手动：
--   ALTER TABLE users DROP COLUMN spirit_root;
--
-- 执行方式：
--   mysql -u root -p xiuxian < add-spirit-root-to-existing-db.sql
-- ============================================================

USE xiuxian;

-- ---------- 1) 建灵根配置表 ----------
CREATE TABLE IF NOT EXISTS spirit_roots (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY           COMMENT '灵根ID（主键）',
    code        VARCHAR(32)  NOT NULL DEFAULT '' UNIQUE           COMMENT '灵根编码（FIVE/DOUBLE/SINGLE/VARIANT/SKY/CHAOS，程序用）',
    name        VARCHAR(16)  NOT NULL DEFAULT ''                  COMMENT '灵根名（五灵根/双灵根/单灵根/变异灵根/天灵根/混沌灵根）',
    multiplier  DECIMAL(4,2) NOT NULL DEFAULT 1.00               COMMENT '道行倍率（答题经验乘此值，未测为 1.0）',
    rarity      VARCHAR(16)  NOT NULL DEFAULT '常见'              COMMENT '稀有度（常见/较稀有/稀有/极稀有）',
    description VARCHAR(255)                                     COMMENT '灵根描述',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间'
) COMMENT '灵根配置表：属性集中在此，前端按 code 关联';

-- ---------- 2) 灌 6 款灵根（幂等） ----------
INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'FIVE',   '五灵根',   0.80, '常见',   '五行驳杂，修行缓慢，胜在根基扎实、后劲绵长。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'FIVE');

INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'DOUBLE', '双灵根',   1.00, '常见',   '双系并行，中规中矩，稳扎稳打亦是道。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'DOUBLE');

INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'SINGLE', '单灵根',   1.15, '较稀有', '一系精纯，杂念尽去，悟道快人一步。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'SINGLE');

INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'VARIANT','变异灵根', 1.30, '稀有',   '异变之体，不循常理，修行事半功倍。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'VARIANT');

INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'SKY',    '天灵根',   1.50, '稀有',   '天赐之资，心随意动，道途坦荡无阻。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'SKY');

INSERT INTO spirit_roots (code, name, multiplier, rarity, description)
SELECT 'CHAOS',  '混沌灵根', 2.00, '极稀有', '混沌初开，万法归一，修行一日千里。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM spirit_roots WHERE code = 'CHAOS');

-- ---------- 3) users 加 spirit_root_code 列 ----------
DROP PROCEDURE IF EXISTS xiuxian_add_spirit_root_code;
DELIMITER $$
CREATE PROCEDURE xiuxian_add_spirit_root_code()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'spirit_root_code'
    ) THEN
        ALTER TABLE users
            ADD COLUMN spirit_root_code VARCHAR(32) NULL COMMENT '灵根编码（关联 spirit_roots.code；NULL 表示尚未测灵根）'
            AFTER spirit_root;
    END IF;
END$$
DELIMITER ;
CALL xiuxian_add_spirit_root_code();
DROP PROCEDURE IF EXISTS xiuxian_add_spirit_root_code;

-- ---------- 4) 回填：旧 spirit_root(中文名) → spirit_root_code ----------
UPDATE users u
JOIN spirit_roots sr ON sr.name = u.spirit_root
SET u.spirit_root_code = sr.code
WHERE u.spirit_root IS NOT NULL AND u.spirit_root <> ''
  AND u.spirit_root_code IS NULL;

-- ---------- 5) 校验 ----------
-- SELECT code, name, multiplier, rarity FROM spirit_roots ORDER BY id;
-- SELECT id, spirit_root, spirit_root_code FROM users;
-- SELECT COUNT(*) AS 未测灵根 FROM users WHERE spirit_root_code IS NULL;
-- SELECT sr.name, COUNT(*) FROM users u
--   JOIN spirit_roots sr ON sr.code = u.spirit_root_code
--   GROUP BY sr.name ORDER BY sr.id;
-- ============================================================
