-- ============================================================
-- 仙途 · 已有 MySQL 库「新增 user_no（仙途编号）列」脚本
-- 文件：db/add-user-no-to-existing-db.sql
--
-- 背景：users 表新增对外用户ID —— 仙途编号，规则为 6 位数字、首位非 0、全站唯一，
--       注册时自动分配。CREATE TABLE IF NOT EXISTS 对已存在的表不会做任何变更，
--       因此老库必须手工执行本脚本补列。
--
-- 执行顺序不可颠倒：
--   1) 先 ADD COLUMN（允许空串，暂不建唯一索引）
--   2) 回填历史数据（否则全为空串，建唯一索引会报 1062 重复）
--   3) 再建唯一索引
--
-- 本脚本幂等：列或索引已存在时会自动跳过，可重复执行。
-- 建议低峰执行并先备份数据。
--
-- 执行方式：
--   mysql -u root -p xiuxian < add-user-no-to-existing-db.sql
--   （如客户端不在目标库，请先执行： USE xiuxian; ）
-- ============================================================

USE xiuxian;

-- ---------- 1) 补列（若不存在） ----------
DROP PROCEDURE IF EXISTS xiuxian_add_user_no_column;
DELIMITER $$
CREATE PROCEDURE xiuxian_add_user_no_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'user_no'
    ) THEN
        ALTER TABLE users
            ADD COLUMN user_no VARCHAR(6) NOT NULL DEFAULT '' COMMENT '仙途编号（6位数字，首位非0，全站唯一，对外展示的用户ID）'
            AFTER id;
    ELSE
        -- 已存在则顺带补齐注释（MySQL 的 MODIFY 必须完整重述列定义）
        ALTER TABLE users
            MODIFY COLUMN user_no VARCHAR(6) NOT NULL DEFAULT '' COMMENT '仙途编号（6位数字，首位非0，全站唯一，对外展示的用户ID）';
    END IF;
END$$
DELIMITER ;
CALL xiuxian_add_user_no_column();
DROP PROCEDURE IF EXISTS xiuxian_add_user_no_column;

-- ---------- 2) 回填历史账号 ----------
-- 规则：'1' + id 左补零到 5 位 => 固定 6 位、首位为 1、天然不重复（前提：id < 100000）
UPDATE users
SET user_no = CONCAT('1', LPAD(id, 5, '0'))
WHERE user_no IS NULL OR user_no = '';

-- 若 id 已经超过 99999，上面的回填会产生重复值，请先跑下面这条检查：
--   SELECT user_no, COUNT(*) c FROM users GROUP BY user_no HAVING c > 1;
-- 有重复时改用随机号回填（每执行一次再看是否仍有重复）：
--   UPDATE users SET user_no = LPAD(FLOOR(100000 + RAND() * 900000), 6, '0')
--   WHERE user_no IN (SELECT user_no FROM (SELECT user_no FROM users GROUP BY user_no HAVING COUNT(*) > 1) t);

-- ---------- 3) 建唯一索引（若不存在） ----------
DROP PROCEDURE IF EXISTS xiuxian_add_user_no_index;
DELIMITER $$
CREATE PROCEDURE xiuxian_add_user_no_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND INDEX_NAME = 'uk_users_user_no'
    ) THEN
        ALTER TABLE users ADD UNIQUE KEY uk_users_user_no (user_no);
    END IF;
END$$
DELIMITER ;
CALL xiuxian_add_user_no_index();
DROP PROCEDURE IF EXISTS xiuxian_add_user_no_index;

-- ============================================================
-- 【校验】执行完后检查列与索引是否就位、是否存在重复或非法编号
--   SHOW FULL COLUMNS FROM users LIKE 'user_no';
--   SHOW INDEX FROM users WHERE Key_name = 'uk_users_user_no';
--   SELECT COUNT(*) AS 空编号 FROM users WHERE user_no = '';
--   SELECT COUNT(*) AS 非法编号 FROM users WHERE user_no NOT REGEXP '^[1-9][0-9]{5}$';
--   SELECT user_no, COUNT(*) c FROM users GROUP BY user_no HAVING c > 1;
-- ============================================================
