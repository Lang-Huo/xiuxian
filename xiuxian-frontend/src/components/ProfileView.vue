<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'

const emit = defineEmits(['test-root'])

const profile = ref(null)
const avatars = ref([])
const loading = ref(true)
const error = ref('')

const pickerOpen = ref(false)     // 换头像面板
const saving = ref(false)
const notice = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await api.profile()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 后端保证头像不为 null，这里再兜一层，防止旧版本接口返回空
const avatar = computed(() => profile.value?.avatar || { code: '', name: '未定', glyph: '仙', fg: '#9e3b34', bg: '#f7ece9' })
const spiritRoot = computed(() => profile.value?.spiritRoot || null)

async function togglePicker() {
  pickerOpen.value = !pickerOpen.value
  if (pickerOpen.value && avatars.value.length === 0) {
    try {
      avatars.value = await api.avatars()
    } catch (e) {
      error.value = e.message
    }
  }
}

async function chooseAvatar(a) {
  if (saving.value || a.code === avatar.value.code) return
  saving.value = true
  error.value = ''
  try {
    const saved = await api.setAvatar(a.code)
    profile.value = { ...profile.value, avatar: saved }
    notice.value = '已换上「' + saved.name + '」'
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

const pct = (v) => (v * 100).toFixed(1) + '%'
</script>

<template>
  <div class="profile">
    <p v-if="loading" class="tip">正在整理道友的行囊…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="profile">
      <!-- 名帖 -->
      <section class="panel namecard">
        <div
          class="avatar-lg"
          :style="{ background: avatar.bg, color: avatar.fg }"
        >{{ avatar.glyph }}</div>

        <div class="who">
          <div class="nick">
            {{ profile.nickname }}
            <span class="badge">{{ profile.realm }} · {{ profile.layer }} 层</span>
          </div>
          <div class="title">称号：{{ profile.title }}</div>
          <div class="sub">仙途编号 {{ profile.userNo || '—' }} · 修行 {{ profile.practiceDays }} 天</div>
        </div>

        <button class="mini pick" :disabled="saving" @click="togglePicker">
          {{ pickerOpen ? '收起' : '换头像' }}
        </button>
      </section>

      <!-- 灵根 -->
      <section v-if="spiritRoot" class="panel root-card">
        <div class="r-name">{{ spiritRoot.name }}</div>
        <div class="r-meta">
          <span class="rarity">{{ spiritRoot.rarity }}</span>
          <span class="mult">道行 ×{{ spiritRoot.multiplier }}</span>
        </div>
        <p class="r-desc">{{ spiritRoot.description }}</p>
      </section>
      <section v-else class="panel root-card untested">
        <div class="r-name muted">灵根未定</div>
        <p class="r-desc">测灵根后可获得答对题目的道行倍率加成。</p>
        <button class="btn primary small" @click="$emit('test-root')">立即测灵根</button>
      </section>

      <!-- 换头像 -->
      <section v-if="pickerOpen" class="panel picker">
        <h3>择一幅容颜</h3>
        <div class="av-grid">
          <button
            v-for="a in avatars"
            :key="a.code"
            class="av"
            :class="{ on: a.code === avatar.code }"
            :style="{ background: a.bg, color: a.fg }"
            :disabled="saving"
            @click="chooseAvatar(a)"
          >
            <span class="g">{{ a.glyph }}</span>
            <span class="n">{{ a.name }}</span>
          </button>
        </div>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </section>

      <!-- 个人信息 + 修为进度 -->
      <div class="cols">
        <section class="panel">
          <h3>个人信息</h3>
          <dl class="kv">
            <div><dt>仙途编号</dt><dd class="uno">{{ profile.userNo || '—' }}</dd></div>
            <div><dt>登录手机号</dt><dd>{{ profile.username }}</dd></div>
            <div><dt>当前境界</dt><dd>{{ profile.realm }} 第 {{ profile.layer }} 层</dd></div>
            <div><dt>入道日期</dt><dd>{{ profile.joinedAt }}</dd></div>
            <div><dt>累计道行</dt><dd>{{ profile.exp }}</dd></div>
            <div v-if="!profile.maxRealm">
              <dt>距 {{ profile.nextRealm }}</dt><dd>还需 {{ profile.expToNext }} 道行</dd>
            </div>
            <div v-else><dt>境界</dt><dd>已至巅峰（{{ profile.realm }}）</dd></div>
          </dl>
        </section>

        <section class="panel">
          <h3>修为进度</h3>
          <div class="row">
            <span class="label">境界进度</span>
            <span class="val">{{ pct(profile.progress) }}</span>
          </div>
          <div class="bar"><div class="fill exp" :style="{ width: pct(profile.progress) }"></div></div>

          <div class="row">
            <span class="label">气血</span>
            <span class="val">{{ profile.hp }} / {{ profile.maxHp }}</span>
          </div>
          <div class="bar"><div class="fill hp" :style="{ width: (profile.hp / profile.maxHp * 100) + '%' }"></div></div>

          <p class="tip-next" v-if="profile.nextRealm">
            再积 {{ profile.expToNext }} 道行，可晋 {{ profile.nextRealm }}
          </p>
        </section>
      </div>

      <!-- 战绩 -->
      <section class="panel">
        <h3>修炼战绩</h3>
        <div class="stats">
          <div class="stat"><div class="num">{{ profile.answerCount }}</div><div class="cap">累计作答</div></div>
          <div class="stat"><div class="num">{{ profile.correctCount }}</div><div class="cap">答对</div></div>
          <div class="stat"><div class="num">{{ (profile.accuracy * 100).toFixed(0) }}%</div><div class="cap">正确率</div></div>
          <div class="stat"><div class="num">{{ profile.topicCount }}</div><div class="cap">修炼主题</div></div>
          <div class="stat"><div class="num">{{ profile.questionCount }}</div><div class="cap">累计题目</div></div>
        </div>
      </section>

      <!-- 最近修炼 -->
      <section class="panel">
        <h3>最近修炼</h3>
        <p v-if="profile.recentTopics.length === 0" class="tip">尚未开始修炼，去「修炼」页起个头吧。</p>
        <ul v-else class="topics">
          <li v-for="t in profile.recentTopics" :key="t.id">
            <span class="kw">{{ t.keyword }}</span>
            <span class="tags">{{ t.difficulty }} · {{ t.count }} 题</span>
            <span class="time">{{ t.createdAt }}</span>
          </li>
        </ul>
      </section>

      <button class="btn refresh" @click="load">刷新</button>
    </template>
  </div>
</template>

<style scoped>
.profile { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; }
.notice {
  text-align: center; color: var(--seal); font-size: 13px; letter-spacing: 1px;
  margin: 12px 0 0;
}

.panel {
  position: relative; overflow: hidden; background: rgba(255,255,255,.55);
  border: 1px solid var(--line); border-radius: 4px; padding: 20px 22px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  margin-bottom: 16px;
}
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 60% 45% at 16% 0%, rgba(31,29,26,.05), transparent 62%);
}
.panel > * { position: relative; }
h3 {
  margin: 0 0 14px; font-size: 15px; color: var(--ink); letter-spacing: 2px;
  border-left: 3px solid var(--ink); padding-left: 10px; line-height: 1.2;
}

