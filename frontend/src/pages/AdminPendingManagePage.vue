<template>
  <AdminLayout
    title="超级管理员 - 待审核物品管理"
    system-title="系统后台管理中心"
    subtitle="超级管理员可直接处理待审核记录"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar">
        <input v-model.trim="keyword" class="field" style="max-width: 300px;" placeholder="搜索物品标题、发布人或地点" />
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>序号</th>
            <th>物品信息</th>
            <th>发布人</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!filteredRows.length">
            <td colspan="5" class="empty-cell">当前没有待审核记录</td>
          </tr>
          <tr v-for="(item, index) in filteredRows" :key="item.id">
            <td>{{ index + 1 }}</td>
            <td>
              <div class="item-brief">
                <strong>{{ item.title }}</strong>
                <span>{{ item.typeLabel }} · {{ item.category }}</span>
                <span>{{ item.location }}</span>
              </div>
            </td>
            <td>
              <div class="person-chip">
                <div class="person-chip__avatar">
                  <img v-if="item.publisherAvatarUrl" :src="toBackendAssetUrl(item.publisherAvatarUrl)" :alt="item.publisher" />
                  <span v-else>{{ avatarText(item.publisher) }}</span>
                </div>
                <span>{{ item.publisher }}</span>
              </div>
            </td>
            <td>{{ item.time }}</td>
            <td>
              <div class="table-actions">
                <RouterLink class="table-action" :to="{ path: '/item-detail', query: { id: item.id } }">查看</RouterLink>
                <button class="table-action" type="button" @click="handleAction('approve', item.id)">通过</button>
                <button class="table-action" type="button" @click="handleAction('reject', item.id)">驳回</button>
                <button class="table-action" type="button" @click="handleAction('delete', item.id)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { approveReview, deleteReview, getReviews, rejectReview } from '../api/admin'
import { toBackendAssetUrl } from '../api/client'
import { adminMenu } from '../data/mock'
import { normalizeReviewItem } from '../utils/items'
import { runConfirmedAction } from '../utils/ui'

const keyword = ref('')
const rows = ref([])

const filteredRows = computed(() =>
  rows.value.filter((item) => {
    if (!keyword.value) return true
    return [item.title, item.publisher, item.location]
      .filter(Boolean)
      .some((field) => String(field).includes(keyword.value))
  })
)

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

async function loadRows() {
  rows.value = (await getReviews()).map(normalizeReviewItem)
}

async function handleAction(action, id) {
  const ok = await runConfirmedAction({
    confirmMessage:
      action === 'approve'
        ? '确认通过该待审核物品？'
        : action === 'reject'
          ? '确认驳回该待审核物品？'
          : '确认删除该待审核物品？',
    successMessage:
      action === 'approve'
        ? '审核通过成功'
        : action === 'reject'
          ? '审核驳回成功'
          : '删除成功',
    errorMessage:
      action === 'approve'
        ? '审核通过失败'
        : action === 'reject'
          ? '审核驳回失败'
          : '删除失败',
    action: async () => {
      if (action === 'approve') await approveReview(id, '')
      if (action === 'reject') await rejectReview(id, '')
      if (action === 'delete') await deleteReview(id, '')
    },
  })
  if (!ok) return
  await loadRows()
}

onMounted(loadRows)
</script>

<style scoped>
.item-brief {
  display: grid;
  gap: 4px;
}

.item-brief strong {
  color: #173854;
}

.item-brief span {
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
