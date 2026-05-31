<template>
  <PublicLayout>
    <section class="user-center-grid">
      <aside class="side-menu user-center-menu">
        <a
          v-for="item in sections"
          :key="item.key"
          href="#"
          :class="{ active: activeTab === item.key }"
          @click.prevent="activeTab = item.key"
        >
          {{ item.label }}
        </a>
      </aside>

      <section class="content-card user-center-main">
        <h1 class="section-title user-center-main__title">个人中心</h1>

        <div v-if="activeTab === 'profile'" class="form-grid">
          <div class="profile-header profile-card">
            <div class="profile-avatar-wrap">
              <img
                v-if="profile.avatarUrl"
                :src="toBackendAssetUrl(profile.avatarUrl)"
                alt="avatar"
                class="profile-avatar"
              />
              <div v-else class="profile-avatar profile-avatar--placeholder">
                {{ avatarText(profile.nickname || profile.username) }}
              </div>
              <label class="avatar-upload-button">
                更换头像
                <input type="file" accept="image/*" hidden @change="handleAvatarChange" />
              </label>
            </div>

            <div class="profile-summary">
              <strong>{{ profile.nickname || profile.username }}</strong>
              <span>@{{ profile.username }}</span>
              <span>{{ profile.roleLabel }}</span>
            </div>
          </div>

          <div class="dual-grid">
            <div class="form-row">
              <label>用户名</label>
              <input class="field" :value="profile.username" disabled />
            </div>
            <div class="form-row">
              <label>角色</label>
              <input class="field" :value="profile.roleLabel" disabled />
            </div>
            <div class="form-row">
              <label>昵称</label>
              <input v-model="profile.nickname" class="field" />
            </div>
            <div class="form-row">
              <label>手机号</label>
              <input v-model="profile.phone" class="field" />
            </div>
          </div>

          <div class="action-row user-center-actions">
            <button class="primary-button" type="button" @click="saveProfile">保存资料</button>
          </div>
        </div>

        <div v-else-if="activeTab === 'lost'" class="table-card bare-table">
          <table class="results-table">
            <thead>
              <tr>
                <th>物品名称</th>
                <th>发布时间</th>
                <th>审核状态</th>
                <th>驳回原因</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!lostItems.length">
                <td colspan="5" class="empty-cell">暂无遗失发布</td>
              </tr>
              <tr v-for="entry in lostItems" :key="entry.id">
                <td>{{ entry.title }}</td>
                <td>{{ entry.time }}</td>
                <td><span class="status-pill" :class="entry.statusClass">{{ entry.statusLabel }}</span></td>
                <td>{{ entry.reviewRemark || '-' }}</td>
                <td class="claim-actions-cell">
                  <button class="table-action" type="button" @click="openItemDetail(entry.id)">查看详情</button>
                  <button
                    class="table-action"
                    type="button"
                    :disabled="['COMPLETED', 'OFFLINE'].includes(entry.status)"
                    @click="openEditor(entry)"
                  >
                    修改
                  </button>
                  <button
                    class="table-action"
                    type="button"
                    :disabled="entry.status === 'OFFLINE'"
                    @click="handleOffline(entry.id)"
                  >
                    下架
                  </button>
                  <button class="table-action" type="button" @click="handleDelete(entry.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else-if="activeTab === 'found'" class="table-card bare-table">
          <table class="results-table">
            <thead>
              <tr>
                <th>物品名称</th>
                <th>发布时间</th>
                <th>审核状态</th>
                <th>领取方式</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!foundItems.length">
                <td colspan="5" class="empty-cell">暂无招领发布</td>
              </tr>
              <tr v-for="entry in foundItems" :key="entry.id">
                <td>{{ entry.title }}</td>
                <td>{{ entry.time }}</td>
                <td><span class="status-pill" :class="entry.statusClass">{{ entry.statusLabel }}</span></td>
                <td>{{ entry.pickupMethod || '-' }}</td>
                <td class="claim-actions-cell">
                  <button class="table-action" type="button" @click="openItemDetail(entry.id)">查看详情</button>
                  <button
                    class="table-action"
                    type="button"
                    :disabled="['COMPLETED', 'OFFLINE'].includes(entry.status)"
                    @click="openEditor(entry)"
                  >
                    修改
                  </button>
                  <button
                    class="table-action"
                    type="button"
                    :disabled="entry.status === 'OFFLINE'"
                    @click="handleOffline(entry.id)"
                  >
                    下架
                  </button>
                  <button class="table-action" type="button" @click="handleDelete(entry.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else-if="activeTab === 'claims'" class="claim-board">
          <div class="claim-switcher">
            <button
              type="button"
              class="claim-switcher__tab"
              :class="{ active: claimView === 'sent' }"
              @click="claimView = 'sent'"
            >
              我提交的认领申请
            </button>
            <button
              type="button"
              class="claim-switcher__tab"
              :class="{ active: claimView === 'received' }"
              @click="claimView = 'received'"
            >
              待我确认的认领
            </button>
          </div>

          <div class="claim-toolbar claim-table-toolbar">
            <div class="claim-status-tabs">
              <button
                v-for="option in claimStatusOptions"
                :key="option.value"
                type="button"
                class="claim-status-tabs__item"
                :class="{ active: currentStatusFilter === option.value }"
                @click="currentStatusFilter = option.value"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="claim-count">共 {{ currentFilteredRows.length }} 条</div>
          </div>

          <div class="table-card bare-table">
            <table class="results-table">
              <thead v-if="claimView === 'sent'">
                <tr>
                  <th>物品名称</th>
                  <th>提交时间</th>
                  <th>状态</th>
                  <th>备注</th>
                  <th>操作</th>
                </tr>
              </thead>
              <thead v-else>
                <tr>
                  <th>物品名称</th>
                  <th>申请人</th>
                  <th>留言</th>
                  <th>状态</th>
                  <th>备注</th>
                  <th>操作</th>
                </tr>
              </thead>

              <tbody v-if="claimView === 'sent'">
                <tr v-if="!sentPagedRows.length">
                  <td colspan="5" class="empty-cell">暂无认领申请</td>
                </tr>
                <tr v-for="entry in sentPagedRows" :key="entry.id">
                  <td>{{ entry.itemTitle }}</td>
                  <td>{{ entry.createdAt }}</td>
                  <td><span class="status-pill" :class="claimStatusClass(entry.status)">{{ claimStatusLabel(entry.status) }}</span></td>
                  <td>{{ entry.reviewRemark || '-' }}</td>
                  <td>
                    <button class="table-action" type="button" @click="openItemDetail(entry.itemId)">查看物品</button>
                  </td>
                </tr>
              </tbody>

              <tbody v-else>
                <tr v-if="!receivedPagedRows.length">
                  <td colspan="6" class="empty-cell">暂无待确认记录</td>
                </tr>
                <tr v-for="entry in receivedPagedRows" :key="entry.id">
                  <td>{{ entry.itemTitle }}</td>
                  <td>{{ entry.applicant }}</td>
                  <td class="claim-message-cell">{{ entry.message || '-' }}</td>
                  <td><span class="status-pill" :class="claimStatusClass(entry.status)">{{ claimStatusLabel(entry.status) }}</span></td>
                  <td>{{ entry.reviewRemark || '-' }}</td>
                  <td class="claim-actions-cell">
                    <button class="table-action" type="button" @click="openItemDetail(entry.itemId)">查看物品</button>
                    <button
                      v-if="entry.status === 'PENDING'"
                      class="table-action"
                      type="button"
                      @click="handleApproveClaim(entry.id)"
                    >
                      通过
                    </button>
                    <button
                      v-if="entry.status === 'PENDING'"
                      class="table-action"
                      type="button"
                      @click="handleRejectClaim(entry.id)"
                    >
                      驳回
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="currentTotalPages > 1" class="pager claim-pager">
              <button type="button" :disabled="currentPage === 1" @click="currentPage--">上一页</button>
              <span>第 {{ currentPage }} / {{ currentTotalPages }} 页</span>
              <button type="button" :disabled="currentPage === currentTotalPages" @click="currentPage++">下一页</button>
            </div>
          </div>
        </div>

        <div v-else class="list-card user-message-card">
          <h3 class="card-title">系统消息通知</h3>
          <div v-if="messages.length" class="side-list user-message-list">
            <div v-for="entry in messages" :key="entry.id" class="user-message-item">
              <strong>{{ entry.time }}</strong>
              <span>{{ entry.content }}</span>
            </div>
          </div>
          <div v-else class="empty-cell user-message-empty">暂无系统消息</div>
        </div>
      </section>
    </section>

    <div v-if="editorVisible" class="modal-mask" @click.self="closeEditor">
      <section class="modal-card">
        <div class="modal-card__header">
          <h3 class="card-title">{{ editForm.type === 'lost' ? '修改遗失发布' : '修改招领发布' }}</h3>
          <button class="table-action" type="button" @click="closeEditor">关闭</button>
        </div>

        <div class="form-grid">
          <div class="dual-grid">
            <div class="form-row">
              <label>物品名称</label>
              <input v-model="editForm.title" class="field" />
            </div>
            <div class="form-row">
              <label>物品分类</label>
              <select v-model="editForm.categoryId" class="select-field">
                <option value="">请选择分类</option>
                <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </div>
            <div class="form-row">
              <label>{{ editForm.type === 'lost' ? '遗失地点' : '捡到地点' }}</label>
              <input v-model="editForm.location" class="field" />
            </div>
            <div class="form-row">
              <label>{{ editForm.type === 'lost' ? '遗失时间' : '捡到时间' }}</label>
              <input v-model="editForm.eventTime" class="field" type="datetime-local" />
            </div>
          </div>

          <div class="form-row">
            <label>物品详细描述</label>
            <textarea v-model="editForm.description" class="textarea-field"></textarea>
          </div>

          <div class="dual-grid">
            <div class="form-row">
              <label>补充图片上传</label>
              <input class="field" type="file" multiple accept="image/*" @change="handleEditFileChange" />
            </div>
            <div class="form-row">
              <label>{{ editForm.type === 'lost' ? '联系方式' : '领取方式' }}</label>
              <input v-model="editForm.extraField" class="field" />
            </div>
          </div>

          <div v-if="editPreviewUrls.length || editExistingImages.length" class="card-grid">
            <div v-for="url in editExistingImages" :key="url" class="preview-box preview-box--image">
              <img :src="toBackendAssetUrl(url)" alt="existing" class="preview-box__image" />
            </div>
            <div v-for="url in editPreviewUrls" :key="url" class="preview-box preview-box--image">
              <img :src="url" alt="preview" class="preview-box__image" />
            </div>
          </div>

          <div class="action-row action-row--end">
            <button class="ghost-button" type="button" @click="closeEditor">取消</button>
            <button class="primary-button" type="button" :disabled="editorSubmitting" @click="submitEdit">
              {{ editorSubmitting ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </PublicLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getCategories } from '../api/category'
import { toBackendAssetUrl } from '../api/client'
import { getMessages } from '../api/message'
import { approveClaim, deleteItem, getMyClaims, getMyItems, offlineItem, rejectClaim, updateItem } from '../api/item'
import { getProfile, updateProfile } from '../api/system'
import { uploadImage } from '../api/upload'
import { normalizeItem, toBackendDateTime, toDateTimeInputValue } from '../utils/items'
import { roleLabel, safeAlert } from '../utils/ui'

const PAGE_SIZE = 5

const router = useRouter()

const sections = [
  { key: 'profile', label: '个人资料修改' },
  { key: 'lost', label: '我的遗失发布' },
  { key: 'found', label: '我的招领发布' },
  { key: 'claims', label: '我的认领申请' },
  { key: 'messages', label: '系统消息通知' },
]

const claimStatusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

const activeTab = ref('profile')
const claimView = ref('sent')
const currentStatusFilter = ref('ALL')
const sentPage = ref(1)
const receivedPage = ref(1)
const categories = ref([])
const editorVisible = ref(false)
const editorSubmitting = ref(false)
const editFiles = ref([])
const editPreviewUrls = ref([])
const editExistingImages = ref([])

const profile = reactive({
  username: '',
  nickname: '',
  phone: '',
  avatarUrl: '',
  role: '',
  roleLabel: '',
})

const editForm = reactive({
  id: '',
  type: 'lost',
  title: '',
  categoryId: '',
  location: '',
  eventTime: '',
  description: '',
  extraField: '',
})

const items = ref([])
const claims = reactive({
  sentClaims: [],
  receivedClaims: [],
})
const messages = ref([])

const lostItems = computed(() => items.value.filter((item) => item.type === 'lost'))
const foundItems = computed(() => items.value.filter((item) => item.type === 'found'))
const sentFilteredRows = computed(() => filterClaimsByStatus(claims.sentClaims, currentStatusFilter.value))
const receivedFilteredRows = computed(() => filterClaimsByStatus(claims.receivedClaims, currentStatusFilter.value))
const sentTotalPages = computed(() => Math.max(1, Math.ceil(sentFilteredRows.value.length / PAGE_SIZE)))
const receivedTotalPages = computed(() => Math.max(1, Math.ceil(receivedFilteredRows.value.length / PAGE_SIZE)))
const sentPagedRows = computed(() => paginate(sentFilteredRows.value, sentPage.value))
const receivedPagedRows = computed(() => paginate(receivedFilteredRows.value, receivedPage.value))
const currentFilteredRows = computed(() => (claimView.value === 'sent' ? sentFilteredRows.value : receivedFilteredRows.value))
const currentTotalPages = computed(() => (claimView.value === 'sent' ? sentTotalPages.value : receivedTotalPages.value))
const currentPage = computed({
  get: () => (claimView.value === 'sent' ? sentPage.value : receivedPage.value),
  set: (value) => {
    if (claimView.value === 'sent') {
      sentPage.value = value
      return
    }
    receivedPage.value = value
  },
})

watch(currentStatusFilter, () => {
  sentPage.value = 1
  receivedPage.value = 1
})

watch(sentTotalPages, (value) => {
  if (sentPage.value > value) sentPage.value = value
})

watch(receivedTotalPages, (value) => {
  if (receivedPage.value > value) receivedPage.value = value
})

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function paginate(rows, page) {
  const start = (page - 1) * PAGE_SIZE
  return rows.slice(start, start + PAGE_SIZE)
}

function filterClaimsByStatus(rows, status) {
  if (status === 'ALL') return rows
  return rows.filter((entry) => entry.status === status)
}

function claimStatusLabel(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status || '-'
}

function claimStatusClass(status) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'pending'
}

