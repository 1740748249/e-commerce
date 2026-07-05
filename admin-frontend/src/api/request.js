const BASE = '/api'

const pending = new Map()
const cache = new Map()

function getToken() {
  return localStorage.getItem('admin_token') || ''
}

function cacheKey(url, options) {
  return `${options.method || 'GET'}:${url}:${options.body || ''}`
}

export function clearCache(prefix = '') {
  for (const k of cache.keys()) {
    if (k.startsWith(prefix)) cache.delete(k)
  }
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

  const method = config.method || 'GET'

  // Strip custom props before passing to native fetch (cache boolean is invalid per Fetch API spec)
  const fetchOptions = { ...config }
  delete fetchOptions.cache
  delete fetchOptions.cacheTTL

  // Cache read (GET only, unless force skip)
  if (method === 'GET' && config.cache !== false) {
    const ck = cacheKey(url, config)
    const entry = cache.get(ck)
    if (entry && Date.now() - entry.ts < (config.cacheTTL || 30000)) {
      return entry.data
    }
  }

  // Dedup in-flight requests
  const dedupKey = cacheKey(url, config)
  if (method === 'GET' && pending.has(dedupKey)) {
    return pending.get(dedupKey)
  }

  const promise = (async () => {
    let res
    try {
      res = await fetch(BASE + url, fetchOptions)
    } catch {
      throw new Error('网络连接失败，请检查网络')
    }

    if (res.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      throw new Error('登录已过期，请重新登录')
    }

    const json = await res.json().catch(() => null)
    if (!json) throw new Error('服务器响应异常')
    if (json.code !== 200) throw new Error(json.message || '请求失败')
    return json.data
  })()

  if (method === 'GET') {
    pending.set(dedupKey, promise)
  }

  try {
    const data = await promise
    // Cache successful GET responses
    if (method === 'GET' && config.cache !== false) {
      const ck = cacheKey(url, config)
      cache.set(ck, { data, ts: Date.now() })
    }
    return data
  } finally {
    pending.delete(dedupKey)
  }
}

export function get(url, params, opts = {}) {
  const qs = params ? '?' + new URLSearchParams(params).toString() : ''
  return request(url + qs, { ...opts })
}

export function post(url, data, opts = {}) {
  return request(url, { method: 'POST', body: data, ...opts })
}

export function put(url, data, params, opts = {}) {
  const qs = params ? '?' + new URLSearchParams(params).toString() : ''
  return request(url + qs, { method: 'PUT', body: data, ...opts })
}

export function del(url, opts = {}) {
  return request(url, { method: 'DELETE', ...opts })
}
