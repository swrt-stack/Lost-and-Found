<template>
  <PublicLayout>
    <section class="content-card">
      <div class="detail-grid">
        <div class="detail-image">
          <img
            v-if="item.imageUrl"
            :src="toBackendAssetUrl(item.imageUrl)"
            :alt="item.title"
            style="width: 100%; height: 420px; object-fit: cover; border-radius: 24px;"
          />
          <span v-else>物品大图展示区</span>
        </div>

        <div class="detail-list">
          <h2 class="section-title" style="margin-bottom: 6px;">物品详细信息</h2>
          <div><strong>物品标题：</strong>{{ item.title }}</div>
          <div><strong>物品状态：</strong><span class="status-pill" :class="item.statusClass">{{ item.statusLabel }}</span></div>
          <div><strong>物品分类：</strong>{{ item.category }}</div>
          <div><strong>发布类型：</strong>{{ item.typeLabel }}</div>
          <div><strong>地点：</strong>{{ item.location }}</div>
          <div><strong>时间：</strong>{{ item.time }}</div>
          <div><strong>物品描述：</strong>{{ item.description }}</div>

          <section class="publisher-card">
            <div class="publisher-card__avatar">
              <img
                v-if="item.publisherAvatarUrl"
                :src="toBackendAssetUrl(item.publisherAvatarUrl)"
                :alt="item.publisher"
              />
              <span v-else>{{ avatarText(item.publisher) }}</span>
            </div>
            <div class="publisher-card__body">
              <strong>{{ item.publisher }}</strong>
              <span>{{ item.type === 'lost' ? '失主发布' : '拾到者发布' }}</span>
            </div>
          </section>

          <div v-if="pageMessage.text" class="inline-message" :class="pageMessage.type">
            {{ pageMessage.text }}
          </div>

          <div v-if="currentClaim" class="inline-message info">
            <div><strong>我的认领状态：</strong>{{ claimStatusLabel(currentClaim.status) }}</div>
            <div><strong>申请时间：</strong>{{ currentClaim.createdAt }}</div>
            <div><strong>申请留言：</strong>{{ currentClaim.message || '-' }}</div>
            <div><strong>处理备注：</strong>{{ currentClaim.reviewRemark || '-' }}</div>
          </div>

          <div v-if="showClaimForm" class="form-row" style="margin-top: 14px;">
            <label>认领申请留言</label>
            <input
              v-model="claimMessage"
              class="field"
              placeholder="请输入认领说明或联系方式"
            />
          </div>

          <div class="inline-message info" style="margin-top: 16px;">
            <strong>在线沟通：</strong>
            {{ isOwner ? '这件物品的全部联系记录已经集中到消息中心，适合处理多人咨询。' : '如需确认物品细节，可进入消息中心与发布人单独沟通。' }}
          </div>

          <div class="action-row" style="justify-content: flex-start;">
            <button
              v-if="showClaimForm"
              class="primary-button"
              :disabled="submitting"
              @click="submitClaim"
            >
              提交认领申请
            </button>
            <button
              v-if="session.token"
              class="ghost-button"
              type="button"
              @click="goMessageCenter"
            >
              {{ isOwner ? '查看全部会话' : '联系发布人' }}
            </button>
            <RouterLink class="ghost-button nav-button" to="/items">返回列表</RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section class="content-card" style="margin-top: 22px;">
      <h2 class="section-title">相似线索推荐</h2>
      <div class="recommend-grid">
        <RouterLink
          v-for="related in relatedItems"
          :key="related.id"
          class="recommend-card"
          :to="{ path: '/item-detail', query: { id: related.id } }"
        >
          <div class="recommend-card__media">
            <img
              v-if="related.imageUrl"
              :src="toBackendAssetUrl(related.imageUrl)"
              :alt="related.title"
            />
            <span v-else>暂无图片</span>
          </div>
          <div class="recommend-card__body">
            <strong>{{ related.title }}</strong>
            <span>{{ related.location }}</span>
          </div>
        </RouterLink>
      </div>
    </section>
  </PublicLayout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getAuthSession, toBackendAssetUrl } from '../api/client'
import { getAdminItemDetail } from '../api/admin'
import {
  claimFoundItem,
  getMyClaims,
  getMyItems,
  searchItems,
} from '../api/item'
import { normalizeItem } from '../utils/items'

const route = useRoute()
const router = useRouter()
const item = ref(normalizeItem())
const allItems = ref([])
const itemOwnedByCurrentUser = ref(false)
const claimMessage = ref('')
const submitting = ref(false)
const pageMessage = ref({ type: '', text: '' })
const myClaims = ref({ sentClaims: [], receivedClaims: [] })

