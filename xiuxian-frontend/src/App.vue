<script setup>
import { ref, onMounted } from 'vue'
import { api, token, setToken, clearToken } from './api'
import GameStatus from './components/GameStatus.vue'
import QuestionCard from './components/QuestionCard.vue'
import AuthView from './components/AuthView.vue'
import InkBackground from './components/InkBackground.vue'
import ProfileView from './components/ProfileView.vue'
import BagView from './components/BagView.vue'

const authed = ref(false)
const userState = ref(null)
const topic = ref('')
const difficulty = ref('入门')
const count = ref(5)

const questions = ref([])
const currentIndex = ref(0)
const result = ref(null)
const finished = ref(false)

const loading = ref(false)
const error = ref('')

const session = ref({ total: 0, correct: 0, drops: [] })   // drops：本轮拾获的宝物

// 主导航：修炼 / 背包 / 我的
const tab = ref('practice')
const profileKey = ref(0)          // 切到「我的」时自增，强制重新拉取详情
const bagKey = ref(0)              // 切到「背包」时自增，强制重新拉取行囊

function switchTab(t) {
  tab.value = t
  if (t === 'profile') profileKey.value++
  if (t === 'bag') bagKey.value++
}

onMounted(async () => {
  if (token.value) await loadMe()
})

async function loadMe() {
  try {
    userState.value = await api.me()
    authed.value = true
  } catch (e) {
    // 令牌失效：回到登录页
    clearToken()
    authed.value = false
    userState.value = null
  }
}

async function onAuthSuccess(resp) {
  setToken(resp.token)
  userState.value = resp.user
  authed.value = true
}

function logout() {
  clearToken()
  authed.value = false
  userState.value = null
  questions.value = []
  finished.value = false
  result.value = null
  currentIndex.value = 0
  error.value = ''
  tab.value = 'practice'
}

