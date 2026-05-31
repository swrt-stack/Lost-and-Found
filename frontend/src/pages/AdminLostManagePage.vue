<template>
  <AdminLayout
    title="超级管理员 - 遗失物品管理"
    system-title="系统后台管理中心"
    subtitle="查看全部遗失物品记录与发布信息"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar">
        <input v-model.trim="keyword" class="field" style="max-width: 300px;" placeholder="搜索物品名称、地点或发布人" />
        <button class="primary-button" type="button" @click="loadRows">搜索</button>
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>ID</th>
            <th>物品信息</th>
            <th>地点</th>
            <th>发布人</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!rows.length">
            <td colspan="6" class="empty-cell">暂无遗失物品记录</td>
          </tr>
          <tr v-for="item in rows" :key="item.id">
            <td>{{ item.id }}</td>
            <td>
              <div class="item-cell">
                <div class="item-cell__thumb">
                  <img v-if="item.imageUrl" :src="toBackendAssetUrl(item.imageUrl)" :alt="item.title" />
                  <span v-else>无图</span>
                </div>
                <div class="item-cell__meta">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.category }}</span>
                  <span>{{ item.time }}</span>
                </div>
              </div>
            </td>
            <td>{{ item.location }}</td>
            <td>
              <div class="person-chip">
                <div class="person-chip__avatar">
                  <img v-if="item.publisherAvatarUrl" :src="toBackendAssetUrl(item.publisherAvatarUrl)" :alt="item.publisher" />
                  <span v-else>{{ avatarText(item.publisher) }}</span>
                </div>
                <span>{{ item.publisher }}</span>
              </div>
            </td>
            <td><span class="status-pill" :class="item.statusClass">{{ item.statusLabel }}</span></td>
            <td>
              <RouterLink class="table-action" :to="{ path: '/item-detail', query: { id: item.id } }">查看</RouterLink>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getAdminLostItems } from '../api/admin'
import { toBackendAssetUrl } from '../api/client'
import { adminMenu } from '../data/mock'
import { normalizeItem } from '../utils/items'
import { safeAlert } from '../utils/ui'

const keyword = ref('')
const rows = ref([])

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

async function loadRows() {
  try {
    rows.value = (await getAdminLostItems(keyword.value || undefined)).map(normalizeItem)
  } catch (error) {
    safeAlert(error.message)
  }
}

onMounted(loadRows)
</script>

<style scoped>
.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-cell__thumb {
  width: 62px;
  height: 62px;
  border-radius: 14px;
  overflow: hidden;
  display: grid;
  place-items: center;
  flex: 0 0 62px;
  background: #eef4f8;
  color: #607285;
  font-size: 12px;
}

.item-cell__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-cell__meta {
  display: grid;
  gap: 4px;
}

.item-cell__meta strong {
  color: #173854;
}

.item-cell__meta span {
  color: #667788;
  font-size: 12px;
}

.person-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.person-chip__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
  color: #173854;
  font-weight: 700;
  flex: 0 0 34px;
}

.person-chip__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
