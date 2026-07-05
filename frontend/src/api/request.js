import router from '@/router'

const BASE = 'http://localhost:8080'

function getToken() {
  return localStorage.getItem('token') || ''
}

function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

async function request(url, options = {}) {
  const config = {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  }
  const token = getToken()
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  if (config.body instanceof FormData) {
    delete config.headers['Content-Type']
  } else if (config.body && typeof config.body === 'object') {
    config.body = JSON.stringify(config.body)
  }

  let res
  try {
    res = await fetch(BASE + url, config)
  } catch {
    throw new Error('网络连接失败，请检查网络')
  }

  if (res.status === 401) {
    clearAuth()
    if (router.currentRoute.value.path !== '/login') {
      router.replace('/login')
    }
    throw new Error('登录已过期，请重新登录')
  }

  const json = await res.json().catch(() => null)
  if (!json) {
    throw new Error('服务器响应异常')
  }
  if (json.code !== 200) {
    throw new Error(json.message || '请求失败')
  }
  return json.data
}

export function get(url, params) {
  const qs = params ? '?' + new URLSearchParams(params).toString() : ''
  return request(url + qs)
}

export function post(url, data) {
  return request(url, { method: 'POST', body: data })
}

export function put(url, data, params) {
  const qs = params ? '?' + new URLSearchParams(params).toString() : ''
  return request(url + qs, { method: 'PUT', body: data })
}

export function del(url) {
  return request(url, { method: 'DELETE' })
}

export function upload(url, file, type) {
  const form = new FormData()
  form.append('file', file)
  form.append('type', type)
  return request(url, { method: 'POST', body: form, headers: {} })
}
