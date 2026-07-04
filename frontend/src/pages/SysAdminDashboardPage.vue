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
          <p>{{ formatStatusPairs(dashboard.statusDistribution) }}</p>
        </div>
      </div>

      <div class="heatmap-box spatiotemporal-panel">
        <div class="spatiotemporal-panel__head">
          <div>
            <strong>校园物品遗失时空分析</strong>
            <p class="spatiotemporal-panel__hint">
              基于已审核遗失记录构建时空序列，调用 Transformer 预测下一高发地点与时段。
            </p>
          </div>
          <button class="ghost-button" type="button" :disabled="spatiotemporalLoading" @click="loadSpatiotemporalAnalysis">
            {{ spatiotemporalLoading ? '分析中…' : '刷新分析' }}
          </button>
        </div>

        <div v-if="spatiotemporalLoading" class="spatiotemporal-panel__status">正在加载时空预测结果…</div>
        <div v-else-if="spatiotemporal.serviceMessage" class="inline-message" :class="spatiotemporal.serviceAvailable ? 'info' : 'warning'">
          {{ spatiotemporal.serviceMessage }}
          <span v-if="spatiotemporal.historySampleSize">
            （样本：{{ spatiotemporal.historySampleSize }} 条已审核遗失记录，序列长度 {{ spatiotemporal.historyLength || 0 }}）
          </span>
        </div>

        <div v-if="!spatiotemporalLoading" class="spatiotemporal-grid">
          <div class="spatiotemporal-card">
            <h3 class="spatiotemporal-card__title">历史地点分布</h3>
            <p v-if="!heatmapRows.length" class="spatiotemporal-card__empty">暂无地点统计数据</p>
            <ul v-else class="heatmap-list">
              <li v-for="row in heatmapRows" :key="row.label" class="heatmap-list__item">
                <div class="heatmap-list__meta">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.count }}</strong>
                </div>
                <div class="heatmap-list__bar">
                  <span :style="{ width: `${row.percent}%` }"></span>
                </div>
              </li>
            </ul>
          </div>

          <div class="spatiotemporal-card">
            <h3 class="spatiotemporal-card__title">预测下一高发地点 Top-K</h3>
            <p v-if="!spatiotemporal.nextLocations.length" class="spatiotemporal-card__empty">暂无预测结果</p>
            <ul v-else class="prediction-list">
              <li v-for="(item, index) in spatiotemporal.nextLocations" :key="`loc-${item.id}-${index}`">
                <span class="prediction-list__rank">{{ index + 1 }}</span>
                <span class="prediction-list__label">{{ item.label }}</span>
                <strong>{{ formatScore(item.score) }}</strong>
              </li>
            </ul>
          </div>

          <div class="spatiotemporal-card">
            <h3 class="spatiotemporal-card__title">预测下一活跃时段 Top-K</h3>
            <p v-if="!spatiotemporal.nextTimeBuckets.length" class="spatiotemporal-card__empty">暂无预测结果</p>
            <ul v-else class="prediction-list">
              <li v-for="(item, index) in spatiotemporal.nextTimeBuckets" :key="`time-${item.id}-${index}`">
                <span class="prediction-list__rank">{{ index + 1 }}</span>
                <span class="prediction-list__label">{{ item.label }}</span>
                <strong>{{ formatScore(item.score) }}</strong>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getDashboard, getSpatiotemporalAnalysis } from '../api/admin'
import { adminMenu } from '../data/mock'
import { safeAlert, statusLabel } from '../utils/ui'

const dashboard = reactive({
  publishedCount: 0,
  retrievalRate: '0%',
  categoryDistribution: {},
  statusDistribution: {},
  typeDistribution: {},
})

const spatiotemporal = reactive({
  serviceAvailable: false,
  serviceMessage: '',
  modelReady: false,
  historyLength: 0,
  historySampleSize: 0,
  locationHeatmap: {},
  nextLocations: [],
  nextTimeBuckets: [],
})