function openItemDetail(itemId) {
  if (!itemId) {
    safeAlert('当前记录缺少物品编号，无法查看详情')
    return
  }
  router.push({ path: '/item-detail', query: { id: itemId } })
}

function openEditor(entry) {
  editForm.id = entry.id
  editForm.type = entry.type
  editForm.title = entry.title
  editForm.categoryId = resolveCategoryId(entry.category)
  editForm.location = entry.location
  editForm.eventTime = toDateTimeInputValue(entry.time)
  editForm.description = entry.description || ''
  editForm.extraField = entry.type === 'lost' ? (entry.contact || '') : (entry.pickupMethod || '')
  editFiles.value = []
  editPreviewUrls.value = []
  editExistingImages.value = entry.images
    ? entry.images.split(',').map((item) => item.trim()).filter(Boolean)
    : []
  editorVisible.value = true
}

function closeEditor() {
  editorVisible.value = false
  editFiles.value = []
  editPreviewUrls.value = []
  editExistingImages.value = []
}

function handleEditFileChange(event) {
  editFiles.value = Array.from(event.target.files || [])
  editPreviewUrls.value = editFiles.value.map((file) => URL.createObjectURL(file))
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const uploaded = await uploadImage(file)
    profile.avatarUrl = uploaded.url
    safeAlert('头像已上传，记得保存资料')
  } catch (error) {
    safeAlert(error.message)
  } finally {
    event.target.value = ''
  }
}

