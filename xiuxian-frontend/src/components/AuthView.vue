<script setup>
import { ref } from 'vue'
import { api } from '../api'

const emit = defineEmits(['success'])

const PHONE_RE = /^1[3-9]\d{9}$/   // 用户名即手机号

const mode = ref('login')          // login | register
const username = ref('')           // 登录账号 = 手机号（唯一）
const nickname = ref('')           // 展示昵称（可重复，仅注册时填写）
const password = ref('')
const error = ref('')
const loading = ref(false)

/** 手机号输入框：仅允许数字，最长 11 位 */
function onPhoneInput(e) {
  username.value = e.target.value.replace(/\D/g, '').slice(0, 11)
}

async function submit() {
  error.value = ''
  if (!PHONE_RE.test(username.value)) {
    error.value = '请输入正确的 11 位手机号'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  if (mode.value === 'register' && !nickname.value.trim()) {
    error.value = '请输入昵称'
    return
  }
  loading.value = true
  try {
    const resp = mode.value === 'login'
      ? await api.auth.login(username.value.trim(), password.value)
      : await api.auth.register(username.value.trim(), nickname.value.trim(), password.value)
    emit('success', resp)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function toggle() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  error.value = ''
}
</script>

<template>
  <div class="auth">
    <h2>{{ mode === 'login' ? '入道 · 登录' : '开宗 · 注册' }}</h2>
    <p class="tip">手机号即登录账号，昵称用于展示；密码至少 6 位</p>

    <div class="field">
      <label>道号</label>
      <input
        v-model="username"
        type="tel"
        inputmode="numeric"
        maxlength="11"
        autocomplete="tel"
        placeholder="请输入道号(手机号)"
        @input="onPhoneInput"
        @keyup.enter="submit"
      />
    </div>
    <div v-if="mode === 'register'" class="field">
      <label>昵称</label>
      <input v-model="nickname" placeholder="修行名号" @keyup.enter="submit" />
    </div>
    <div class="field">
      <label>密码</label>
      <input type="password" v-model="password" placeholder="设置或输入密码" @keyup.enter="submit" />
    </div>

    <button class="btn primary big" :disabled="loading" @click="submit">
      {{ loading ? '施法中…' : (mode === 'login' ? '登 录' : '注 册并入门') }}
    </button>

    <p class="switch">
      {{ mode === 'login' ? '尚未入道？' : '已有道号？' }}
      <a @click="toggle">{{ mode === 'login' ? '立即注册' : '去登录' }}</a>
    </p>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.auth {
  max-width: 360px; margin: 8vh auto 0; background: rgba(255,255,255,.6); border: 1px solid var(--line);
  border-radius: 4px; padding: 30px 26px; box-shadow: 0 4px 24px rgba(31,29,26,.07);
  position: relative; overflow: hidden; animation: inkIn .7s ease-out both;
}
/* 登录卡上的淡墨晕染 */
.auth::before {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background:
    radial-gradient(ellipse 70% 40% at 10% 0%,   rgba(31,29,26,.06), transparent 60%),
    radial-gradient(circle at 88% 96%,            rgba(158,59,52,.05), transparent 45%);
}
.auth > * { position: relative; }
h2 {
  text-align: center; color: var(--ink); letter-spacing: 4px; margin: 0 0 6px;
  position: relative; padding-bottom: 12px;
}
h2::after {
  content: ""; position: absolute; left: 12%; right: 12%; bottom: 0; height: 3px;
  border-radius: 50%; transform-origin: left center;
  background: linear-gradient(90deg,
    transparent, rgba(31,29,26,.5) 10%, rgba(31,29,26,.18) 48%, rgba(31,29,26,.5) 84%, transparent);
  filter: blur(.4px);
  animation: brushSweep .85s .2s ease-out both;
}
.tip { text-align: center; color: var(--ink-light); font-size: 12px; margin: 0 0 18px; }
.field { margin-bottom: 14px; }
label { display: block; font-size: 13px; color: var(--ink-soft); margin-bottom: 6px; letter-spacing: 1px; }
input { width: 100%; padding: 11px 12px; border: 1px solid var(--line); border-radius: 4px;
  font-size: 15px; font-family: inherit; background: rgba(255,255,255,.7); color: var(--ink); transition: border-color .2s; }
input:focus { outline: none; border-color: var(--ink); }
.btn { border: 1px solid var(--ink); border-radius: 4px; padding: 10px 22px; font-size: 15px; font-weight: 600;
  background: transparent; color: var(--ink); transition: all .2s; }
.btn:hover:not(:disabled) { background: var(--ink); color: #fff; }
.btn.primary { background: var(--seal); border-color: var(--seal); color: #fff; }
.btn.primary:hover:not(:disabled) { background: #8a312b; border-color: #8a312b; }
.btn.big { width: 100%; padding: 13px; font-size: 16px; margin-top: 4px; letter-spacing: 3px; }
.switch { text-align: center; color: var(--ink-light); font-size: 13px; margin: 16px 0 0; }
.switch a { color: var(--seal); cursor: pointer; font-weight: 600; }
.error { color: var(--seal); text-align: center; margin-top: 14px; font-size: 14px; }
</style>