const session = computed(() => getAuthSession())
const isOwner = computed(() => itemOwnedByCurrentUser.value || (!!session.value.username && session.value.username === item.value.publisher))
const currentClaim = computed(() =>
  myClaims.value.sentClaims?.find((entry) => entry.itemId === item.value.id) || null
)
const showClaimForm = computed(() =>
  item.value.type === 'found'
  && !isOwner.value
  && item.value.status === 'APPROVED'
  && !currentClaim.value
)

const relatedItems = computed(() =>
  allItems.value
    .filter((entry) => entry.id !== item.value.id && entry.category === item.value.category)
    .slice(0, 4)
)

watch(
  [() => item.value.id, isOwner, () => item.value.type, () => item.value.status, currentClaim],
  () => {
    if (item.value.type !== 'found') {
      pageMessage.value = { type: '', text: '' }
      return
    }
    if (isOwner.value) {
      pageMessage.value = { type: 'warning', text: '这是你自己发布的招领信息，不能提交认领申请。' }
      return
    }
    if (currentClaim.value) {
      pageMessage.value = { type: 'info', text: '你已经提交过这条物品的认领申请。' }
      return
    }
    if (item.value.status !== 'APPROVED') {
      pageMessage.value = { type: 'warning', text: '当前仅已通过审核的招领物品支持认领申请。' }
      return
    }
    pageMessage.value = { type: '', text: '' }
  },
  { immediate: true }
)

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

function claimStatusLabel(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status || '-'
}

function goMessageCenter() {
  router.push({
    path: '/messages',
    query: {
      itemId: item.value.id,
    },
  })
}

async function loadItemDetail() {
  try {
    claimMessage.value = ''
    pageMessage.value = { type: '', text: '' }
    const [rows, claims] = await Promise.all([
      searchItems({}),
      session.value.token ? getMyClaims() : Promise.resolve({ sentClaims: [], receivedClaims: [] }),
    ])
    myClaims.value = claims
    allItems.value = rows.map(normalizeItem)
    const targetId = typeof route.query.id === 'string' ? route.query.id : ''
    const matched = allItems.value.find((entry) => entry.id === targetId)
    if (matched) {
      item.value = matched
      itemOwnedByCurrentUser.value = !!session.value.username && session.value.username === matched.publisher
      return
    }

    if (targetId && session.value.token) {
      const ownItems = await getMyItems()
      const ownMatched = ownItems.map(normalizeItem).find((entry) => entry.id === targetId)
      if (ownMatched) {
        item.value = ownMatched
        itemOwnedByCurrentUser.value = true
        return
      }
    }

    const role = session.value.role
    if (targetId && ['SYS_ADMIN', 'REVIEW_ADMIN'].includes(role)) {
      item.value = normalizeItem(await getAdminItemDetail(targetId))
      itemOwnedByCurrentUser.value = false
      return
    }

    if (allItems.value.length) {
      item.value = allItems.value[0]
      itemOwnedByCurrentUser.value = !!session.value.username && session.value.username === item.value.publisher
    }
  } catch (error) {
    pageMessage.value = { type: 'error', text: error.message || '加载物品详情失败' }
  }
}

async function submitClaim() {
  if (item.value.type !== 'found') {
    pageMessage.value = { type: 'warning', text: '当前仅招领物品支持提交认领申请。' }
    return
  }
  if (isOwner.value) {
    pageMessage.value = { type: 'warning', text: '不能认领自己发布的招领物品。' }
    return
  }
  if (currentClaim.value) {
    pageMessage.value = { type: 'info', text: '你已经提交过这条物品的认领申请。' }
    return
  }
  if (!claimMessage.value) {
    pageMessage.value = { type: 'warning', text: '请输入认领留言。' }
    return
  }

  submitting.value = true
  try {
    const result = await claimFoundItem(item.value.id, claimMessage.value)
    pageMessage.value = { type: 'success', text: result.result || '认领申请已提交。' }
    claimMessage.value = ''
    myClaims.value = await getMyClaims()
  } catch (error) {
    pageMessage.value = { type: 'error', text: error.message || '认领申请提交失败' }
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.query.id,
  () => {
    loadItemDetail()
  },
  { immediate: true }
)
</script>

<style scoped>
.publisher-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(223, 239, 250, 0.75), rgba(252, 236, 214, 0.78));
}

.publisher-card__avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
  display: grid;
  place-items: center;
  color: #163652;
  font-size: 22px;
  font-weight: 700;
  flex: 0 0 56px;
}

.publisher-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.publisher-card__body {
  display: grid;
  gap: 4px;
}

.publisher-card__body strong {
  color: #173854;
}

.publisher-card__body span {
  color: #607285;
  font-size: 13px;
}
</style>