async function startPractice() {
  error.value = ''
  if (!topic.value.trim()) { error.value = '请先输入你想修炼的知识'; return }
  if (userState.value && !userState.value.canAnswer) {
    error.value = '气血已耗尽，无法开始，请先回血。'
    return
  }
  loading.value = true
  try {
    const resp = await api.generate(topic.value.trim(), difficulty.value, count.value)
    questions.value = resp.questions
    currentIndex.value = 0
    result.value = null
    finished.value = false
    session.value = { total: resp.questions.length, correct: 0, drops: [] }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function handleSubmit(answer) {
  error.value = ''
  const q = questions.value[currentIndex.value]
  try {
    const r = await api.answer(q.id, answer)
    result.value = r
    if (r.correct) session.value.correct++
    if (r.drop) session.value.drops.push(r.drop)
    await loadMe()
  } catch (e) {
    error.value = e.message
  }
}

function handleNext() {
  result.value = null
  if (currentIndex.value + 1 >= questions.value.length) {
    finished.value = true
  } else {
    currentIndex.value++
  }
}

function restart() {
  questions.value = []
  finished.value = false
  result.value = null
  currentIndex.value = 0
}
</script>

<template>
  <InkBackground />

  <div class="app">
    <header>
      <div class="brand">
        <span class="seal">仙</span>
        <h1>仙途 · 修仙学习</h1>
      </div>
      <p class="sub">输入你想学的知识，AI 为你出题 · 答题修仙，证道长生</p>
      <button v-if="authed" class="logout" @click="logout">退出登录</button>
    </header>

    <!-- 未登录：登录 / 注册 -->
    <AuthView v-if="!authed" @success="onAuthSuccess" />

    <!-- 已登录：修炼主界面 -->
    <template v-else>
      <div v-if="userState" class="status-wrap">
        <GameStatus :state="userState" />
      </div>

      <nav class="tabs">
        <button class="tab" :class="{ active: tab === 'practice' }" @click="switchTab('practice')">修炼</button>
        <button class="tab" :class="{ active: tab === 'bag' }" @click="switchTab('bag')">背包</button>
        <button class="tab" :class="{ active: tab === 'profile' }" @click="switchTab('profile')">我的</button>
      </nav>

      <template v-if="tab === 'practice'">
      <!-- 首页 / 配置 -->
      <section v-if="questions.length === 0" class="panel">
        <div class="field">
          <label>想修炼的知识</label>
          <input v-model="topic" placeholder="如：量子力学基础 / 唐朝历史 / Python 装饰器" />
        </div>
        <div class="field row">
          <div>
            <label>难度</label>
            <select v-model="difficulty">
              <option>入门</option>
              <option>进阶</option>
              <option>精通</option>
            </select>
          </div>
          <div>
            <label>题数</label>
            <input type="number" min="1" max="20" v-model.number="count" />
          </div>
        </div>
        <button class="btn primary big" :disabled="loading || (userState && !userState.canAnswer)" @click="startPractice">
          {{ loading ? '正在请神明出题…' : '开始修炼' }}
        </button>
      </section>

      <!-- 答题中 -->
      <section v-else-if="!finished" class="panel">
        <QuestionCard
          :question="questions[currentIndex]"
          :index="currentIndex"
          :total="questions.length"
          :disabled="userState && !userState.canAnswer"
          :result="result"
          @submit="handleSubmit"
          @next="handleNext"
        />
      </section>

      <!-- 完成 -->
      <section v-else class="panel center">
        <h2>本轮修炼完成</h2>
        <p>共 {{ session.total }} 题，答对 {{ session.correct }} 题。</p>

        <div v-if="session.drops.length" class="drops">
          <div class="drops-title">本轮机缘</div>
          <ul>
            <li v-for="(d, i) in session.drops" :key="i">
              <span class="dname">「{{ d.name }}」</span>
              <span class="dquality">{{ d.rarity }}</span>
            </li>
          </ul>
          <p class="drops-tip">已收入行囊，可到「背包」查看与佩戴。</p>
        </div>
        <p v-else class="drops-none">此番未有宝物现世，再修一轮或有机缘。</p>

        <button class="btn primary" @click="restart">再修一轮</button>
      </section>

        <p v-if="error" class="error">{{ error }}</p>
      </template>

      <!-- 背包（储物）页面 -->
      <BagView v-else-if="tab === 'bag'" :key="bagKey" @changed="loadMe" />

      <!-- 个人页面 -->
      <ProfileView v-else :key="profileKey" />
    </template>

    <footer>仙途 MVP · 后端 Spring Boot + MyBatis-Plus + MySQL · 前端 Vue3</footer>
  </div>
</template>

<style scoped>
.app {
  max-width: 720px; margin: 0 auto; padding: 32px 20px 60px;
  position: relative; z-index: 1;          /* 浮于水墨背景之上 */
  animation: inkIn .7s ease-out both;      /* 墨落宣纸的入场 */
}
header { text-align: center; margin-bottom: 22px; position: relative; }
.brand { display: inline-flex; align-items: center; gap: 12px; justify-content: center; }
.seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 20px; font-weight: 700; border-radius: 5px; background: rgba(158,59,52,.06);
  line-height: 1; transform: rotate(-4deg);
  box-shadow: 0 0 0 3px rgba(158,59,52,.05), 0 1px 6px rgba(158,59,52,.18);
  animation: sealStamp .8s .15s cubic-bezier(.2,.8,.3,1) both;
}
h1 {
  margin: 0; font-size: 30px; letter-spacing: 6px; color: var(--ink); font-weight: 700;
  position: relative; padding-bottom: 12px;
}
/* 标题笔触：一道由左扫出的墨线 */
h1::after {
  content: ""; position: absolute; left: 6%; right: 6%; bottom: 2px; height: 3px;
  border-radius: 50%; transform-origin: left center;
  background: linear-gradient(90deg,
    transparent, rgba(31,29,26,.5) 8%, rgba(31,29,26,.18) 45%, rgba(31,29,26,.55) 80%, transparent);
  filter: blur(.4px);
  animation: brushSweep .9s .25s ease-out both;
}
.sub { color: var(--ink-light); font-size: 14px; margin-top: 8px; letter-spacing: 1px; }
.logout { position: absolute; right: 0; top: 8px; background: transparent; border: 1px solid var(--line);
  color: var(--ink-light); border-radius: 4px; padding: 5px 12px; font-size: 13px; transition: all .2s; }
.logout:hover { border-color: var(--ink); color: var(--ink); }
.status-wrap { margin-bottom: 20px; }

/* 主导航：修炼 / 我的 */
.tabs { display: flex; justify-content: center; gap: 28px; margin-bottom: 18px; }
.tab {
  position: relative; background: transparent; border: none; font-family: inherit;
  padding: 6px 4px 12px; font-size: 15px; letter-spacing: 3px; color: var(--ink-light);
  transition: color .2s;
}
.tab:hover { color: var(--ink-soft); }
.tab.active { color: var(--ink); font-weight: 700; }
/* 选中项下方一道笔触墨线 */
.tab::after {
  content: ""; position: absolute; left: 0; right: 0; bottom: 2px; height: 3px; border-radius: 50%;
  background: linear-gradient(90deg,
    transparent, rgba(31,29,26,.5) 15%, rgba(31,29,26,.18) 50%, rgba(31,29,26,.5) 85%, transparent);
  transform: scaleX(0); transform-origin: center; transition: transform .25s ease; filter: blur(.4px);
}
.tab.active::after { transform: scaleX(1); }
.panel {
  position: relative; overflow: hidden;
  background: rgba(255,255,255,.55); border: 1px solid var(--line); border-radius: 4px; padding: 24px;
  box-shadow: 0 2px 18px rgba(31,29,26,.06); backdrop-filter: blur(2px);
  animation: inkIn .55s ease-out both;
}
/* 卡片内的淡墨晕染，如宣纸吸墨 */
.panel::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(ellipse 60% 45% at 18% 0%,  rgba(31,29,26,.05), transparent 62%),
    radial-gradient(ellipse 50% 40% at 92% 22%, rgba(31,29,26,.04), transparent 60%);
}
.panel > * { position: relative; }   /* 内容浮于墨晕之上 */
.field { margin-bottom: 16px; }
.field.row { display: flex; gap: 16px; }
.field.row > div { flex: 1; }
label { display: block; font-size: 13px; color: var(--ink-soft); margin-bottom: 6px; letter-spacing: 1px; }
input, select {
  width: 100%; padding: 11px 12px; border: 1px solid var(--line); border-radius: 4px;
  font-size: 15px; font-family: inherit; background: rgba(255,255,255,.7); color: var(--ink); transition: border-color .2s;
}
input:focus, select:focus { outline: none; border-color: var(--ink); }
.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 10px 22px; font-size: 15px; font-weight: 600;
  background: transparent; color: var(--ink); transition: all .2s; }
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover:not(:disabled) { background: #8a312b; border-color: #8a312b; }
.btn.big { width: 100%; padding: 14px; font-size: 16px; margin-top: 6px; letter-spacing: 2px; }
.center { text-align: center; }
.center h2 { color: var(--ink); letter-spacing: 3px; }

/* 本轮掉落清单 */
.drops {
  margin: 18px auto 0; max-width: 380px; text-align: left;
  border: 1px dashed var(--seal); border-radius: 4px; padding: 12px 16px;
  background: rgba(158,59,52,.06); animation: inkIn .5s ease-out both;
}
.drops-title { color: var(--seal); font-size: 13px; font-weight: 700; letter-spacing: 2px; margin-bottom: 8px; }
.drops ul { list-style: none; margin: 0; padding: 0; }
.drops li { display: flex; align-items: center; gap: 8px; padding: 5px 0; border-bottom: 1px dashed var(--line); }
.drops li:last-child { border-bottom: none; }
.dname { font-weight: 600; color: var(--ink); letter-spacing: 1px; }
.dquality { font-size: 12px; color: var(--seal); border: 1px solid var(--seal); border-radius: 2px; padding: 0 5px; }
.drops-tip { margin: 8px 0 0; font-size: 12px; color: var(--ink-light); }
.drops-none { color: var(--ink-light); font-size: 13px; margin-top: 14px; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; }
footer {
  text-align: center; color: var(--ink-light); font-size: 12px;
  margin-top: 36px; padding-top: 18px; letter-spacing: 1px; position: relative;
}
footer::before {
  content: ""; position: absolute; top: 0; left: 22%; right: 22%; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(31,29,26,.2), transparent);
}
</style>