/* 名帖 */
.namecard { display: flex; align-items: center; gap: 18px; padding: 22px; }
.avatar-lg {
  width: 76px; height: 76px; flex: none; display: flex; align-items: center; justify-content: center;
  font-size: 38px; font-weight: 700; border-radius: 6px;
  border: 2px solid var(--seal); transform: rotate(-3deg);
  box-shadow: 0 0 0 3px rgba(158,59,52,.05), 0 2px 10px rgba(31,29,26,.14);
  transition: transform .2s;
}
.avatar-lg:hover { transform: rotate(0deg) scale(1.03); }
.who { min-width: 0; flex: 1; }
.nick {
  font-size: 22px; font-weight: 700; letter-spacing: 2px;
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.badge {
  font-size: 12px; font-weight: 700; letter-spacing: 1px; color: #fff; background: var(--ink);
  padding: 3px 10px; border-radius: 3px; transform: rotate(-1.5deg);
}
.title { color: var(--seal); font-size: 13px; letter-spacing: 1px; margin-top: 6px; }
.sub { color: var(--ink-light); font-size: 12px; margin-top: 4px; letter-spacing: 1px; }
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 4px 10px; font-family: inherit;
  letter-spacing: 1px; transition: all .2s; flex: none; align-self: flex-start;
}
.mini:hover:not(:disabled) { border-color: var(--ink); color: var(--ink); }
.mini:disabled { opacity: .5; }

