# 仙途 · AI 修仙主题自适应学习应用 — MVP

> 输入想学的知识 → AI 出题（判断 / 选择 + 答案 + 解析）→ 答题修仙：经验、境界、血量、宝物（规划中）。

本仓库为计划书对应的 **MVP 前后端**，已本地验证可运行。

```
xiuxian/
├── xiuxian-backend/      # Spring Boot 3.2 + Java 21 + MyBatis-Plus + MySQL（H2 本地可免装）
├── xiuxian-frontend/     # Vue 3 + Vite
├── tooling/              # 本机自带可用 Maven（仅沙箱环境，已被 .gitignore 忽略）
├── 修仙学习应用计划书.md    # 项目计划书
└── 修仙学习应用计划书.html   # 计划书（带图表）
```

## 一、后端运行

### 方式 A：本地免数据库（推荐先体验）
使用 H2 内存库，无需安装 MySQL：

```bash
cd xiuxian-backend
./mvnw spring-boot:run            # 或：java -jar target/xiuxian-backend-0.1.0.jar --spring.profiles.active=local
```

启动后：
- API：`http://localhost:8080/api`
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL：`jdbc:h2:mem:xiuxian`）

### 方式 B：连接 MySQL（生产/计划书默认）
1. 本机准备 MySQL，创建库：
   ```sql
   CREATE DATABASE xiuxian CHARACTER SET utf8mb4;
   ```
2. 在 `xiuxian-backend/src/main/resources/application.yml` 中修改：
   ```yaml
   spring.datasource.username: 你的账号
   spring.datasource.password: 你的密码
   ```
   （也可通过环境变量 `XIUXIAN_DB_USER` / `XIUXIAN_DB_PASSWORD` 注入）
3. 启动时由 `spring.sql.init` 自动执行 `src/main/resources/schema.sql` 建表（含 `IF NOT EXISTS`，可重复运行），直接启动即可。
4. **已有旧库升级**：`IF NOT EXISTS` 不会给已存在的表补列。若表建于「用户名 / 昵称」拆分之前，需先补列：
   ```sql
   ALTER TABLE users ADD COLUMN username VARCHAR(11) NOT NULL DEFAULT '' COMMENT '登录手机号（11位，唯一登录凭证）' AFTER id;
   UPDATE users SET username = LPAD(id, 11, '0') WHERE username = '';    -- 回填占位（旧账号无真实手机号，仅满足唯一索引）
   ALTER TABLE users ADD UNIQUE KEY uk_users_username (username);         -- 回填后再建唯一索引，否则会因重复空值失败
   ```
   > 用户名已规定为 **11 位手机号**，故列长为 `VARCHAR(11)`。旧账号回填的是补零占位值，**无法用手机号登录**，建议导出后清理或引导重新注册。
   >
   > ⚠️ 收紧列长前先检查存量数据，避免截断：
   > `SELECT id, username, LENGTH(username) FROM users WHERE LENGTH(username) > 11;`

   完整的补列与补注释脚本见 `db/add-comments-to-existing-db.sql`。

   若表建于「仙途编号」之前，还需补 `user_no` 列，直接执行幂等脚本：
   ```bash
   mysql -u root -p xiuxian < db/add-user-no-to-existing-db.sql
   ```
   > 该脚本会：补列 → 回填历史账号（`1` + id 左补零共 6 位）→ 建唯一索引 `uk_users_user_no`。
   > 不执行也能启动：老账号在首次登录/查询时会由服务端自动补发编号（懒迁移）。

### 主要接口
| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册（body: `username`（**手机号**）, `nickname`, `password`）→ 返回 `token` + 用户状态 | 否 |
| POST | `/api/auth/login` | 登录（body: `username`（**手机号**）, `password`）→ 返回 `token` + 用户状态 | 否 |
| POST | `/api/auth/me` | 当前登录用户状态 | 是 |
| POST | `/api/topics/generate` | 输入主题生成题目（body: `topic, difficulty, count`） | 是 |
| POST | `/api/answer` | 提交答案结算（body: `questionId, userAnswer`） | 是 |
| GET  | `/api/users/profile` | 当前用户**详细信息**（含境界进度、距下境界所需道行、手机号脱敏、修行天数、修炼统计、最近修炼记录） | 是 |
| POST | `/api/users/me` | 当前用户状态 | 是 |
| GET  | `/api/users/{id}` | 查询用户状态 | 是 |
| GET  | `/api/avatars` | **头像库**列表（首项「本名」= 当前昵称首字，其后 12 款预设，含字/名/配色） | 是 |
| POST | `/api/users/avatar` | 设置头像（body: `code`） | 是 |
| GET  | `/api/bag` | 查看**背包**（容量构成、已佩戴装备、行囊格子） | 是 |
| POST | `/api/bag/equip` | 装备宝物（body: `itemId`；同槽位自动替换） | 是 |
| POST | `/api/bag/unequip` | 卸下宝物（body: `itemId`） | 是 |
| POST | `/api/bag/use` | 服用丹药（body: `itemId`） | 是 |
| POST | `/api/bag/discard` | 丢弃宝物（body: `itemId`, `count` 可选，默认 1） | 是 |

