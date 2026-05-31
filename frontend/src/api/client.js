import axios from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const explicitBackendOrigin = import.meta.env.VITE_BACKEND_ORIGIN
const derivedBackendOrigin = apiBaseUrl.replace(/\/api\/?$/, '')
const isLocalFrontendPort = typeof window !== 'undefined'
  && /:(5173|4173)$/.test(window.location.origin)
const backendOrigin = explicitBackendOrigin || derivedBackendOrigin || (isLocalFrontendPort ? 'http://127.0.0.1:8080' : '')
const AUTH_EVENT = 'auth-changed'
const UNREAD_EVENT = 'unread-changed'

function emitAuthChanged() {
  window.dispatchEvent(new Event(AUTH_EVENT))
}

export function emitUnreadChanged() {
  window.dispatchEvent(new Event(UNREAD_EVENT))
}

export function saveAuthSession(payload) {
  localStorage.setItem('token', payload.token || '')
  localStorage.setItem('role', payload.role || '')
  localStorage.setItem('username', payload.username || '')
  emitAuthChanged()
}

export function clearAuthSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('username')
  emitAuthChanged()
}

export function getAuthSession() {
  return {
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    username: localStorage.getItem('username') || '',
  }
}

export function onAuthChanged(handler) {
  window.addEventListener(AUTH_EVENT, handler)
  return () => window.removeEventListener(AUTH_EVENT, handler)
}

export function onUnreadChanged(handler) {
  window.addEventListener(UNREAD_EVENT, handler)
  return () => window.removeEventListener(UNREAD_EVENT, handler)
}

const client = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const payload = response?.data
    if (!payload || typeof payload !== 'object' || !('code' in payload)) {
      return payload
    }
    if (payload.code !== 200) {
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return payload.data
  },
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error?.message || 'Request failed'
    if (status === 401) {
      clearAuthSession()
      if (window.location.pathname !== '/login') {
        const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
        window.location.replace(`/login?redirect=${redirect}`)
      }
    }
    return Promise.reject(new Error(message))
  }
)

export function toBackendAssetUrl(path) {
  if (!path) {
    return ''
  }
  if (path.startsWith('/api/')) {
    return path
  }
  return path.startsWith('http://') || path.startsWith('https://')
    ? path
    : `${backendOrigin}${path.startsWith('/') ? path : `/${path}`}`
}

export default client
