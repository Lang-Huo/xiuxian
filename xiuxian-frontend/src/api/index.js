import { ref } from 'vue'

const BASE = '/api'
const TOKEN_KEY = 'xiuxian_token'

// 登录态：从 localStorage 恢复，保证刷新页面仍保持登录
export const token = ref(localStorage.getItem(TOKEN_KEY) || '')

export function setToken(t) {
  token.value = t
  localStorage.setItem(TOKEN_KEY, t)
}

export function clearToken() {
  token.value = ''
  localStorage.removeItem(TOKEN_KEY)
}

// 统一的 fetch 封装：自动附带令牌；令牌失效时清除登录态
async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token.value) headers['Authorization'] = 'Bearer ' + token.value
  const opts = { method, headers }
  if (body) opts.body = JSON.stringify(body)
  const res = await fetch(BASE + path, opts)
  if (res.status === 401) clearToken()
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || ('请求失败(' + res.status + ')'))
  }
  return res.json()
}

export const api = {
  auth: {
    register: (username, nickname, password) =>
      request('POST', '/auth/register', { username, nickname, password }),
    login: (username, password) => request('POST', '/auth/login', { username, password }),
    me: () => request('POST', '/auth/me')
  },
  me: () => request('POST', '/users/me'),
  profile: () => request('GET', '/users/profile'),
  generate: (topic, difficulty, count) =>
    request('POST', '/topics/generate', { topic, difficulty, count }),
  answer: (questionId, userAnswer) =>
    request('POST', '/answer', { questionId, userAnswer }),
  avatars: () => request('GET', '/avatars'),
  setAvatar: (code) => request('POST', '/users/avatar', { code }),
  bag: () => request('GET', '/bag'),
  bagEquip: (itemId) => request('POST', '/bag/equip', { itemId }),
  bagUnequip: (itemId) => request('POST', '/bag/unequip', { itemId }),
  bagUse: (itemId) => request('POST', '/bag/use', { itemId }),
  bagDiscard: (itemId, count) => request('POST', '/bag/discard', { itemId, count })
}