> 受保护接口需在请求头携带 `Authorization: Bearer <token>`。无令牌/令牌失效返回 401。
> JWT 密钥：环境变量 `XIUXIAN_JWT_SECRET`（默认开发密钥，生产务必替换）；有效期 `XIUXIAN_JWT_EXPIRE`（小时，默认 168）。

### 前端登录
打开页面后先进入「登录 / 注册」页：

- **登录**：输入 **手机号** + 密码
- **注册**：输入 **手机号**（登录凭证，唯一）、**昵称**（展示用，可重复）、密码（至少 6 位）
- 手机号输入框仅接受数字，最多 11 位；提交前前端会校验 `^1[3-9]\d{9}$`，后端同样校验（不符返回 400）

成功后令牌保存在 `localStorage`，刷新仍保持登录；右上角可「退出登录」。

登录后顶部为「**修炼** / **背包** / **我的**」三个标签：
- **修炼**：输入想学的知识 → 出题答题 → 查看解析（答对才随机掉落宝物，结果页直接提示；本轮结束汇总「本轮机缘」）
- **背包**：储物空间进度（已用 / 总格数，标明境界基础与装备加成）、4 个装备槽、行囊格子网格（空格一眼可见）；点格子看详情，可装备 / 卸下 / 服用 / 丢弃
- **我的**：**名帖式大卡**布局 —— 顶部名帖（左侧大头像 + 右侧昵称/境界徽章/称号/仙途编号/修行天数，右上角「换头像」可切换 12 款内置头像）；下方左侧「个人信息」（编号、手机号、境界、入道日期、道行、距下境界）、右侧「修为进度」（境界条 + 气血条 + 进阶提示）；再往下是修炼战绩 5 宫格与最近 5 条修炼记录；切到该页会自动重新拉取。

> 切到「背包」「我的」都会重新拉取数据；在背包中服用丹药会同步刷新顶部气血。

> 说明：**用户名即手机号**（11 位，唯一登录凭证）；昵称仅用于界面展示，允许重复。

### 仙途编号（对外用户ID）

- 数据库列 `users.user_no`，`VARCHAR(6)` + 唯一索引，规则：**6 位数字、首位非 0、全站唯一**
- **注册时自动分配**：随机取 `[100000, 999999]`，撞号重摇（最多 20 次），数据库唯一索引兜底
- 序号逻辑集中在 `com.xiuxian.util.UserNoUtil`（取值区间 / 正则 / 重试次数），避免多处漂移
- 老账号（加列前注册）编号为空，首次登录或查询 `/api/users/me` 时由 `AuthService#ensureUserNo` 自动补发
- 注册、登录、`/api/users/me`、`/api/users/profile` 的响应均含 `userNo`；「我的」页顶部与个人信息中展示

### 头像（默认昵称首字 + 12 款内置库）

- **默认头像 = 昵称第一个字**（编码 `AUTO`）：注册即生效，改昵称后头像自动跟着变，无需重新设置
- 另有 12 款水墨修仙风可选：**仙缘 / 问道 / 剑心 / 丹火 / 符箓 / 孤鹤 / 苍松 / 闲云 / 明月 / 青莲 / 翠竹 / 醉仙**，每款 = 单字 + 字色 + 淡色底
- 预设头像定义集中在 `com.xiuxian.service.AvatarPolicy#AVATARS`，**新增一款只需加一行**，前后端自动生效，不用改表结构
- 数据库 `users.avatar` 存编码（`VARCHAR(32)`）：`AUTO` 或空 = 昵称首字，其余为预设编码；老库执行 `mysql -u root -p xiuxian < db/add-avatar-to-existing-db.sql` 补列即可（幂等，**无需回填**，历史账号留空就自动显示首字）
- 编码非法、为空或不在图鉴内（脏数据）时，`AvatarPolicy.of(code, nickname)` 一律回退到昵称首字，**绝不返回 null**，前端无需判空
- 「我的」页顶部名帖右侧「换头像」展开选择面板，第一项「本名」即昵称首字，点选即保存
### 背包（储物）系统