/* 换头像面板 */
.picker { animation: inkIn .35s ease-out both; }
.av-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(76px, 1fr)); gap: 10px; }
.av {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 4px 6px; border: 1px solid var(--line); border-radius: 5px;
  background: transparent; font-family: inherit; cursor: pointer; transition: all .18s;
}
.av:hover:not(:disabled) { transform: translateY(-2px); border-color: var(--ink-soft); }
.av .g { font-size: 24px; font-weight: 700; line-height: 1; }
.av .n { font-size: 11px; color: var(--ink-light); letter-spacing: 1px; }
.av.on { border-color: var(--seal); box-shadow: 0 0 0 2px rgba(158,59,52,.14); }
.av.on .n { color: var(--seal); font-weight: 600; }
.av:disabled { opacity: .6; cursor: default; }

/* 灵根卡 */
.root-card { text-align: center; }
.root-card .r-name {
  font-size: 28px; font-weight: 700; color: var(--seal);
  letter-spacing: 8px; padding: 4px 0 8px;
}
.root-card.untested .r-name { font-size: 18px; letter-spacing: 4px; }
.root-card .r-name.muted { color: var(--ink-light); font-weight: 500; }
.r-meta { display: flex; justify-content: center; gap: 12px; margin-bottom: 12px; }
.r-meta .rarity { font-size: 12px; color: var(--ink-light); border: 1px solid var(--line); border-radius: 2px; padding: 1px 8px; }
.r-meta .mult { font-size: 12px; color: var(--seal); border: 1px solid var(--seal); border-radius: 2px; padding: 1px 8px; font-weight: 600; }
.r-desc { margin: 0; color: var(--ink-soft); font-size: 13px; line-height: 1.8; letter-spacing: 1px; }
.root-card.untested .r-desc { margin-bottom: 14px; }
.btn.primary.small { padding: 7px 22px; font-size: 13px; letter-spacing: 3px; }

/* 双栏 */
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.cols .panel { margin-bottom: 16px; }

/* 明细表 */
.kv { margin: 0; display: grid; grid-template-columns: 1fr; gap: 10px; }
.kv > div { display: flex; justify-content: space-between; gap: 8px; border-bottom: 1px dashed var(--line); padding-bottom: 6px; }
dt { color: var(--ink-light); font-size: 13px; letter-spacing: 1px; }
dd { margin: 0; font-size: 13px; color: var(--ink); font-weight: 600; text-align: right; }
dd.uno { color: var(--seal); letter-spacing: 2px; font-variant-numeric: tabular-nums; }

/* 进度条 */
.row { display: flex; justify-content: space-between; font-size: 13px; margin: 8px 0 5px; }
.label { color: var(--ink-soft); letter-spacing: 1px; }
.val { font-weight: 600; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; margin-bottom: 12px; }
.bar .fill { height: 100%; border-radius: 2px; transition: width .4s ease; }
.bar .fill.exp { background: linear-gradient(90deg, #6b665d, var(--ink)); }
.bar .fill.hp { background: linear-gradient(90deg, #c4897f, var(--seal)); }
.tip-next { margin: 2px 0 0; font-size: 12px; color: var(--ink-light); letter-spacing: 1px; }

/* 战绩 */
.stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; text-align: center; }
.stat .num { font-size: 20px; font-weight: 700; color: var(--ink); }
.stat .cap { font-size: 12px; color: var(--ink-light); margin-top: 2px; letter-spacing: 1px; }

/* 最近修炼 */
.topics { list-style: none; margin: 0; padding: 0; }
.topics li {
  display: flex; align-items: center; gap: 10px; padding: 9px 0;
  border-bottom: 1px dashed var(--line); font-size: 14px;
}
.topics li:last-child { border-bottom: none; }
.kw { font-weight: 600; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tags { color: var(--ink-soft); font-size: 12px; flex: none; }
.time { margin-left: auto; color: var(--ink-light); font-size: 12px; flex: none; }

.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 9px 20px; font-size: 14px;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s; }
.btn:hover { background: var(--ink); color: #fff; }
.refresh { display: block; margin: 0 auto; letter-spacing: 2px; }

@media (max-width: 620px) {
  .cols { grid-template-columns: 1fr; gap: 0; }
  .stats { grid-template-columns: repeat(3, 1fr); }
  .namecard { flex-wrap: wrap; }
  .mini.pick { margin-left: auto; }
}
</style>