const spatiotemporalLoading = ref(false)

const heatmapRows = computed(() => {
  const entries = Object.entries(spatiotemporal.locationHeatmap || {})
  if (!entries.length) return []
  const max = Math.max(...entries.map(([, count]) => count))
  return entries
    .sort((left, right) => right[1] - left[1])
    .slice(0, 8)
    .map(([label, count]) => ({
      label,
      count,
      percent: max ? Math.round((count / max) * 100) : 0,
    }))
})

function formatPairs(source) {
  const entries = Object.entries(source || {})
  if (!entries.length) return '暂无数据'
  return entries.map(([key, value]) => `${key} ${value}`).join(' / ')
}

function formatStatusPairs(source) {
  const entries = Object.entries(source || {})
  if (!entries.length) return '暂无数据'
  return entries
    .map(([key, value]) => `${statusLabel(key)} ${value}`)
    .join(' / ')
}

function formatScore(score) {
  if (score == null || Number.isNaN(Number(score))) return '-'
  const percent = Number(score) * 100
  if (percent > 0 && percent < 0.05) return '<0.1%'
  return `${percent.toFixed(1)}%`
}

function applySpatiotemporalAnalysis(data) {
  spatiotemporal.serviceAvailable = Boolean(data?.serviceAvailable)
  spatiotemporal.serviceMessage = data?.serviceMessage || ''
  spatiotemporal.modelReady = Boolean(data?.modelReady)
  spatiotemporal.historyLength = data?.historyLength || 0
  spatiotemporal.historySampleSize = data?.historySampleSize || 0
  spatiotemporal.locationHeatmap = data?.locationHeatmap || {}
  spatiotemporal.nextLocations = data?.nextLocations || []
  spatiotemporal.nextTimeBuckets = data?.nextTimeBuckets || []
}

async function loadSpatiotemporalAnalysis() {
  spatiotemporalLoading.value = true
  try {
    applySpatiotemporalAnalysis(await getSpatiotemporalAnalysis(5))
  } catch (error) {
    applySpatiotemporalAnalysis({
      serviceAvailable: false,
      serviceMessage: error.message || '时空预测分析加载失败',
      locationHeatmap: {},
      nextLocations: [],
      nextTimeBuckets: [],
    })
  } finally {
    spatiotemporalLoading.value = false
  }
}

onMounted(async () => {
  try {
    Object.assign(dashboard, await getDashboard())
  } catch (error) {
    safeAlert(error.message)
  }
  await loadSpatiotemporalAnalysis()
})
</script>

<style scoped>
.spatiotemporal-panel {
  display: grid;
  gap: 18px;
  padding: 20px;
  align-content: start;
  min-height: auto;
}

.spatiotemporal-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.spatiotemporal-panel__hint {
  margin: 8px 0 0;
  color: #5f6f7f;
  line-height: 1.6;
}

.spatiotemporal-panel__status {
  color: #5f6f7f;
}

.spatiotemporal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.spatiotemporal-card {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(196, 170, 133, 0.35);
  border-radius: 16px;
  padding: 16px;
  min-height: 220px;
}

.spatiotemporal-card__title {
  margin: 0 0 12px;
  font-size: 15px;
  color: #173854;
}

.spatiotemporal-card__empty {
  margin: 0;
  color: #5f6f7f;
}

.heatmap-list,
.prediction-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.heatmap-list__meta,
.prediction-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.heatmap-list__meta span,
.prediction-list__label {
  color: #4f6072;
}

.heatmap-list__bar {
  margin-top: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(31, 95, 91, 0.12);
  overflow: hidden;
}

.heatmap-list__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1f5f5b, #9f5a22);
}

.prediction-list__rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(159, 90, 34, 0.12);
  color: #9f5a22;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 24px;
}

.prediction-list li {
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(196, 170, 133, 0.35);
}

.prediction-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

@media (max-width: 1100px) {
  .spatiotemporal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
