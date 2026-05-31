<template>
  <AdminLayout
    title="超级管理员 - 管理员账号管理"
    system-title="系统后台管理中心"
    subtitle="管理审核管理员账号状态与角色变更"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar" style="justify-content: space-between;">
        <h2 class="section-title" style="margin: 0;">管理员账号管理</h2>
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>账号类型</th>
            <th>账号名</th>
            <th>权限类型</th>
            <th>账号状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!rows.length">
            <td colspan="5" class="empty-cell">暂无管理员账号记录</td>
          </tr>
          <tr v-for="item in rows" :key="item.id">
            <td>{{ roleText(item.role) }}</td>
            <td>{{ item.username }}</td>
            <td>{{ permissionText(item.role) }}</td>
            <td>
              <span class="status-pill" :class="item.status === 'ACTIVE' ? 'success' : 'danger'">
                {{ item.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="toggleAdmin(item)">
                  {{ item.status === 'ACTIVE' ? '禁用' : '启用' }}
                </button>
                <button v-if="item.role === 'USER'" class="table-action" type="button" @click="setRole(item, 'REVIEW_ADMIN')">
                  设为审核管理员
                </button>
                <button v-else-if="item.role === 'REVIEW_ADMIN'" class="table-action" type="button" @click="setRole(item, 'USER')">
                  降为普通用户
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
import { getUsers, updateUserRole, updateUserStatus } from '../api/system'
import { runConfirmedAction } from '../utils/ui'

const allRows = ref([])
const rows = computed(() => allRows.value.filter((item) => item.role !== 'USER' || item.status === 'ACTIVE'))

function roleText(role) {
  if (role === 'SYS_ADMIN') return '超级管理员'
  if (role === 'REVIEW_ADMIN') return '审核管理员'
  return '普通用户'
}

function permissionText(role) {
  if (role === 'REVIEW_ADMIN') return '仅物品审核'
  if (role === 'SYS_ADMIN') return '系统最高权限'
  return '无后台权限'
}

async function loadRows() {
  allRows.value = await getUsers()
}

async function toggleAdmin(item) {
  const targetStatus = item.status === 'ACTIVE' ? 0 : 1
  const ok = await runConfirmedAction({
    confirmMessage: item.status === 'ACTIVE' ? `确认禁用管理员账号 ${item.username}？` : `确认启用管理员账号 ${item.username}？`,
    successMessage: item.status === 'ACTIVE' ? '管理员账号已禁用' : '管理员账号已启用',
    errorMessage: item.status === 'ACTIVE' ? '管理员账号禁用失败' : '管理员账号启用失败',
    action: async () => updateUserStatus(item.id, targetStatus),
  })
  if (!ok) return
  await loadRows()
}

async function setRole(item, role) {
  const ok = await runConfirmedAction({
    confirmMessage: role === 'REVIEW_ADMIN' ? `确认将 ${item.username} 设为审核管理员？` : `确认将 ${item.username} 降为普通用户？`,
    successMessage: role === 'REVIEW_ADMIN' ? '角色已提升为审核管理员' : '角色已降为普通用户',
    errorMessage: role === 'REVIEW_ADMIN' ? '角色提升失败' : '角色降级失败',
    action: async () => updateUserRole(item.id, role),
  })
  if (!ok) return
  await loadRows()
}

onMounted(loadRows)
</script>
