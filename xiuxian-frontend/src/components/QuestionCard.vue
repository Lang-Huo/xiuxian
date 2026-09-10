<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, required: true },
  total: { type: Number, required: true },
  disabled: { type: Boolean, default: false },
  result: { type: Object, default: null }
})
const emit = defineEmits(['submit', 'next'])

const selected = ref(null)

const isJudge = computed(() => props.question.type === 'JUDGE')

// 选项展示：JUDGE 用 正确/错误；SINGLE 直接用选项文本
const optionLabels = computed(() =>
  props.question.options.map((opt, i) => ({
    key: isJudge.value ? (i === 0 ? 'true' : 'false') : String.fromCharCode(65 + i),
    text: opt
  }))
)

const canSubmit = computed(() => selected.value !== null && !props.result && !props.disabled)

function choose(key) {
  if (props.result || props.disabled) return
  selected.value = key
}
function submit() {
  if (!canSubmit.value) return
  emit('submit', selected.value)
}
function next() {
  selected.value = null
  emit('next')
}
</script>

<template>
  <div class="card">
    <div class="head">
      <span class="tag">{{ isJudge ? '判断题' : '选择题' }}</span>
      <span class="idx">第 {{ index + 1 }} / {{ total }} 题</span>
      <span class="diff">{{ question.difficulty }}</span>
    </div>

    <p class="stem">{{ question.stem }}</p>

    <div class="options">
      <button
        v-for="opt in optionLabels"
        :key="opt.key"
        class="opt"
        :class="{
          active: selected === opt.key && !result,
          correct: result && opt.key === result.correctAnswer,
          wrong: result && selected === opt.key && opt.key !== result.correctAnswer
        }"
        :disabled="!!result || disabled"
        @click="choose(opt.key)"
      >{{ opt.text }}</button>
    </div>

    <!-- 未答题 -->
    <div v-if="!result" class="actions">
      <span v-if="disabled" class="hint">气血耗尽，无法作答，请先回血。</span>
      <button v-else class="btn primary" :disabled="!canSubmit" @click="submit">作答</button>
    </div>

    <!-- 已答题：展示解析 -->
    <div v-else class="result">
      <div class="verdict" :class="result.correct ? 'ok' : 'no'">
        {{ result.correct ? '✓ 答对' : '✗ 答错' }}
        <span class="delta">EXP +{{ result.expGain }} · HP {{ result.hpDelta >= 0 ? '+' : '' }}{{ result.hpDelta }}</span>
        <span v-if="result.leveledUp" class="levelup">
          <span class="stamp">突破</span>晋入 {{ result.realm }}
        </span>
      </div>
      <div v-if="result.drop" class="drop">
        <span class="dtag">拾获</span>
        <span class="dname">「{{ result.drop.name }}」</span>
        <span class="dquality">{{ result.drop.rarity }}</span>
        <span class="ddesc">{{ result.drop.description }}</span>
      </div>
      <p v-else-if="result.bagFull" class="drop miss">
        储物空间已满，一件宝物与你擦肩而过……
      </p>
      <div class="explain">
        <div class="etitle">解析</div>
        <p>{{ result.explanation }}</p>
        <div v-if="result.sourceUrl" class="src">
          来源：<a :href="result.sourceUrl" target="_blank" rel="noopener">{{ result.sourceUrl }}</a>
        </div>
      </div>
      <button class="btn primary" @click="next">
        {{ index + 1 >= total ? '完成修炼' : '下一题' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.card {
  position: relative;
  background: rgba(255,255,255,.6); border: 1px solid var(--line); border-radius: 4px; padding: 22px;
  animation: inkIn .5s ease-out both;
}
/* 答题卡的淡墨晕染 */
.card::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(ellipse 60% 40% at 12% 0%,  rgba(31,29,26,.05), transparent 60%),
    radial-gradient(ellipse 50% 40% at 95% 30%, rgba(31,29,26,.035), transparent 60%);
}
.card > * { position: relative; }
.head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.tag { background: var(--seal-soft); color: var(--seal); border-radius: 3px; padding: 2px 10px; font-size: 12px; letter-spacing: 1px; }
.idx { color: var(--ink-light); font-size: 13px; }
.diff { margin-left: auto; color: var(--ink-soft); font-size: 12px; }
.stem { font-size: 18px; line-height: 1.8; margin: 8px 0 18px; letter-spacing: .5px; }
.options { display: flex; flex-direction: column; gap: 10px; }
.opt {
  text-align: left; padding: 12px 14px; border: 1px solid var(--line); border-radius: 4px;
  background: rgba(255,255,255,.7); font-size: 15px; color: var(--ink); transition: all .15s; font-family: inherit;
}
.opt:hover:not(:disabled) { border-color: var(--ink); }
.opt.active { border-color: var(--ink); background: #e6e2d8; font-weight: 600; }
.opt.correct { border-color: var(--ink); background: #e4e1d8; font-weight: 600; }
.opt.correct::after { content: " ✓"; color: var(--ink); }
.opt.wrong { border-color: var(--seal); background: var(--seal-soft); }
.opt.wrong::after { content: " ✗"; color: var(--seal); }
.actions { margin-top: 16px; }
.hint { color: var(--seal); font-size: 13px; }
.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 10px 22px; font-size: 15px; font-weight: 600;
  background: transparent; color: var(--ink); transition: all .2s; }
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover:not(:disabled) { background: #8a312b; border-color: #8a312b; }
.result { margin-top: 16px; }
.verdict { font-weight: 700; font-size: 16px; }
.verdict.ok { color: var(--ink); }
.verdict.no { color: var(--seal); }
.verdict .delta { margin-left: 10px; font-size: 13px; color: var(--ink-soft); font-weight: 500; }
.verdict .levelup {
  margin-left: 10px; color: var(--seal); letter-spacing: 1px;
  display: inline-flex; align-items: center; gap: 8px;
}
/* 突破：朱砂印盖下 */
.stamp {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 2px 8px; border: 2px solid var(--seal); color: var(--seal);
  font-size: 12px; font-weight: 700; letter-spacing: 2px; border-radius: 3px;
  background: rgba(158,59,52,.06); transform: rotate(-4deg);
  box-shadow: 0 0 0 2px rgba(158,59,52,.05), 0 1px 5px rgba(158,59,52,.2);
  animation: sealStamp .7s cubic-bezier(.2,.8,.3,1) both;
}
/* 掉落提示 */
.drop {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  margin: 12px 0; padding: 9px 12px; border-radius: 4px;
  background: rgba(158,59,52,.07); border: 1px dashed var(--seal);
  animation: inkIn .45s ease-out both, dropGlow 1.5s .5s ease-out 2;
}
/* 宝物现世的灵光：一圈朱砂色涟漪 */
@keyframes dropGlow {
  0%   { box-shadow: 0 0 0 0 rgba(158,59,52,.30); }
  70%  { box-shadow: 0 0 0 8px rgba(158,59,52,0); }
  100% { box-shadow: 0 0 0 0 rgba(158,59,52,0); }
}
.drop.miss { display: block; color: var(--ink-light); background: rgba(31,29,26,.04); border-color: var(--line); }
.dtag {
  font-size: 12px; color: #fff; background: var(--seal); border-radius: 2px;
  padding: 2px 7px; letter-spacing: 2px; transform: rotate(-2deg);
}
.dname { font-weight: 700; color: var(--ink); letter-spacing: 1px; }
.dquality { font-size: 12px; color: var(--seal); border: 1px solid var(--seal); border-radius: 2px; padding: 1px 5px; }
.ddesc { flex-basis: 100%; font-size: 12px; color: var(--ink-light); line-height: 1.6; }

.explain { margin: 12px 0; background: rgba(255,255,255,.6); border: 1px dashed var(--line); border-radius: 4px; padding: 12px 14px; }
.etitle { color: var(--seal); font-weight: 700; font-size: 13px; margin-bottom: 4px; letter-spacing: 1px; }
.explain p { margin: 0 0 6px; line-height: 1.8; }
.src { font-size: 12px; color: var(--ink-light); word-break: break-all; }
</style>
