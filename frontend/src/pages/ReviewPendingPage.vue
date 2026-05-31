<template>
  <AdminLayout
    title="审核管理员 - 待审核物品管理"
    system-title="审核管理员工作台"
    subtitle="处理待审核物品并记录审核意见"
    admin-label="管理员：审核账号"
    :menu-items="reviewMenu"
  >
    <section class="split-layout">
      <aside class="side-menu">
        <RouterLink v-for="item in reviewMenu" :key="item.to" :to="item.to" :class="{ active: item.to === '/review-pending' }">
          {{ item.label }}
        </RouterLink>
      </aside>

      <section class="admin-main-card">
        <div class="toolbar">
          <input v-model.trim="keyword" class="field" style="max-width: 300px;" placeholder="搜索物品标题、发布人或地点" />
          <button class="primary-button" type="button" @click="pickFirstFiltered">搜索</button>
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
            <tr v-if="!filteredReviews.length">
              <td colspan="5" class="empty-cell">当前没有待审核物品</td>
            </tr>
            <tr v-for="(item, index) in filteredReviews" :key="item.id">
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
                  <button class="table-action" type="button" @click="selectRow(item)">查看详情</button>
                  <button class="table-action" type="button" @click="reviewAction('approve', item.id)">审核通过</button>
                  <button class="table-action" type="button" @click="reviewAction('reject', item.id)">审核驳回</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <section v-if="selected.id" class="dialog-card">
          <h3 class="card-title">物品审核详情</h3>
          <div class="dual-grid" style="margin-top: 18px;">
            <div class="preview-box preview-box--image">
              <img
                v-if="selected.imageUrl"
                :src="toBackendAssetUrl(selected.imageUrl)"
                :alt="selected.title"
              />
              <span v-else>暂无图片</span>
            </div>
            <div class="detail-list">
              <div><strong>物品名称：</strong>{{ selected.title }}</div>
              <div><strong>物品分类：</strong>{{ selected.category }}</div>
              <div><strong>发布类型：</strong>{{ selected.typeLabel }}</div>
              <div><strong>地点：</strong>{{ selected.location }}</div>
              <div><strong>时间：</strong>{{ selected.time }}</div>
              <div class="detail-person-row">
                <strong>发布人：</strong>
                <div class="person-chip">
                  <div class="person-chip__avatar">
                    <img v-if="selected.publisherAvatarUrl" :src="toBackendAssetUrl(selected.publisherAvatarUrl)" :alt="selected.publisher" />
                    <span v-else>{{ avatarText(selected.publisher) }}</span>
                  </div>
                  <span>{{ selected.publisher }}</span>
                </div>
              </div>
              <div><strong>物品描述：</strong>{{ selected.description }}</div>
            </div>
          </div>
          <div class="form-row" style="margin-top: 18px;">
            <label>审核备注 / 驳回原因</label>
            <textarea v-model="remark" class="textarea-field" placeholder="可填写审核说明，驳回时建议说明原因"></textarea>
          </div>
          <div class="action-row">
            <button class="primary-button" type="button" @click="reviewAction('approve', selected.id)">审核通过</button>
            <button class="ghost-button" type="button" @click="reviewAction('reject', selected.id)">驳回审核</button>
          </div>
        </section>
      </section>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '../components/AdminLayout.vue'
import { getAdminItemDetail, approveReview, getReviews, rejectReview } from '../api/admin'
import { toBackendAssetUrl } from '../api/client'
import { reviewMenu } from '../data/mock'
import { normalizeItem, normalizeReviewItem } from '../utils/items'
import { runConfirmedAction } from '../utils/ui'

const keyword = ref('')
const reviews = ref([])
const selected = reactive({})
const remark = ref('')

const filteredReviews = computed(() =>
  reviews.value.filter((item) => {
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

async function loadReviews() {
  reviews.value = (await getReviews()).map(normalizeReviewItem)
  if (reviews.value.length && !selected.id) {
    await selectRow(reviews.value[0])
  }
}

async function selectRow(item) {
  const detail = normalizeItem(await getAdminItemDetail(item.id))
  Object.assign(selected, { ...item, ...detail })
}

async function pickFirstFiltered() {
  if (filteredReviews.value.length) {
    await selectRow(filteredReviews.value[0])
  }
}

async function reviewAction(action, id) {
  const ok = await runConfirmedAction({
    confirmMessage: action === 'approve' ? '确认审核通过该物品？' : '确认驳回该物品？',
    successMessage: action === 'approve' ? '审核通过成功' : '审核驳回成功',
    errorMessage: action === 'approve' ? '审核通过失败' : '审核驳回失败',
    action: async () => {
      if (action === 'approve') {
        await approveReview(id, remark.value)
      } else {
        await rejectReview(id, remark.value)
      }
    },
  })
  if (!ok) return
  remark.value = ''
  Object.keys(selected).forEach((key) => delete selected[key])
  await loadReviews()
}

onMounted(loadReviews)
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

.preview-box--image {
  min-height: 220px;
  overflow: hidden;
}

.preview-box--image img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  border-radius: 18px;
}

.detail-person-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
