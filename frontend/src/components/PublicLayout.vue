<template>
  <div class="site-shell">
    <header class="top-nav">
      <div class="site-container">
        <div class="top-nav__inner">
          <RouterLink to="/" class="brand">
            <div class="brand__logo">LF</div>
            <div>
              <strong>校园失物招领系统</strong>
              <span>Campus Lost & Found</span>
            </div>
          </RouterLink>

          <nav class="nav-links">
            <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
              <span>{{ item.label }}</span>
              <span v-if="item.badge" class="top-nav__badge">{{ item.badge }}</span>
            </RouterLink>
          </nav>

          <div class="auth-actions">
            <template v-for="item in authItems" :key="item.label">
              <button
                v-if="item.action"
                type="button"
                :class="item.className"
                @click="handleAuthAction(item)"
              >
                {{ item.label }}
              </button>
              <RouterLink v-else :to="item.to" :class="item.className">
                <span>{{ item.label }}</span>
                <span v-if="item.badge" class="top-nav__badge">{{ item.badge }}</span>
              </RouterLink>
            </template>
          </div>
        </div>
      </div>
    </header>

    <main class="page-wrap">
      <div class="site-container">
        <slot />
      </div>
    </main>

    <footer class="footer">
      <div class="site-container">
        <div class="footer-bar">
          校园失物招领系统 | 版权所有 | 使用须知
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuthSession, getAuthSession, onAuthChanged, onUnreadChanged } from '../api/client'
import { getChatSummary } from '../api/chat'
import { getMessageSummary } from '../api/message'

const props = defineProps({
  loggedIn: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const systemUnreadCount = ref(0)
const chatUnreadCount = ref(0)
let timerId = null
let stopAuthListener = null
let stopUnreadListener = null

const isLoggedIn = computed(() => props.loggedIn || Boolean(getAuthSession().token))
const totalUnreadCount = computed(() => systemUnreadCount.value + chatUnreadCount.value)

const navItems = computed(() => {
  const items = [
    { label: '首页', to: '/' },
    { label: '我要寻物', to: '/lost-publish' },
    { label: '我要招领', to: '/found-publish' },
    { label: '物品检索', to: '/search' },
  ]
  if (isLoggedIn.value) {
    items.push({ label: '消息', to: '/messages', badge: totalUnreadCount.value || null })
    items.push({ label: '个人中心', to: '/user-center' })
  } else {
    items.push({ label: '个人中心', to: '/login' })
  }
  return items
})

const authItems = computed(() =>
  isLoggedIn.value
    ? [{ label: '退出登录', action: 'logout', className: 'ghost-button nav-button' }]
    : [
        { label: '登录', to: '/login', className: 'ghost-button nav-button' },
        { label: '注册', to: '/register', className: 'primary-button nav-button' },
      ]
)

async function loadUnreadSummary() {
  if (!isLoggedIn.value) {
    systemUnreadCount.value = 0
    chatUnreadCount.value = 0
    return
  }
  try {
    const [messageSummary, conversationSummary] = await Promise.all([
      getMessageSummary(),
      getChatSummary(),
    ])
    systemUnreadCount.value = messageSummary.unreadCount || 0
    chatUnreadCount.value = conversationSummary.unreadCount || 0
  } catch {
    systemUnreadCount.value = 0
    chatUnreadCount.value = 0
  }
}

function handleAuthAction(item) {
  if (item.action !== 'logout') return
  clearAuthSession()
  router.push('/login')
}

onMounted(() => {
  loadUnreadSummary()
  timerId = window.setInterval(loadUnreadSummary, 20000)
  stopAuthListener = onAuthChanged(() => {
    loadUnreadSummary()
  })
  stopUnreadListener = onUnreadChanged(() => {
    loadUnreadSummary()
  })
})

onBeforeUnmount(() => {
  if (timerId) window.clearInterval(timerId)
  if (stopAuthListener) stopAuthListener()
  if (stopUnreadListener) stopUnreadListener()
})
</script>
