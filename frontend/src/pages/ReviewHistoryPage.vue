<template>
  <AdminLayout
    title="审核管理员 - 已审核历史记录"
    system-title="审核管理员工作台"
    subtitle="查看审核动作、处理结果与操作时间"
    admin-label="管理员：审核账号"
    :menu-items="reviewMenu"
  >
    <section class="split-layout">
      <aside class="side-menu">
        <RouterLink v-for="item in reviewMenu" :key="item.to" :to="item.to" :class="{ active: item.to === '/review-history' }">
          {{ item.label }}
        </RouterLink>
      </aside>

      <section class="admin-main-card">
        <div v-if="historyHint" class="notice-bar" style="margin-bottom: 18px;">{{ historyHint }}</div>

        <table class="admin-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>动作</th>
              <th>详情</th>
              <th>审核人</th>
              <th>审核时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!logs.length">
              <td colspan="5" class="empty-cell">当前暂无可展示的审核历史记录</td>
            </tr>
            <tr v-for="(item, index) in logs" :key="`${item.createdAt}-${index}`">
              <td>{{ index + 1 }}</td>
              <td>{{ actionText(item.action) }}</td>
              <td>{{ item.detail || '-' }}</td>
              <td>{{ item.reviewer || '-' }}</td>
              <td>{{ item.createdAt || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getReviewHistory } from '../api/admin'
import { reviewMenu } from '../data/mock'

const logs = ref([])
const historyHint = ref('')

function actionText(action) {
  if (action === 'APPROVE_REVIEW') return '审核通过'
  if (action === 'REJECT_REVIEW') return '审核驳回'
  if (action === 'DELETE_REVIEW') return '删除记录'
  return action || '-'
}

onMounted(async () => {
  try {
    logs.value = await getReviewHistory()
    if (!logs.value.length) {
      historyHint.value = '当前暂无可展示的审核历史记录。'
    }
  } catch (error) {
    historyHint.value = `获取审核历史失败：${error.message}`
  }
})
</script>
