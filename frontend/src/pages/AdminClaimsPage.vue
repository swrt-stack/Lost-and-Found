<template>
  <AdminLayout
    title="超级管理员 - 认领记录管理"
    system-title="系统后台管理中心"
    subtitle="统一查看认领申请、处理状态与备注"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar">
        <input v-model.trim="keyword" class="field" style="max-width: 300px;" placeholder="搜索物品名称、申请人或发布人" />
        <select v-model="statusFilter" class="select-field" style="max-width: 180px;">
          <option value="">全部状态</option>
          <option value="PENDING">待处理</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <button class="primary-button" type="button" @click="resetFilters">重置筛选</button>
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>序号</th>
            <th>物品</th>
            <th>申请人</th>
            <th>发布人</th>
            <th>申请时间</th>
            <th>状态</th>
            <th>申请留言</th>
            <th>备注 / 驳回原因</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!filteredRows.length">
            <td colspan="9" class="empty-cell">暂无符合条件的认领记录</td>
          </tr>
          <tr v-for="(item, index) in filteredRows" :key="item.id">
            <td>{{ index + 1 }}</td>
            <td>
              <div class="item-brief">
                <strong>{{ item.itemTitle }}</strong>
                <span>{{ item.itemId }}</span>
              </div>
            </td>
            <td>
              <div class="person-chip">
                <div class="person-chip__avatar">
                  <img v-if="item.applicantAvatarUrl" :src="toBackendAssetUrl(item.applicantAvatarUrl)" :alt="item.applicant" />
                  <span v-else>{{ avatarText(item.applicant) }}</span>
                </div>
                <span>{{ item.applicant }}</span>
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
            <td>{{ item.createdAt }}</td>
            <td><span class="status-pill" :class="statusClass(item.status)">{{ statusText(item.status) }}</span></td>
            <td class="message-cell">{{ item.message || '-' }}</td>
            <td>
              <textarea
                v-if="item.status === 'PENDING'"
                v-model="remarks[item.id]"
                class="textarea-field"
                style="min-height: 84px; min-width: 240px;"
                placeholder="填写处理备注或驳回原因"
              ></textarea>
              <span v-else>{{ item.reviewRemark || '-' }}</span>
            </td>
            <td>
              <div class="table-actions">
                <RouterLink class="table-action" :to="{ path: '/item-detail', query: { id: item.itemId } }">查看物品</RouterLink>
                <button v-if="item.status === 'PENDING'" class="table-action" type="button" @click="handleAction('approve', item.id)">通过</button>
                <button v-if="item.status === 'PENDING'" class="table-action" type="button" @click="handleAction('reject', item.id)">驳回</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { approveAdminClaim, getAdminClaims, rejectAdminClaim } from '../api/admin'
import { toBackendAssetUrl } from '../api/client'
import { adminMenu } from '../data/mock'
import { runConfirmedAction, statusClass } from '../utils/ui'

const rows = ref([])
const remarks = reactive({})
const keyword = ref('')
const statusFilter = ref('')

const filteredRows = computed(() =>
  rows.value.filter((item) => {
    const matchesKeyword =
      !keyword.value ||
      [item.itemTitle, item.applicant, item.publisher]
        .filter(Boolean)
        .some((field) => String(field).includes(keyword.value))
    const matchesStatus = !statusFilter.value || item.status === statusFilter.value
    return matchesKeyword && matchesStatus
  })
)

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function statusText(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status || '-'
}

async function loadRows() {
  rows.value = await getAdminClaims()
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
}

async function handleAction(action, id) {
  const remark = remarks[id] || ''
  const ok = await runConfirmedAction({
    confirmMessage: action === 'approve' ? '确认通过这条认领申请？' : '确认驳回这条认领申请？',
    successMessage: action === 'approve' ? '认领申请已通过' : '认领申请已驳回',
    errorMessage: action === 'approve' ? '认领通过失败' : '认领驳回失败',
    action: async () => {
      if (action === 'approve') {
        await approveAdminClaim(id, remark)
      } else {
        await rejectAdminClaim(id, remark)
      }
    },
  })
  if (!ok) return
  delete remarks[id]
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

.message-cell {
  max-width: 240px;
  color: #41576d;
  white-space: pre-wrap;
}
</style>
