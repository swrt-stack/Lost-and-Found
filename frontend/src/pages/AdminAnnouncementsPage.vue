<template>
  <AdminLayout
    title="超级管理员 - 系统公告管理"
    system-title="系统后台管理中心"
    subtitle="统一维护平台公告内容与发布状态"
    admin-label="管理员：超级管理员"
    :menu-items="adminMenu"
  >
    <section class="admin-main-card">
      <div class="toolbar" style="justify-content: space-between;">
        <h2 class="section-title" style="margin: 0;">公告管理</h2>
        <button class="primary-button" type="button" @click="startCreate">新增公告</button>
      </div>

      <div class="dialog-card" style="margin-bottom: 18px;">
        <div class="form-grid">
          <div class="form-row">
            <label>公告标题</label>
            <input v-model.trim="form.title" class="field" />
          </div>
          <div class="form-row">
            <label>公告内容</label>
            <textarea v-model.trim="form.content" class="textarea-field"></textarea>
          </div>
          <div class="form-row">
            <label>发布状态</label>
            <select v-model="form.status" class="select-field">
              <option :value="1">已发布</option>
              <option :value="0">草稿</option>
            </select>
          </div>
          <div class="action-row" style="justify-content: flex-start;">
            <button class="primary-button" type="button" @click="submitForm">
              {{ form.id ? '保存修改' : '创建公告' }}
            </button>
          </div>
        </div>
      </div>

      <table class="admin-table" style="margin-top: 18px;">
        <thead>
          <tr>
            <th>序号</th>
            <th>公告标题</th>
            <th>发布时间</th>
            <th>发布状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!rows.length">
            <td colspan="5" class="empty-cell">暂无公告记录</td>
          </tr>
          <tr v-for="item in rows" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.title }}</td>
            <td>{{ item.createdAt || '-' }}</td>
            <td>
              <span class="status-pill" :class="item.status === 'PUBLISHED' ? 'success' : 'pending'">
                {{ item.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
            </td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="editRow(item)">编辑</button>
                <button class="table-action" type="button" @click="removeRow(item.id)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { createAnnouncement, deleteAnnouncement, getAdminAnnouncements, updateAnnouncement } from '../api/admin'
import { adminMenu } from '../data/mock'
import { runConfirmedAction, safeAlert } from '../utils/ui'

const rows = ref([])
const form = reactive({
  id: null,
  title: '',
  content: '',
  status: 1,
})

async function loadRows() {
  rows.value = await getAdminAnnouncements()
}

function startCreate() {
  form.id = null
  form.title = ''
  form.content = ''
  form.status = 1
}

function editRow(item) {
  form.id = item.id
  form.title = item.title
  form.content = item.content
  form.status = item.status === 'PUBLISHED' ? 1 : 0
}

async function submitForm() {
  if (!form.title || !form.content) {
    safeAlert('请完整填写公告标题和内容')
    return
  }
  const ok = await runConfirmedAction({
    confirmMessage: form.id ? '确认保存当前公告修改？' : '确认创建这条公告？',
    successMessage: form.id ? '公告修改成功' : '公告创建成功',
    errorMessage: form.id ? '公告修改失败' : '公告创建失败',
    action: async () => {
      if (form.id) {
        await updateAnnouncement(form.id, { title: form.title, content: form.content, status: form.status })
      } else {
        await createAnnouncement({ title: form.title, content: form.content, status: form.status })
      }
    },
  })
  if (!ok) return
  startCreate()
  await loadRows()
}

async function removeRow(id) {
  const ok = await runConfirmedAction({
    confirmMessage: '确认删除这条公告？',
    successMessage: '公告删除成功',
    errorMessage: '公告删除失败',
    action: async () => deleteAnnouncement(id),
  })
  if (!ok) return
  await loadRows()
}

onMounted(loadRows)
</script>
