<script setup>
defineProps({
  state: { type: Object, required: true }
})
</script>

<template>
  <div class="status">
    <div class="realm">
      <span class="badge">{{ state.realm }}</span>
      <span class="layer">第 {{ state.layer }} 层</span>
    </div>

    <div class="row">
      <span class="label">道行（EXP）</span>
      <span class="val">{{ state.exp }}</span>
    </div>
    <div class="bar exp">
      <div class="fill" :style="{ width: (state.progress * 100).toFixed(1) + '%' }"></div>
    </div>

    <div class="row">
      <span class="label">气血（HP）</span>
      <span class="val">{{ state.hp }} / {{ state.maxHp }}</span>
    </div>
    <div class="bar hp">
      <div class="fill" :style="{ width: ((state.hp / state.maxHp) * 100).toFixed(1) + '%' }"></div>
    </div>

    <div class="row small">
      <span>作答 {{ state.answerCount }} 题</span>
      <span>正确率 {{ (state.accuracy * 100).toFixed(0) }}%</span>
    </div>

    <div v-if="!state.canAnswer" class="warn">
      ⚠ 气血耗尽，暂不能答题，请打坐疗伤（答对可回血）。
    </div>
  </div>
</template>

<style scoped>
.status {
  position: relative; overflow: hidden;
  background: rgba(255,255,255,.55);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 18px 20px;
  box-shadow: 0 2px 14px rgba(31,29,26,.06);
  animation: inkIn .5s ease-out both;
}
/* 状态卡的淡墨晕染 */
.status::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(ellipse 65% 45% at 8% 0%,   rgba(31,29,26,.055), transparent 60%),
    radial-gradient(ellipse 45% 40% at 96% 25%, rgba(31,29,26,.035), transparent 60%);
}
.status > * { position: relative; }
.realm { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.badge {
  background: var(--ink); color: #fff; font-weight: 700; padding: 5px 16px;
  border-radius: 3px; letter-spacing: 2px;
  transform: rotate(-1.5deg);                       /* 印章般略微倾斜 */
  box-shadow: 0 0 0 2px rgba(31,29,26,.06), 0 1px 6px rgba(31,29,26,.22);
}
.layer { color: var(--ink-light); font-size: 13px; }
.row { display: flex; justify-content: space-between; font-size: 13px; margin: 8px 0 4px; }
.row.small { color: var(--ink-light); font-size: 12px; }
.label { color: var(--ink-soft); letter-spacing: 1px; }
.val { font-weight: 600; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; }
.bar .fill { height: 100%; border-radius: 2px; transition: width .4s ease; }
.bar.exp .fill { background: linear-gradient(90deg, #6b665d, var(--ink)); }
.bar.hp .fill { background: linear-gradient(90deg, #c4897f, var(--seal)); }
.warn { margin-top: 10px; color: var(--seal); font-size: 13px; font-weight: 600; }
</style>
