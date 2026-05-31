<template>
  <AdminLayout
    title="超级管理员 - 用户信息管理"
    system-title="系统后台管理中心"
    subtitle="查看用户账号、头像、联系方式与启用状态"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar">
        <input v-model.trim="keyword" class="field" style="max-width: 300px;" placeholder="搜索用户名、昵称或手机号" />
        <button class="primary-button" type="button">搜索</button>
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>用户ID</th>
            <th>用户信息</th>
            <th>手机号</th>
            <th>角色</th>
            <th>账号状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!filtered.length">
            <td colspan="6" class="empty-cell">暂无匹配的用户记录</td>
          </tr>
          <tr v-for="item in filtered" :key="item.id">
            <td>{{ item.id }}</td>
            <td>
              <div class="user-identity-cell">
                <div class="user-identity-cell__avatar">
                  <img
                    v-if="item.avatarUrl"
                    :src="toBackendAssetUrl(item.avatarUrl)"
                    :alt="item.nickname || item.username"
                  />
                  <span v-else>{{ avatarText(item.nickname || item.username) }}</span>
                </div>
                <div class="user-identity-cell__meta">
                  <strong>{{ item.nickname || item.username }}</strong>
                  <span>@{{ item.username }}</span>
                </div>
              </div>
            </td>
            <td>{{ item.phone || '-' }}</td>
            <td>{{ roleText(item.role) }}</td>
            <td>
              <span class="status-pill" :class="item.status === 'ACTIVE' ? 'success' : 'danger'">
                {{ item.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="toggleStatus(item)">
                  {{ item.status === 'ACTIVE' ? '禁用账号' : '启用账号' }}
                </button>
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
import { adminMenu } from '../data/mock'
import { toBackendAssetUrl } from '../api/client'
import { getUsers, updateUserStatus } from '../api/system'
import { runConfirmedAction } from '../utils/ui'

const keyword = ref('')
const rows = ref([])

const filtered = computed(() => {
  const value = keyword.value.toLowerCase()
  return rows.value.filter((item) => {
    if (!value) return true
    return [item.username, item.nickname, item.phone]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(value))
  })
})

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function roleText(role) {
  if (role === 'SYS_ADMIN') return '超级管理员'
  if (role === 'REVIEW_ADMIN') return '审核管理员'
  return '普通用户'
}

async function loadRows() {
  rows.value = await getUsers()
}

async function toggleStatus(item) {
  const targetStatus = item.status === 'ACTIVE' ? 0 : 1
  const ok = await runConfirmedAction({
    confirmMessage: item.status === 'ACTIVE' ? `确认禁用用户 ${item.username}？` : `确认启用用户 ${item.username}？`,
    successMessage: item.status === 'ACTIVE' ? '账号已禁用' : '账号已启用',
    errorMessage: item.status === 'ACTIVE' ? '禁用账号失败' : '启用账号失败',
    action: async () => updateUserStatus(item.id, targetStatus),
  })
  if (!ok) return
  await loadRows()
}

onMounted(loadRows)
</script>

<style scoped>
.user-identity-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-identity-cell__avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  flex: 0 0 42px;
  color: #173854;
  font-weight: 700;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
}

.user-identity-cell__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-identity-cell__meta {
  display: grid;
  gap: 3px;
}

.user-identity-cell__meta strong {
  color: #173854;
}

.user-identity-cell__meta span {
  color: #667788;
  font-size: 12px;
}
</style>