**容量 = 境界基础格数 + 已装备宝物的格数加成**

| 境界 | 基础格数 | 境界 | 基础格数 |
|------|---------|------|---------|
| 凡人 | **10** | 元婴 | 30 |
| 练气 | 15 | 化神 | 35 |
| 筑基 | 20 | 炼虚 | 40 |
| 金丹 | 25 | 合体 | 45 |
|      |         | 大乘 | 50 |

- 每提升一个大境界 +5 格，规则集中在 `com.xiuxian.service.BagPolicy#BASE_SLOTS`，调参改一处即可
- **装备加成**：储物法宝类宝物带 `bag_bonus`，装备后累加到容量（如「乾坤袋 +12 格」）；卸下即失效
- 同种宝物堆叠，只占一格；已装备的宝物仍占一格
- 装备槽共 4 个（本命法器 / 护身宝衣 / 储物法宝 / 功法秘籍），每槽限一件，装备同槽新物会自动替换旧的
- **宝物来源**：**仅答对**才随机判定掉落，再按 `drop_weight` 权重抽取具体宝物；背包已满时掉落失败，仅提示「擦肩而过」

  | 情形 | 掉落概率 | 说明 |
  |------|---------|------|
  | 答对 · 入门 | 12% | 基准值 `GameService.DROP_RATE_CORRECT` |
  | 答对 · 进阶 | 15.6% | 难度系数 ×1.3 |
  | 答对 · 精通 | 19.2% | 难度系数 ×1.6 |
  | **答错** | **0%** | `DROP_RATE_WRONG = 0.0`，答错不掉宝物（改成正数即可恢复） |
  | —— | 上限 35% | `DROP_RATE_MAX`，避免刷精通题把背包撑爆 |

  掉落结果随答题结算一起返回（`AnswerResponse.drop` / `bagFull`），结果页会弹出「拾获 / 机缘」提示，本轮结束还会汇总「本轮机缘」清单
- 已装备法器/宝衣/功法还会提供**经验加成**（`exp_bonus`），直接作用于每题结算
- 丹药可服用回血，用尽自动销毁该格

预置宝物图鉴见 `schema.sql`（11 件）：储物法宝 4 件（+5 / +12 / +25 / +50 格）、丹药 3 种、法器 2 件、宝衣 1 件、功法 1 门。
已有老库执行 `mysql -u root -p xiuxian < db/add-bag-tables-to-existing-db.sql` 建表并灌入图鉴（幂等）。

## 二、前端运行

```bash
cd xiuxian-frontend
npm install
npm run dev          # 默认 http://localhost:5173
```

前端已配置开发代理：`/api` → `http://localhost:8080`，无需处理跨域。
打开页面后输入知识主题，即可开始"答题修仙"。

## 三、MVP 已实现 vs 规划
- ✅ 主题输入 → 出题（判断/选择）→ 逐题作答 → 强制展示解析 + 来源
- ✅ 经验 / 血量 / 境界（凡人→练气→…→大乘）闭环，答错扣血、答对小幅回血
- ✅ AI 出题可插拔：默认 Mock（离线可玩），配置 `xiuxian.ai.enabled=true` + API Key 后切换真实大模型（OpenAI 兼容）
- ⏳ 规划中（v1.0）：灵根觉醒、宝物掉落、称号、境界动画、账号体系

## 四、接入真实大模型（可选）
在 `application.yml` 或环境变量中配置：
```yaml
xiuxian:
  ai:
    enabled: true
    base-url: https://api.openai.com/v1   # 或 DeepSeek / 通义 / 文心 的兼容地址
    api-key: sk-xxxx
    model: gpt-4o-mini
```
出题将以 JSON 形式返回精准题目；失败自动回退 Mock。

## 五、技术栈
- 后端：Java 21 · Spring Boot 3.2（Web / MyBatis-Plus / Validation / JWT 鉴权）· MySQL（默认）· H2（本地）
- 前端：Vue 3 · Vite 5 · 原生 CSS（国风 UI）

---
*本 MVP 用于验证核心闭环，数值（经验/扣血阈值）可在 `GameService` 与 `RealmPolicy` 中统一调参。*
