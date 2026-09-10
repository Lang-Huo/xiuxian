<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'

const bag = ref(null)
const loading = ref(true)
const error = ref('')
const notice = ref('')          // 操作反馈
const selectedId = ref(null)    // 选中的格子

const emit = defineEmits(['changed'])

async function load() {
  loading.value = true
  error.value = ''
  try {
    bag.value = await api.bag()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

const items = computed(() => bag.value ? bag.value.items : [])
const selected = computed(() => items.value.find(i => i.id === selectedId.value) || null)

// 空格补足到总容量，让「还能装几格」一眼可见
const slots = computed(() => {
  if (!bag.value) return []
  const filled = items.value.map(i => ({ item: i, empty: false }))
  const pad = Math.max(0, bag.value.capacity - filled.length)
  return filled.concat(Array.from({ length: pad }, () => ({ item: null, empty: true })))
})

const usedPct = computed(() => {
  if (!bag.value || bag.value.capacity === 0) return '0%'
  return Math.min(100, bag.value.used / bag.value.capacity * 100) + '%'
})

function pick(cell) {
  if (cell.empty) return
  selectedId.value = selectedId.value === cell.item.id ? null : cell.item.id
}

async function act(fn) {
  notice.value = ''
  error.value = ''
  try {
    const res = await fn()
    bag.value = res.bag
    notice.value = res.message
    emit('changed')                      // 气血/容量可能变了，通知外层刷新状态
  } catch (e) {
    error.value = e.message
  }
}

const equip = (item) => act(() => api.bagEquip(item.id))
const unequip = (item) => act(() => api.bagUnequip(item.id))
const use = (item) => act(() => api.bagUse(item.id))
const discard = (item) => {
  if (!confirm(`确定丢弃「${item.name}」？此操作不可撤销。`)) return
  act(() => api.bagDiscard(item.id, 1))
}

const isPill = (i) => i && i.type === 'PILL'
const isEquip = (i) => i && i.slot

function effectText(i) {
  const parts = []
  if (i.bagBonus > 0) parts.push(`储物格数 +${i.bagBonus}`)
  if (i.expBonus > 0) parts.push(`道行 +${i.expBonus}%`)
  if (i.hpRestore > 0) parts.push(`服用回血 +${i.hpRestore}`)
  return parts.length ? parts.join(' · ') : '暂无附加之效'
}
</script>

<template>
  <div class="bag">
    <p v-if="loading" class="tip">正在打开储物法宝…</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="bag">
      <!-- 容量 -->
      <section class="panel">
        <div class="cap-head">
          <h3>储物空间</h3>
          <span class="cap-num">{{ bag.used }} / {{ bag.capacity }}</span>
        </div>
        <div class="bar"><div class="fill" :style="{ width: usedPct }"></div></div>
        <div class="cap-detail">
          <span>{{ bag.realm }} 境界基础 {{ bag.baseSlots }} 格</span>
          <span v-if="bag.bonusSlots > 0" class="bonus">装备加成 +{{ bag.bonusSlots }} 格</span>
          <span v-else class="muted">尚无储物法宝加成</span>
        </div>
        <div class="cap-detail next" v-if="bag.nextRealm">
          晋入 {{ bag.nextRealm }} 后，基础格数增至 {{ bag.nextRealmSlots }} 格
        </div>
      </section>

      <!-- 装备槽 -->
      <section class="panel">
        <h3>已佩戴</h3>
        <div class="slots">
          <div v-for="s in bag.slots" :key="s.code" class="slot" :class="{ filled: s.item }">
            <div class="slot-label">{{ s.label }}</div>
            <div class="slot-body">
              <template v-if="s.item">
                <span class="slot-name" :class="'r-' + s.item.rarity">{{ s.item.name }}</span>
                <button class="mini" @click="unequip(s.item)">卸下</button>
              </template>
              <span v-else class="slot-empty">空</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 背包格子 -->
      <section class="panel">
        <h3>行囊</h3>
        <p v-if="items.length === 0" class="tip">行囊空空如也，去「修炼」答对几题，或许能拾获机缘。</p>
        <div v-else class="grid">
          <div
            v-for="(cell, i) in slots"
            :key="i"
            class="cell"
            :class="{ empty: cell.empty, active: !cell.empty && cell.item.id === selectedId, worn: !cell.empty && cell.item.equipped }"
            @click="pick(cell)"
          >
            <template v-if="!cell.empty">
              <span class="glyph" :class="'r-' + cell.item.rarity">{{ cell.item.name.slice(0, 1) }}</span>
              <span v-if="cell.item.count > 1" class="badge">{{ cell.item.count }}</span>
              <span v-if="cell.item.equipped" class="worn-dot">佩</span>
            </template>
          </div>
        </div>
      </section>

      <!-- 选中详情 -->
      <section v-if="selected" class="panel detail">
        <div class="d-head">
          <span class="glyph big" :class="'r-' + selected.rarity">{{ selected.name.slice(0, 1) }}</span>
          <div>
            <div class="d-name">
              {{ selected.name }}
              <span class="rarity" :class="'r-' + selected.rarity">{{ selected.rarity }}</span>
            </div>
            <div class="d-sub">
              {{ selected.slotLabel || '不可装备' }}<template v-if="selected.count > 1"> · 持有 {{ selected.count }}</template>
            </div>
          </div>
        </div>
        <p class="d-desc">{{ selected.description }}</p>
        <div class="d-effect">{{ effectText(selected) }}</div>
        <div class="d-actions">
          <button v-if="isPill(selected)" class="btn primary" @click="use(selected)">服用</button>
          <button v-if="isEquip(selected) && !selected.equipped" class="btn" @click="equip(selected)">装备</button>
          <button v-if="isEquip(selected) && selected.equipped" class="btn" @click="unequip(selected)">卸下</button>
          <button class="btn danger" @click="discard(selected)">丢弃</button>
        </div>
      </section>

      <p v-if="notice" class="notice">{{ notice }}</p>
      <button class="btn refresh" @click="load">整理行囊</button>
    </template>
  </div>
</template>

<style scoped>
.bag { animation: inkIn .5s ease-out both; }
.tip { text-align: center; color: var(--ink-light); font-size: 14px; padding: 18px 0; }
.error { color: var(--seal); text-align: center; margin-top: 16px; font-size: 14px; }
.notice {
  text-align: center; color: var(--ink); font-size: 14px; letter-spacing: 1px;
  background: rgba(158,59,52,.08); border: 1px dashed var(--seal);
  border-radius: 4px; padding: 10px; margin-bottom: 14px;
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

/* 容量 */
.cap-head { display: flex; justify-content: space-between; align-items: baseline; }
.cap-head h3 { margin-bottom: 10px; }
.cap-num { font-size: 18px; font-weight: 700; color: var(--ink); font-variant-numeric: tabular-nums; }
.bar { height: 8px; border-radius: 2px; background: #e3ded2; overflow: hidden; }
.bar .fill { height: 100%; background: linear-gradient(90deg, #6b665d, var(--ink)); transition: width .4s ease; }
.cap-detail {
  display: flex; gap: 14px; flex-wrap: wrap;
  color: var(--ink-soft); font-size: 12px; margin-top: 10px; letter-spacing: 1px;
}
.cap-detail .bonus { color: var(--seal); font-weight: 600; }
.cap-detail .muted { color: var(--ink-light); }
.cap-detail.next { margin-top: 4px; color: var(--ink-light); }

/* 装备槽 */
.slots { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.slot {
  border: 1px dashed var(--line); border-radius: 4px; padding: 10px 12px;
  background: rgba(255,255,255,.4); transition: border-color .2s;
}
.slot.filled { border-style: solid; border-color: var(--ink-soft); }
.slot-label { font-size: 12px; color: var(--ink-light); letter-spacing: 2px; }
.slot-body { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 4px; }
.slot-name { font-size: 14px; font-weight: 600; }
.slot-empty { font-size: 13px; color: var(--ink-light); }
.mini {
  border: 1px solid var(--line); background: transparent; color: var(--ink-light);
  border-radius: 3px; font-size: 12px; padding: 2px 8px; font-family: inherit; transition: all .2s;
}
.mini:hover { border-color: var(--ink); color: var(--ink); }

/* 格子 */
.grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.cell {
  position: relative; aspect-ratio: 1; border: 1px solid var(--line); border-radius: 4px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: rgba(255,255,255,.5); transition: all .18s;
}
.cell.empty { background: rgba(31,29,26,.03); border-style: dashed; cursor: default; }
.cell:hover:not(.empty) { border-color: var(--ink-soft); transform: translateY(-2px); }
.cell.active { border-color: var(--seal); box-shadow: 0 0 0 2px rgba(158,59,52,.12); }
.cell.worn { background: rgba(158,59,52,.06); }
.glyph { font-size: 20px; font-weight: 700; }
.badge {
  position: absolute; right: 2px; bottom: 1px; font-size: 11px; font-weight: 700;
  color: var(--ink); font-variant-numeric: tabular-nums;
}
.worn-dot {
  position: absolute; left: 3px; top: 2px; font-size: 10px; color: #fff;
  background: var(--seal); border-radius: 2px; padding: 0 3px;
}

/* 详情 */
.detail { margin-top: 4px; }
.d-head { display: flex; align-items: center; gap: 14px; }
.glyph.big {
  width: 46px; height: 46px; flex: none; display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--ink-soft); border-radius: 4px; font-size: 22px; transform: rotate(-3deg);
}
.d-name { font-size: 17px; font-weight: 700; letter-spacing: 2px; }
.d-sub { font-size: 12px; color: var(--ink-light); margin-top: 4px; letter-spacing: 1px; }
.d-desc { color: var(--ink-soft); font-size: 13px; line-height: 1.7; margin: 14px 0 8px; }
.d-effect { color: var(--seal); font-size: 13px; letter-spacing: 1px; font-weight: 600; }
.d-actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }

.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 9px 18px; font-size: 14px;
  background: transparent; color: var(--ink); font-family: inherit; transition: all .2s; }
.btn:hover { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover { background: #8a312b; border-color: #8a312b; }
.btn.danger { border-color: var(--ink-light); color: var(--ink-light); }
.btn.danger:hover { background: var(--ink-light); color: #fff; }
.refresh { display: block; margin: 0 auto; letter-spacing: 2px; }

/* 品阶配色（水墨淡雅，仅以墨色深浅与朱砂区分） */
.rarity { font-size: 12px; margin-left: 6px; padding: 1px 6px; border-radius: 2px; border: 1px solid currentColor; }
.r-凡品 { color: #6b665d; }
.r-灵品 { color: #4a7c6f; }
.r-宝品 { color: #3f6a9e; }
.r-仙品 { color: #7a5aa0; }
.r-神品 { color: var(--seal); }

@media (max-width: 520px) {
  .slots { grid-template-columns: 1fr; }
  .grid { grid-template-columns: repeat(5, 1fr); }
}
</style>
