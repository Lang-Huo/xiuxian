-- ============================================================
-- 仙途 · 已有 MySQL 库「新增头像字段」脚本
-- 文件：db/add-avatar-to-existing-db.sql
--
-- 背景：新增头像功能，users 表需补 avatar 列存头像编码。
--       默认头像 = 昵称第一个字（AUTO），也可从 12 款内置头像中任选。
--       编码取值与后端 AvatarPolicy 保持一致：
--       XIAN / DAO / JIAN / DAN / FU / HE / SONG / YUN / YUE / LIAN / ZHU / JIU
--
-- 说明：
--   1. MySQL 没有 ADD COLUMN IF NOT EXISTS，用临时存储过程 + INFORMATION_SCHEMA 判存实现幂等
--   2. **默认头像 = 昵称第一个字**：avatar 为空或为 AUTO 时即如此，历史账号无需回填
--   3. 头像编码非法、为空或不在图鉴内（如手改过的脏数据），后端一律回退到昵称首字，不会报错
--
-- 执行方式：
--   mysql -u root -p xiuxian < add-avatar-to-existing-db.sql
--   （如客户端不在目标库，请先执行： USE xiuxian; ）
-- ============================================================

USE xiuxian;

-- ---------- 1) 补列（若不存在） ----------
DROP PROCEDURE IF EXISTS xiuxian_add_avatar_column;
DELIMITER $$
CREATE PROCEDURE xiuxian_add_avatar_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'avatar'
    ) THEN
        ALTER TABLE users
            ADD COLUMN avatar VARCHAR(32) NOT NULL DEFAULT '' COMMENT '头像编码（内置头像库编码，见 AvatarPolicy；空则按默认处理）'
            AFTER nickname;
    ELSE
        ALTER TABLE users
            MODIFY COLUMN avatar VARCHAR(32) NOT NULL DEFAULT '' COMMENT '头像编码（内置头像库编码，见 AvatarPolicy；空则按默认处理）';
    END IF;
END$$
DELIMITER ;
CALL xiuxian_add_avatar_column();
DROP PROCEDURE IF EXISTS xiuxian_add_avatar_column;

-- ---------- 2) 历史账号处理 ----------
-- 默认头像规则：avatar 为空（或编码非法、或为 AUTO）时，头像 = 昵称第一个字。
-- 因此历史账号**无需回填任何值**，保持空串即可自动显示为昵称首字。
-- 下面这行只用于「清理此前误填的随机头像」，让所有老账号回到首字头像（可选执行）：
-- UPDATE users SET avatar = '' WHERE avatar NOT IN
--     ('XIAN','DAO','JIAN','DAN','FU','HE','SONG','YUN','YUE','LIAN','ZHU','JIU');
--
-- 若希望老账号强制统一用昵称首字（含已选过预设头像的），执行：
-- UPDATE users SET avatar = 'AUTO';

-- ---------- 3) 校验 ----------
--   SHOW FULL COLUMNS FROM users LIKE 'avatar';
--   SELECT avatar, COUNT(*) FROM users GROUP BY avatar;
--   -- 查看每位用户将显示的首字：
--   SELECT id, nickname, avatar, LEFT(nickname, 1) AS 首字 FROM users LIMIT 20;
-- ============================================================
