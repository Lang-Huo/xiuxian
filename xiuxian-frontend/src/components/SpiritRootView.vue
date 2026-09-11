<script setup>
import { ref, computed } from 'vue'
import { api } from '../api'

const emit = defineEmits(['close', 'done'])

const phase = ref('pick')           // pick / quiz / result
const difficulty = ref('入门')
const questions = ref([])
const answers = ref([])             // [{ questionId, userAnswer }]
const currentIdx = ref(0)
const result = ref(null)
const loading = ref(false)
const error = ref('')

const currentQ = computed(() => questions.value[currentIdx.value] || null)
const currentA = computed(() => answers.value[currentIdx.value] || null)
const progressLabel = computed(() => {
  if (phase.value !== 'quiz') return ''
  return `第 ${currentIdx.value + 1} / ${questions.value.length} 题`
})

async function start() {
  loading.value = true
  error.value = ''
  try {
    const resp = await api.spiritRootStart(difficulty.value)
    questions.value = resp.questions
    answers.value = resp.questions.map(q => ({ questionId: q.id, userAnswer: null }))
    currentIdx.value = 0
    phase.value = 'quiz'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

// 后端题库 answer 是字母（A/B/C/D）或布尔（true/false），需按 idx 翻译成同款 key
function optKey(idx) {
  if (currentQ.value && currentQ.value.type === 'JUDGE') {
    return idx === 0 ? 'true' : 'false'
  }
  return String.fromCharCode(65 + idx)
}

function choose(opt, idx) {
  answers.value[currentIdx.value].userAnswer = optKey(idx)
}

function next() {
  if (currentIdx.value + 1 >= questions.value.length) {
    submit()
  } else {
    currentIdx.value++
  }
}

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.spiritRootSubmit(answers.value)
    result.value = r
    phase.value = 'result'
    emit('done', r)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function backToPick() {
  phase.value = 'pick'
  questions.value = []
  answers.value = []
  result.value = null
  error.value = ''
}

const diffOptions = ['入门', '进阶', '精通']
</script>

<template>
  <div class="overlay">
    <div class="sheet">
      <!-- 顶部条 -->
      <header class="head">
        <span class="seal">测</span>
        <h2>{{ phase === 'pick' ? '觉醒灵根' : phase === 'quiz' ? '灵根试炼' : '灵根已定' }}</h2>
        <button v-if="phase !== 'quiz'" class="mini close" @click="$emit('close')">跳过</button>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <!-- 选难度 -->
      <section v-if="phase === 'pick'" class="panel inner">
        <p class="tip">灵根决定修行（答对）所得道行的倍率；答对越多，所得越稀有。本测共 5 题，可在右上角跳过。</p>
        <div class="diff-row">
          <button
            v-for="d in diffOptions"
            :key="d"
            class="diff"
            :class="{ on: difficulty === d }"
            @click="difficulty = d"
          >{{ d }}</button>
        </div>
        <div class="root-hint">
          <div v-for="(r, i) in [
            { name: '五灵根', rate: '≤20%' },
            { name: '双灵根', rate: '≥40%' },
            { name: '单灵根', rate: '≥60%' },
            { name: '变异灵根', rate: '≥80%' },
            { name: '天灵根', rate: '100%' },
            { name: '混沌灵根', rate: '100%（15% 概率）' }
          ]" :key="i" class="hint-row">
            <span class="h-name">{{ r.name }}</span>
            <span class="h-rate">{{ r.rate }}</span>
          </div>
        </div>
        <button class="btn primary big" :disabled="loading" @click="start">
          {{ loading ? '正在请神明出题…' : '开始测灵根' }}
        </button>
      </section>

      <!-- 5 题 -->
      <section v-else-if="phase === 'quiz' && currentQ" class="panel inner">
        <div class="progress">{{ progressLabel }}</div>
        <div class="q-stem">{{ currentQ.stem }}</div>

        <div class="opts">
          <button
            v-for="(opt, idx) in (currentQ.options || [])"
            :key="idx"
            class="opt"
            :class="{ picked: currentA && currentA.userAnswer === optKey(idx) }"
            @click="choose(opt, idx)"
          >{{ optKey(idx) }}. {{ opt }}</button>
        </div>

        <div class="actions">
          <button class="btn primary" :disabled="!currentA || !currentA.userAnswer || loading" @click="next">
            {{ currentIdx + 1 >= questions.length ? '揭晓灵根' : '下一题' }}
          </button>
        </div>
      </section>

      <!-- 结果 -->
      <section v-else-if="phase === 'result' && result" class="panel inner result">
        <div class="verdict">
          <div class="label">你觉醒的灵根</div>
          <div class="root-name">{{ result.root.name }}</div>
          <div class="root-rarity">{{ result.root.rarity }} · 道行倍率 ×{{ result.root.multiplier }}</div>
        </div>
        <p class="root-desc">{{ result.root.description }}</p>
        <div class="stats">
          <div class="stat"><div class="n">{{ result.correct }} / {{ result.total }}</div><div class="c">答对</div></div>
          <div class="stat"><div class="n">{{ (result.accuracy * 100).toFixed(0) }}%</div><div class="c">正确率</div></div>
        </div>
        <button class="btn primary big" @click="$emit('done')">开始修炼</button>
        <button class="link" @click="backToPick">再测一次</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(31, 29, 26, .35); backdrop-filter: blur(2px);
  display: flex; align-items: flex-start; justify-content: center;
  padding: 56px 16px 16px; overflow-y: auto;
  animation: inkIn .3s ease-out both;
}
.sheet {
  width: 100%; max-width: 520px;
  position: relative; overflow: hidden;
  background: rgba(255, 255, 255, .82);
  border: 1px solid var(--line); border-radius: 6px;
  padding: 22px 26px 28px;
  box-shadow: 0 8px 36px rgba(31, 29, 26, .18);
  animation: inkIn .4s ease-out both;
}
.sheet::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(ellipse 70% 55% at 20% 0%, rgba(31, 29, 26, .07), transparent 62%),
              radial-gradient(ellipse 60% 50% at 90% 12%, rgba(158, 59, 52, .06), transparent 60%);
}
.sheet > * { position: relative; }

