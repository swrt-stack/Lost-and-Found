<template>
  <AdminLayout
    title="审核管理员首页工作台"
    system-title="审核管理员工作台"
    subtitle="聚焦待审核任务，不开放用户和系统管理权限"
    admin-label="管理员：审核账号"
    :menu-items="reviewMenu"
  >
    <section class="split-layout">
      <aside class="side-menu">
        <RouterLink v-for="item in reviewMenu" :key="item.to" :to="item.to" :class="{ active: item.to === '/review-pending' }">
          {{ item.label }}
        </RouterLink>
      </aside>

      <section class="admin-main-card">
        <h2 class="section-title">数据概览</h2>
        <div class="admin-stats">
          <div class="stat-card">
            <span>待审核总数</span>
            <strong>{{ reviews.length }}</strong>
          </div>
          <div class="stat-card">
            <span>今日新增待审核</span>
            <strong>{{ todayCount }}</strong>
          </div>
        </div>

        <div class="notice-bar" style="margin-top: 22px;">
          温馨提示：当前账号仅可进行物品审核操作，不包含用户管理、数据统计深度分析和系统设置权限。
        </div>
      </section>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getReviews } from '../api/admin'
import { reviewMenu } from '../data/mock'
import { normalizeReviewItem } from '../utils/items'
import { safeAlert } from '../utils/ui'

const reviews = ref([])

const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return reviews.value.filter((item) => String(item.time || '').startsWith(today)).length
})

onMounted(async () => {
  try {
    reviews.value = (await getReviews()).map(normalizeReviewItem)
  } catch (error) {
    safeAlert(error.message)
  }
})
</script>