function resolveCategoryId(categoryName) {
  const matched = categories.value.find((item) => item.name === categoryName)
  return matched ? matched.id : ''
}

async function loadUserCenter() {
  try {
    const [profileData, myItems, myClaims, myMessages, categoryRows] = await Promise.all([
      getProfile(),
      getMyItems(),
      getMyClaims(),
      getMessages(),
      getCategories(),
    ])
    categories.value = categoryRows
    profile.username = profileData.username
    profile.nickname = profileData.nickname
    profile.phone = profileData.phone
    profile.avatarUrl = profileData.avatarUrl || ''
    profile.role = profileData.role
    profile.roleLabel = roleLabel(profileData.role)
    items.value = myItems.map((item) => ({
      ...normalizeItem(item),
      reviewRemark: item.reviewRemark || '',
    }))
    claims.sentClaims = myClaims.sentClaims || []
    claims.receivedClaims = myClaims.receivedClaims || []
    messages.value = myMessages
  } catch (error) {
    safeAlert(error.message)
  }
}

async function saveProfile() {
  try {
    const updated = await updateProfile({
      nickname: profile.nickname,
      phone: profile.phone,
      avatarUrl: profile.avatarUrl,
    })
    profile.nickname = updated.nickname
    profile.phone = updated.phone
    profile.avatarUrl = updated.avatarUrl || ''
    safeAlert('资料已更新')
  } catch (error) {
    safeAlert(error.message)
  }
}

