<template>
  <PublicLayout :logged-in="true">
    <section class="table-card">
      <h1 class="page-heading">全部物品信息列表</h1>

      <div class="tab-row" style="margin-bottom: 18px;">
        <button class="chip" type="button" @click="setType('')">全部类型</button>
        <button class="chip" type="button" @click="setType('lost')">遗失物品</button>
        <button class="chip" type="button" @click="setType('found')">招领物品</button>
        <select v-model="filters.categoryId" class="select-field" style="max-width: 180px;" @change="loadItems">
          <option value="">分类筛选</option>
          <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select>
        <input v-model="filters.location" class="field" style="max-width: 180px;" placeholder="地点筛选" />
        <button class="primary-button" type="button" @click="loadItems">搜索</button>
      </div>

      <table class="results-table">
        <thead>
          <tr>
            <th>图片</th>
            <th>物品名称</th>
            <th>发布人</th>
            <th>类型</th>
            <th>地点</th>
            <th>时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="8" class="empty-cell">暂无公开物品</td>
          </tr>
          <tr v-for="item in items" :key="item.id">
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
            <td>{{ item.typeLabel }}</td>
            <td>{{ item.location }}</td>
            <td>{{ item.time }}</td>
            <td><span class="status-pill" :class="item.statusClass">{{ item.statusLabel }}</span></td>
            <td>
              <RouterLink class="table-action" :to="{ path: '/item-detail', query: { id: item.id } }">查看详情</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </PublicLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PublicLayout from '../components/PublicLayout.vue'
import { toBackendAssetUrl } from '../api/client'
import { getCategories } from '../api/category'
import { searchItems } from '../api/item'
import { normalizeItem } from '../utils/items'
import { safeAlert } from '../utils/ui'

const categories = ref([])
const items = ref([])
const filters = reactive({
  type: '',
  categoryId: '',
  location: '',
})

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function setType(type) {
  filters.type = type
  loadItems()
}

async function loadItems() {
  try {
    const rows = await searchItems({
      type: filters.type || undefined,
      categoryId: filters.categoryId || undefined,
      location: filters.location || undefined,
    })
    items.value = rows.map(normalizeItem)
  } catch (error) {
    safeAlert(error.message)
  }
}

onMounted(async () => {
  try {
    categories.value = await getCategories()
    await loadItems()
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
