<template>
  <PublicLayout>
    <section class="table-card">
      <h1 class="page-heading">物品智能检索平台</h1>

      <div class="search-page-grid">
        <div class="content-card">
          <h2 class="section-title">方式一：关键词文字检索</h2>
          <div class="search-bar" style="grid-template-columns: 1fr auto;">
            <input v-model="filters.keyword" class="field" placeholder="请输入关键词" />
            <button class="primary-button" type="button" @click="runTextSearch">搜索</button>
          </div>
          <div class="filter-row" style="margin-top: 18px;">
            <select v-model="filters.categoryId" class="select-field">
              <option value="">分类筛选</option>
              <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
            <input v-model="filters.location" class="field" placeholder="地点筛选" />
            <select v-model="filters.type" class="select-field">
              <option value="">类型筛选</option>
              <option value="lost">遗失物品</option>
              <option value="found">招领物品</option>
            </select>
            <select v-model="filters.status" class="select-field">
              <option value="">物品状态筛选</option>
              <option value="APPROVED">已通过</option>
              <option value="COMPLETED">已完成</option>
            </select>
          </div>
        </div>

        <div class="content-card">
          <h2 class="section-title">方式二：以图搜线索</h2>
          <div class="dual-grid">
            <div class="form-row">
              <label>上传物品图片</label>
              <input class="field" type="file" accept="image/*" @change="handleImageChange" />
              <button class="primary-button" style="margin-top: 12px; width: fit-content;" type="button" :disabled="searching" @click="runImageSearch">
                {{ searching ? '检索中，请稍候…' : '开始智能检索' }}
              </button>
            </div>
            <div class="preview-box" style="min-height: 180px;">
              <img v-if="previewUrl" :src="previewUrl" alt="图片预览" style="width: 100%; height: 180px; object-fit: cover; border-radius: 14px;" />
              <span v-else>图片预览区</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-card" style="margin-top: 22px;">
        <h2 class="section-title">检索结果列表</h2>
        <table class="results-table">
          <thead>
            <tr>
              <th>图片</th>
              <th>物品名称</th>
              <th>发布人</th>
              <th>分类</th>
              <th>地点</th>
              <th>时间</th>
              <th>相似度</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!pagedResults.length">
              <td colspan="9" class="empty-cell">暂无检索结果</td>
            </tr>
            <tr v-for="item in pagedResults" :key="item.id">
              <td>
                <div class="thumb">
                  <img v-if="item.imageUrl" :src="toBackendAssetUrl(item.imageUrl)" :alt="item.title" style="width: 54px; height: 54px; object-fit: cover; border-radius: 12px;" />
                  <span v-else>图</span>
                </div>
              </td>
              <td>{{ item.title }}</td>
              <td>
                <div class="publisher-cell">
                  <div class="publisher-cell__avatar">
                    <img
                      v-if="item.publisherAvatarUrl"
                      :src="toBackendAssetUrl(item.publisherAvatarUrl)"
                      :alt="item.publisher"
                    />
                    <span v-else>{{ avatarText(item.publisher) }}</span>
                  </div>
                  <span>{{ item.publisher }}</span>
                </div>
              </td>
              <td>{{ item.category }}</td>
              <td>{{ item.location }}</td>
              <td>{{ item.time }}</td>
              <td>{{ item.similarity }}</td>
              <td><span class="status-pill" :class="item.statusClass">{{ item.statusLabel }}</span></td>
              <td><RouterLink class="table-action" :to="{ path: '/item-detail', query: { id: item.id } }">查看详情</RouterLink></td>
            </tr>
          </tbody>
        </table>
        <div class="pagination" style="justify-content: flex-end; margin-top: 18px;">
          <button type="button" @click="changePage(-1)">上一页</button>
          <button type="button">{{ page }}</button>
          <button type="button" @click="changePage(1)">下一页</button>
        </div>
      </div>
    </section>
  </PublicLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { toBackendAssetUrl } from '../api/client'
import { getCategories } from '../api/category'
import { smartMatch } from '../api/ai'
import { searchItems } from '../api/item'
import { normalizeItem } from '../utils/items'
import { safeAlert, statusClass, statusLabel } from '../utils/ui'

const route = useRoute()
const categories = ref([])
const previewUrl = ref('')
const uploadFile = ref(null)
const searching = ref(false)
const results = ref([])
const page = ref(1)
const pageSize = 8
const filters = reactive({
  keyword: '',
  categoryId: '',
  location: '',
  type: '',
  status: '',
})

const filteredResults = computed(() => {
  if (!filters.status) return results.value
  return results.value.filter((item) => item.status === filters.status)
})

const pagedResults = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredResults.value.slice(start, start + pageSize)
})

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function handleImageChange(event) {
  uploadFile.value = event.target.files?.[0] || null
  previewUrl.value = uploadFile.value ? URL.createObjectURL(uploadFile.value) : ''
}

function changePage(step) {
  const next = page.value + step
  const maxPage = Math.max(1, Math.ceil(filteredResults.value.length / pageSize))
  page.value = Math.min(maxPage, Math.max(1, next))
}

async function runTextSearch() {
  try {
    const rows = await searchItems({
      keyword: filters.keyword || undefined,
      location: filters.location || undefined,
      type: filters.type || undefined,
      categoryId: filters.categoryId || undefined,
    })
    results.value = rows.map((item) => ({
      ...normalizeItem(item),
      similarity: '-',
    }))
    page.value = 1
  } catch (error) {
    safeAlert(error.message)
  }
}

function parseCategory(title) {
  const match = String(title || '').match(/\[(.+?)\]\s*$/)
  return match ? match[1] : '-'
}

async function runImageSearch() {
  if (!uploadFile.value) {
    safeAlert('请先选择图片')
    return
  }
  searching.value = true
  try {
    const response = await smartMatch({ file: uploadFile.value, description: filters.keyword, topK: 10 })
    results.value = (response.results || []).map((item) => ({
      id: item.itemId,
      title: item.title,
      publisher: item.publisher || '-',
      publisherAvatarUrl: '',
      category: parseCategory(item.title),
      location: item.location || '-',
      time: item.time || '-',
      similarity: item.finalScore ? `${Math.round(item.finalScore * 100)}%` : '-',
      status: 'APPROVED',
      statusLabel: statusLabel('APPROVED'),
      statusClass: statusClass('APPROVED'),
      imageUrl: item.imageUrl || '',
    }))
    page.value = 1
  } catch (error) {
    safeAlert(error.message)
  } finally {
    searching.value = false
  }
}

onMounted(async () => {
  try {
    categories.value = await getCategories()
    if (typeof route.query.keyword === 'string') {
      filters.keyword = route.query.keyword
    }
    await runTextSearch()
  } catch (error) {
    safeAlert(error.message)
  }
})
</script>

<style scoped>
.publisher-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.publisher-cell__avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
  display: grid;
  place-items: center;
  color: #163652;
  font-size: 12px;
  font-weight: 700;
}

.publisher-cell__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