async function submitEdit() {
  if (!editForm.title || !editForm.categoryId || !editForm.location || !editForm.eventTime || !editForm.description) {
    safeAlert('请完整填写修改信息')
    return
  }

  editorSubmitting.value = true
  try {
    const uploaded = []
    for (const file of editFiles.value) {
      uploaded.push(await uploadImage(file))
    }

    const mergedImages = [
      ...editExistingImages.value,
      ...uploaded.map((item) => item.url),
    ].filter(Boolean).join(',')

    await updateItem(editForm.id, {
      title: editForm.title,
      description: editForm.description,
      categoryId: Number(editForm.categoryId),
      location: editForm.location,
      eventTime: toBackendDateTime(editForm.eventTime),
      contact: editForm.type === 'lost' ? editForm.extraField : '',
      pickupMethod: editForm.type === 'found' ? editForm.extraField : '',
      images: mergedImages,
    })

    safeAlert('发布内容已更新')
    closeEditor()
    await loadUserCenter()
  } catch (error) {
    safeAlert(error.message)
  } finally {
    editorSubmitting.value = false
  }
}

async function handleOffline(itemId) {
  if (!window.confirm('确认下架这条发布吗？')) return
  try {
    await offlineItem(itemId)
    safeAlert('发布已下架')
    await loadUserCenter()
  } catch (error) {
    safeAlert(error.message)
  }
}