.head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.seal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 34px; height: 34px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 18px; font-weight: 700; border-radius: 4px; background: rgba(158, 59, 52, .06);
  transform: rotate(-4deg); line-height: 1;
}
h2 { margin: 0; font-size: 19px; letter-spacing: 4px; color: var(--ink); flex: 1; }
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 4px 12px; font-family: inherit;
  letter-spacing: 1px; transition: all .2s;
}
.mini:hover { border-color: var(--ink); color: var(--ink); }

.error { color: var(--seal); text-align: center; font-size: 13px; margin-bottom: 8px; }

.panel.inner {
  background: transparent; border: none; box-shadow: none; padding: 0;
  margin: 0;
}
.panel.inner::before { display: none; }

.tip { color: var(--ink-soft); font-size: 13px; line-height: 1.8; margin: 0 0 14px; letter-spacing: 1px; }

.diff-row { display: flex; gap: 10px; margin-bottom: 16px; }
.diff {
  flex: 1; padding: 12px 0; border: 1px solid var(--line); border-radius: 4px;
  background: rgba(255, 255, 255, .55); color: var(--ink); font-family: inherit;
  font-size: 15px; letter-spacing: 4px; transition: all .18s;
}
.diff:hover { border-color: var(--ink-soft); }
.diff.on { background: var(--ink); color: #fff; border-color: var(--ink); }

.root-hint { margin-bottom: 18px; border-top: 1px dashed var(--line); padding-top: 12px; }
.hint-row { display: flex; justify-content: space-between; font-size: 12px; padding: 4px 0; color: var(--ink-soft); }
.h-name { color: var(--ink); font-weight: 600; letter-spacing: 1px; }
.h-rate { font-variant-numeric: tabular-nums; }

/* 答题中 */
.progress { color: var(--ink-light); font-size: 12px; letter-spacing: 2px; margin-bottom: 8px; }
.q-stem { color: var(--ink); font-size: 16px; line-height: 1.85; margin-bottom: 14px; padding: 14px; border: 1px dashed var(--line); border-radius: 4px; background: rgba(255, 255, 255, .55); }

.opts { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.opt {
  text-align: left; padding: 10px 12px; border: 1px solid var(--line); border-radius: 4px;
  background: rgba(255, 255, 255, .55); color: var(--ink); font-family: inherit;
  font-size: 14px; transition: all .18s; line-height: 1.5;
}
.opt:hover { border-color: var(--ink-soft); }
.opt.picked { border-color: var(--seal); background: rgba(158, 59, 52, .08); color: var(--ink); font-weight: 600; }

.actions { display: flex; justify-content: flex-end; }

/* 结果 */
.result { text-align: center; }
.verdict .label { color: var(--ink-light); font-size: 13px; letter-spacing: 3px; margin-bottom: 6px; }
.root-name {
  font-size: 42px; font-weight: 700; color: var(--seal);
  letter-spacing: 12px; padding: 6px 0 4px;
  text-shadow: 0 0 14px rgba(158, 59, 52, .18);
  animation: sealStamp .8s cubic-bezier(.2, .8, .3, 1) both;
}
.root-rarity { color: var(--ink-soft); font-size: 13px; letter-spacing: 2px; margin-bottom: 18px; }
.root-desc { color: var(--ink-soft); font-size: 13px; line-height: 1.85; margin: 0 auto 22px; max-width: 360px; }
.stats { display: flex; gap: 26px; justify-content: center; margin-bottom: 22px; }
.stat .n { font-size: 20px; font-weight: 700; color: var(--ink); }
.stat .c { font-size: 12px; color: var(--ink-light); margin-top: 2px; letter-spacing: 2px; }

.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 10px 22px; font-size: 14px; font-weight: 600;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s; }
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover:not(:disabled) { background: #8a312b; border-color: #8a312b; }
.btn.big { width: 100%; padding: 13px; font-size: 15px; margin-top: 4px; letter-spacing: 4px; }
.btn:disabled { opacity: .5; cursor: default; }

.link { background: transparent; border: none; color: var(--ink-light); font-size: 13px; cursor: pointer; margin-top: 12px; letter-spacing: 1px; }
.link:hover { color: var(--ink); }
</style>