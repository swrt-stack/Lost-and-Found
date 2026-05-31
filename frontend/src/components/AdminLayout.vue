<template>
  <div class="admin-page">
    <div class="admin-shell">
      <aside class="admin-sidebar">
        <div class="admin-brand">
          <h2>{{ systemTitle }}</h2>
          <p>{{ subtitle }}</p>
        </div>

        <nav class="side-menu">
          <RouterLink
            v-for="item in menuItems"
            :key="item.to"
            :to="item.to"
            :class="{ active: route.path === item.to }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </aside>

      <section class="admin-content">
        <header class="admin-header">
          <div>
            <h1>{{ title }}</h1>
            <p>{{ adminLabel }}</p>
          </div>

          <button class="ghost-button nav-button" type="button" @click="logout">
            退出登录
          </button>
        </header>

        <slot />
      </section>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { clearAuthSession } from '../api/client'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  systemTitle: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  adminLabel: {
    type: String,
    required: true,
  },
  menuItems: {
    type: Array,
    required: true,
  },
  exitTo: {
    type: String,
    default: '/admin-login',
  },
})

function logout() {
  clearAuthSession()
  router.push(props.exitTo)
}
</script>