async function handleDelete(itemId) {
  if (!window.confirm('确认删除这条发布吗？删除后不可恢复。')) return
  try {
    await deleteItem(itemId)
    safeAlert('发布已删除')
    await loadUserCenter()
  } catch (error) {
    safeAlert(error.message)
  }
}

async function handleApproveClaim(claimId) {
  const remark = window.prompt('可选填写通过备注：', '') || ''
  try {
    await approveClaim(claimId, remark)
    safeAlert('认领申请已通过')
    await loadUserCenter()
  } catch (error) {
    safeAlert(error.message)
  }
}

async function handleRejectClaim(claimId) {
  const remark = window.prompt('请填写驳回原因：', '')
  if (remark === null) return
  try {
    await rejectClaim(claimId, remark)
    safeAlert('认领申请已驳回')
    await loadUserCenter()
  } catch (error) {
    safeAlert(error.message)
  }
}

onMounted(loadUserCenter)
</script>

<style scoped>
.user-center-menu {
  position: sticky;
  top: 108px;
}

.user-center-main {
  display: grid;
  gap: 18px;
}

.user-center-main__title {
  margin-bottom: 2px;
}

.profile-card {
  padding: 22px 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 246, 231, 0.96), rgba(255, 252, 247, 0.98));
  border: 1px solid rgba(196, 170, 133, 0.45);
}

.bare-table {
  padding: 0;
  box-shadow: none;
  border: 0;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.profile-avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.profile-avatar {
  width: 102px;
  height: 102px;
  border-radius: 50%;
  object-fit: cover;
  background: #d8e9f6;
  border: 4px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 36px rgba(18, 50, 79, 0.14);
}

.profile-avatar--placeholder {
  display: grid;
  place-items: center;
  color: #153550;
  font-size: 34px;
  font-weight: 700;
  background: linear-gradient(135deg, #d7ecfb, #f8dcb8);
}

.avatar-upload-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 106px;
  padding: 10px 16px;
  border-radius: 999px;
  background: #153550;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(21, 53, 80, 0.16);
}

.profile-summary {
  display: grid;
  gap: 6px;
}

.profile-summary strong {
  font-size: 28px;
  color: #153550;
}

.profile-summary span {
  color: #5f6f7f;
}

.user-center-actions {
  justify-content: flex-start;
}

.user-message-card {
  padding: 22px;
}

.user-message-list {
  gap: 14px;
  margin-top: 14px;
}

.user-message-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 248, 239, 0.78);
  border: 1px solid rgba(196, 170, 133, 0.34);
}

.user-message-item strong {
  color: var(--primary-deep);
  font-size: 13px;
}

.user-message-item span {
  color: var(--muted);
  line-height: 1.7;
}

.user-message-empty {
  margin-top: 14px;
}

.preview-box--image {
  min-height: 120px;
}

.preview-box__image {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
}

.action-row--end {
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .profile-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
