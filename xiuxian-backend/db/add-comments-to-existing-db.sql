-- ============================================================
-- 仙途 · 已有 MySQL 库「补充字段中文注释」脚本
-- 文件：db/add-comments-to-existing-db.sql
--
-- 适用：表已由旧版（无 COMMENT）的 schema.sql 创建，需要补齐字段注释
-- 原因：CREATE TABLE IF NOT EXISTS 对「已存在的表」不会做任何变更，因此注释不会自动补上
--
-- 【重要注意事项】
--   1. MySQL 的 MODIFY COLUMN 必须完整重述列定义（类型 / 非空 / 默认值），否则会丢失原有约束
--   2. 自增主键列必须显式保留 AUTO_INCREMENT，否则会丢掉自增属性，导致插入不传 id 时报错
--   3. 本脚本幂等，可重复执行
--   4. 建议低峰执行并先备份数据
--   5. 若某列不存在（例如早期版本缺 password 列），该行会报错，请先执行文件末尾【补列】段落
--
-- 执行方式：
--   mysql -u root -p xiuxian < add-comments-to-existing-db.sql
--   （如客户端不在目标库，请先执行： USE xiuxian; ）
-- ============================================================

USE xiuxian;

-- ======================= users 用户表 =======================
ALTER TABLE users
    MODIFY COLUMN id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
    MODIFY COLUMN username      VARCHAR(64)  NOT NULL DEFAULT '' UNIQUE COMMENT '登录用户名（唯一，登录凭证）',
    MODIFY COLUMN nickname      VARCHAR(64)  NOT NULL DEFAULT '道友'  COMMENT '道号/昵称（展示用，可重复）',
    MODIFY COLUMN password      VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '登录密码（BCrypt 哈希存储）',
    MODIFY COLUMN realm         VARCHAR(32)  NOT NULL DEFAULT '凡人' COMMENT '修仙境界（凡人/练气/筑基/金丹/元婴/化神…）',
    MODIFY COLUMN layer         INT          NOT NULL DEFAULT 1      COMMENT '当前境界层数',
    MODIFY COLUMN exp           INT          NOT NULL DEFAULT 0      COMMENT '累计道行/经验值',
    MODIFY COLUMN hp            INT          NOT NULL DEFAULT 100    COMMENT '当前气血值（耗尽则不可答题）',
    MODIFY COLUMN max_hp        INT          NOT NULL DEFAULT 100    COMMENT '气血上限',
    MODIFY COLUMN correct_count INT          NOT NULL DEFAULT 0      COMMENT '累计答对题数',
    MODIFY COLUMN answer_count  INT          NOT NULL DEFAULT 0      COMMENT '累计作答题数',
    MODIFY COLUMN created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

-- ======================= topics 主题表 =======================
ALTER TABLE topics
    MODIFY COLUMN id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主题ID（主键）',
    MODIFY COLUMN user_id    BIGINT       NOT NULL DEFAULT 1      COMMENT '所属用户ID',
    MODIFY COLUMN keyword    VARCHAR(512) NOT NULL                COMMENT '修炼主题/想学的知识',
    MODIFY COLUMN difficulty VARCHAR(32)  NOT NULL DEFAULT '入门' COMMENT '难度（入门/进阶/精通）',
    MODIFY COLUMN `count`    INT          NOT NULL DEFAULT 5      COMMENT '生成题目数量',
    MODIFY COLUMN created_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ======================= questions 题目表 =======================
ALTER TABLE questions
    MODIFY COLUMN id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID（主键）',
    MODIFY COLUMN topic_id        BIGINT       NOT NULL                COMMENT '所属主题ID',
    MODIFY COLUMN `type`          VARCHAR(16)  NOT NULL DEFAULT 'SINGLE' COMMENT '题型（SINGLE 单选 / JUDGE 判断）',
    MODIFY COLUMN stem            TEXT         NOT NULL                COMMENT '题干',
    MODIFY COLUMN options         TEXT                                  COMMENT '选项（JSON 数组文本，如 ["A","B","C","D"]）',
    MODIFY COLUMN answer          VARCHAR(16)  NOT NULL                COMMENT '正确答案（单选为字母；判断为 true/false）',
    MODIFY COLUMN explanation     TEXT                                  COMMENT '答案解析',
    MODIFY COLUMN knowledge_point VARCHAR(256)                          COMMENT '知识点',
    MODIFY COLUMN source_url      VARCHAR(1024)                         COMMENT '知识来源链接',
    MODIFY COLUMN difficulty      VARCHAR(16)  DEFAULT '入门'           COMMENT '难度（入门/进阶/精通）';


-- ============================================================
-- 【补列】仅在缺列时才需要执行
-- 判断：DESC users; 看是否已有下列字段，缺哪个补哪个
-- 注意：上面 MODIFY 段引用的列若不存在会整条报错，请先补齐列再执行 MODIFY
-- ============================================================
-- 1) 登录用户名（区分登录账号与展示昵称后新增；现规定用户名必须是 11 位手机号）
--    旧库通常没有这一列，需先补列 + 回填 + 再加唯一索引：
-- ALTER TABLE users ADD COLUMN username VARCHAR(11) NOT NULL DEFAULT '' COMMENT '登录手机号（11位，唯一登录凭证）' AFTER id;
-- UPDATE users SET username = LPAD(id, 11, '0') WHERE username = '';    -- 回填占位：旧账号没有真实手机号，
--                                                                       -- 补零仅为满足唯一索引，这批账号无法用手机号登录，
--                                                                       -- 建议导出后清理，或引导用户重新注册
-- ALTER TABLE users ADD UNIQUE KEY uk_users_username (username);         -- 回填后再建唯一索引，否则会因重复空值失败
--
-- 2) 登录密码（早期 JPA 版建的表可能缺失）
-- ALTER TABLE users ADD COLUMN password VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt 哈希存储）' AFTER nickname;


-- ============================================================
-- 【可选】给表本身加注释，便于在 INFORMATION_SCHEMA.TABLES 查看
-- ============================================================
-- ALTER TABLE users     COMMENT '修仙者用户表：存储境界、经验、气血与答题统计';
-- ALTER TABLE topics    COMMENT '修炼主题表：用户输入的想学知识与出题参数';
-- ALTER TABLE questions COMMENT '题目表：AI 生成的题目、答案与解析';


-- ============================================================
-- 【校验】执行完后，可用以下 SQL 检查注释是否已生效
--   SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = 'xiuxian' AND TABLE_NAME IN ('users','topics','questions')
--   ORDER BY TABLE_NAME, ORDINAL_POSITION;
--
-- 也可直接用 SHOW 查看单表：
--   SHOW FULL COLUMNS FROM users;
-- ============================================================
