<template>
  <AdminLayout
    title="超级管理员后台首页"
    system-title="系统后台管理中心"
    subtitle="查看发布总量、审核通过率和分类状态结构"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-stats">
      <div class="stat-card">
        <span>物品总数</span>
        <strong>{{ dashboard.publishedCount }}</strong>
      </div>
      <div class="stat-card">
        <span>审核通过率</span>
        <strong>{{ dashboard.retrievalRate }}</strong>
      </div>
      <div class="stat-card">
        <span>遗失物品</span>
        <strong>{{ dashboard.typeDistribution.lost || 0 }}</strong>
      </div>
      <div class="stat-card">
        <span>招领物品</span>
        <strong>{{ dashboard.typeDistribution.found || 0 }}</strong>
      </div>
    </section>

    <section class="admin-main-card">
      <h2 class="section-title">图表区域</h2>
      <div class="chart-grid">
        <div class="chart-box">
          <strong>物品分类占比</strong>
          <p>{{ formatPairs(dashboard.categoryDistribution) }}</p>
        </div>
        <div class="chart-box">
          <strong>状态分布</strong>
          <p>{{ formatPairs(dashboard.statusDistribution) }}</p>
        </div>
      </div>

      <div class="heatmap-box" style="margin-top: 18px;">
        <strong>校园物品遗失时空分布热力图</strong>
        <p style="margin: 8px 0 0; color: #5f6f7f;">
          当前先展示后台占位说明。若后续接入时空预测接口，可在这里替换成真实热力图组件。
        </p>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getDashboard } from '../api/admin'
import { adminMenu } from '../data/mock'
import { safeAlert } from '../utils/ui'

const dashboard = reactive({
  publishedCount: 0,
  retrievalRate: '0%',
  categoryDistribution: {},
  statusDistribution: {},
  typeDistribution: {},
})

function formatPairs(source) {
  const entries = Object.entries(source || {})
  if (!entries.length) return '暂无数据'
  return entries.map(([key, value]) => `${key} ${value}`).join(' / ')
}

onMounted(async () => {
  try {
    Object.assign(dashboard, await getDashboard())
  } catch (error) {
    safeAlert(error.message)
  }
})
</script>
