<template>
  <PublicLayout>
    <section class="content-card message-shell">
      <div class="message-center-head">
        <div>
          <h1 class="section-title" style="margin-bottom: 8px;">消息中心</h1>
          <p class="section-subtitle">把系统通知和物品沟通拆开处理，会话按物品归类，方便同时处理多人联系和历史消息。</p>
        </div>
        <div class="message-summary-cards">
          <div class="stat-card">
            <span>系统通知未读</span>
            <strong>{{ systemSummary.unreadCount }}</strong>
          </div>
          <div class="stat-card">
            <span>会话未读</span>
            <strong>{{ chatSummary.unreadCount }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="content-card" style="margin-top: 22px;">
      <div class="claim-switcher">
        <button type="button" class="claim-switcher__tab" :class="{ active: activeTab === 'chats' }" @click="activeTab = 'chats'">
          物品沟通
        </button>
        <button type="button" class="claim-switcher__tab" :class="{ active: activeTab === 'notices' }" @click="activeTab = 'notices'">
          系统通知
        </button>
      </div>

      <template v-if="activeTab === 'chats'">
        <div class="chat-workbench">
          <aside class="chat-sidebar">
            <div class="chat-sidebar__header">
              <strong>会话列表</strong>
              <span>{{ filteredConversations.length }} 个会话</span>
            </div>

            <div class="chat-sidebar__tools">
              <input
                v-model.trim="chatKeyword"
                class="field"
                placeholder="搜索联系人、物品名称或消息内容"
              />
              <label class="identity-option">
                <input v-model="showUnreadChatsOnly" type="checkbox" />
                只看未读
              </label>
            </div>

            <div v-if="currentConversation" class="chat-current-card">
              <div class="chat-current-card__label">当前会话</div>
              <button type="button" class="chat-conversation-card active current" @click="openConversation(currentConversation)">
                <div class="chat-conversation-card__body">
                  <div class="chat-conversation-card__identity">
                    <AvatarView :name="currentConversation.counterpartLabel" :avatar-url="currentConversation.counterpartAvatarUrl" />
                    <div class="chat-conversation-card__identity-text">
                      <strong>{{ currentConversation.counterpartLabel }}</strong>
                      <span>{{ currentConversation.itemTitle }}</span>
                    </div>
                  </div>
                  <div class="chat-conversation-card__top">
                    <span>{{ shortTime(currentConversation.lastTime) }}</span>
                  </div>
                  <div class="chat-conversation-card__preview">{{ currentConversation.lastMessage || '暂无消息' }}</div>
                  <div class="chat-conversation-card__foot">
                    <span>{{ conversationStatusText(currentConversation) }}</span>
                    <em v-if="currentConversation.unreadCount" class="top-nav__badge">{{ currentConversation.unreadCount }}</em>
                  </div>
                </div>
              </button>
            </div>

            <div v-if="groupedConversations.length" class="chat-group-list">
              <section v-for="group in groupedConversations" :key="group.itemId" class="chat-group-card">
                <button type="button" class="chat-group-card__header" @click="toggleItemGroup(group.itemId)">
                  <div class="chat-group-card__title">
                    <strong>{{ group.itemTitle }}</strong>
                    <span>{{ group.location || group.itemId }}</span>
                  </div>
                  <div class="chat-group-card__meta">
                    <span v-if="group.unreadCount" class="top-nav__badge">{{ group.unreadCount }}</span>
                    <span>{{ isGroupExpanded(group.itemId) ? '收起' : '展开' }}</span>
                  </div>
                </button>

                <div v-if="isGroupExpanded(group.itemId)" class="chat-conversation-list">
                  <button
                    v-for="item in group.conversations"
                    :key="`${item.itemId}-${item.counterpartUserId}`"
                    type="button"
                    class="chat-conversation-card"
                    :class="{ active: isCurrentConversation(item) }"
                    @click="openConversation(item)"
                  >
                    <div class="chat-conversation-card__body">
                      <div class="chat-conversation-card__identity">
                        <AvatarView :name="item.counterpartLabel" :avatar-url="item.counterpartAvatarUrl" />
                        <div class="chat-conversation-card__identity-text">
                          <strong>{{ item.counterpartLabel }}</strong>
                          <span>{{ item.itemTitle }}</span>
                        </div>
                      </div>
                      <div class="chat-conversation-card__top">
                        <span>{{ shortTime(item.lastTime) }}</span>
                      </div>
                      <div class="chat-conversation-card__preview">{{ item.lastMessage || '暂无消息' }}</div>
                      <div class="chat-conversation-card__foot">
                        <span>{{ conversationStatusText(item) }}</span>
                        <em v-if="item.unreadCount" class="top-nav__badge">{{ item.unreadCount }}</em>
                      </div>
                    </div>
                  </button>
                </div>
              </section>
            </div>

            <div v-else class="empty-chat-card">当前没有匹配的会话。</div>
          </aside>

          <section class="chat-main-panel">
            <div v-if="chatThread && currentConversation" class="chat-main-panel__header chat-main-panel__header--with-avatar">
              <div class="chat-main-panel__identity">
                <AvatarView :name="chatThread.counterpartName" :avatar-url="chatThread.counterpartAvatarUrl" large />
                <div>
                  <strong>{{ chatThread.counterpartName }}</strong>
                  <span>{{ currentConversation.itemTitle }}</span>
                </div>
              </div>
              <button class="table-action" type="button" @click="goItemDetail">查看物品</button>
            </div>

            <div v-if="chatError" class="inline-message error">
              {{ chatError }}
            </div>

            <div v-if="chatThread && timelineMessages.length" ref="messageScroller" class="chat-main-panel__messages">
              <template v-for="entry in timelineMessages" :key="entry.key">
                <div v-if="entry.kind === 'divider'" class="chat-time-divider">
                  <span>{{ entry.label }}</span>
                </div>
                <article v-else class="chat-message chat-message--commerce" :class="{ mine: entry.message.mine }">
                  <AvatarView
                    class="chat-message__avatar"
                    :name="entry.message.senderName"
                    :avatar-url="entry.message.senderAvatarUrl"
                  />
                  <div class="chat-message__content">
                    <div class="chat-message__meta">
                      <strong>{{ entry.message.senderName }}</strong>
                      <span>{{ entry.message.time }}</span>
                    </div>
                    <div class="chat-message__bubble">{{ entry.message.content }}</div>
                  </div>
                </article>
              </template>
            </div>

            <div v-else-if="currentConversation" class="empty-chat-card">还没有聊天记录，可以直接发送第一条消息。</div>
            <div v-else class="empty-chat-card">先从左侧选择一个会话。</div>

            <div v-if="currentConversation" class="chat-main-panel__composer">
              <textarea
                v-model="chatDraft"
                class="textarea-field"
                placeholder="输入物品特征、约见地点、领取方式等内容"
                @keydown="handleComposerKeydown"
              ></textarea>
              <div class="action-row" style="justify-content: flex-end; margin-top: 0;">
                <button class="primary-button" type="button" :disabled="chatSubmitting" @click="submitChatMessage">
                  {{ chatSubmitting ? '发送中...' : '发送消息' }}
                </button>
              </div>
            </div>
          </section>

          <aside class="chat-detail-panel">
            <div v-if="currentConversation" class="chat-detail-card">
              <span class="chat-detail-card__label">当前沟通物品</span>
              <div class="chat-item-card">
                <div class="chat-item-card__media">
                  <img
                    v-if="currentItemDetail?.imageUrl"
                    :src="toBackendAssetUrl(currentItemDetail.imageUrl)"
                    :alt="currentConversation.itemTitle"
                  />
                  <span v-else>暂无图片</span>
                </div>
                <div class="chat-item-card__body">
                  <strong>{{ currentConversation.itemTitle }}</strong>
                  <div class="chat-item-card__status-row">
                    <span v-if="currentItemDetail?.statusLabel" class="status-pill" :class="currentItemDetail.statusClass">
                      {{ currentItemDetail.statusLabel }}
                    </span>
                    <span class="chat-item-card__type">{{ currentItemDetail?.typeLabel || '-' }}</span>
                  </div>
                  <div class="side-list">
                    <div><strong>物品编号：</strong>{{ currentConversation.itemId }}</div>
                    <div class="chat-detail-person">
                      <strong>对话对象：</strong>
                      <div class="chat-detail-person__body">
                        <AvatarView :name="currentConversation.counterpartLabel" :avatar-url="currentConversation.counterpartAvatarUrl" />
                        <span>{{ currentConversation.counterpartLabel }}</span>
                      </div>
                    </div>
                    <div><strong>地点：</strong>{{ currentItemDetail?.location || '-' }}</div>
                    <div><strong>时间：</strong>{{ currentItemDetail?.time || '-' }}</div>
                    <div><strong>最新消息：</strong>{{ currentConversation.lastTime || '-' }}</div>
                  </div>
                </div>
              </div>
              <div class="action-row" style="justify-content: flex-start;">
                <button class="ghost-button" type="button" @click="goItemDetail">打开详情页</button>
              </div>
            </div>
            <div v-else class="empty-chat-card">选择会话后，这里会显示当前物品信息。</div>
          </aside>
        </div>
      </template>

      <template v-else>
        <div class="message-toolbar">
          <label class="identity-option">
            <input v-model="showUnreadOnly" type="checkbox" />
            只看未读
          </label>
          <button class="ghost-button" type="button" :disabled="!systemSummary.unreadCount" @click="handleReadAll">
            全部标为已读
          </button>
        </div>

        <div v-if="filteredMessages.length" class="message-list-simple">
          <article
            v-for="item in filteredMessages"
            :key="item.id"
            class="message-list-card"
            :class="{ unread: !item.read }"
          >
            <div class="message-list-card__main">
              <div class="message-list-card__meta">
                <span class="status-pill" :class="messageTypeClass(item.type)">{{ messageTypeLabel(item.type) }}</span>
                <span>{{ item.time }}</span>
              </div>
              <div>{{ item.content }}</div>
            </div>
            <div class="claim-actions-cell">
              <button v-if="!item.read" class="table-action" type="button" @click="handleRead(item.id)">标为已读</button>
              <button class="table-action" type="button" @click="handleOpenNotice(item)">查看</button>
            </div>
          </article>
        </div>
        <div v-else class="empty-chat-card">当前没有符合条件的系统通知。</div>
      </template>
    </section>
  </PublicLayout>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getChatConversations, getChatSummary } from '../api/chat'
import { emitUnreadChanged, toBackendAssetUrl } from '../api/client'
import { getMessages, getMessageSummary, markAllMessagesRead, markMessageRead } from '../api/message'
import { getItemChatMessages, getMyItems, searchItems, sendItemChatMessage } from '../api/item'
import { normalizeItem } from '../utils/items'
import { safeAlert } from '../utils/ui'

const AvatarView = defineComponent({
  name: 'AvatarView',
  props: {
    avatarUrl: { type: String, default: '' },
    name: { type: String, default: '' },
    large: { type: Boolean, default: false },
  },
  setup(props) {
    const label = computed(() => String(props.name || '').trim().slice(0, 1).toUpperCase() || 'U')
    return () => h('div', { class: ['avatar-view', props.large && 'avatar-view--large'] }, [
      props.avatarUrl
        ? h('img', { src: toBackendAssetUrl(props.avatarUrl), alt: props.name || 'avatar' })
        : h('span', label.value),
    ])
  },
})

const router = useRouter()
const route = useRoute()
const activeTab = ref(route.query.itemId ? 'chats' : 'notices')
const showUnreadOnly = ref(false)
const showUnreadChatsOnly = ref(false)
const chatKeyword = ref('')
const systemMessages = ref([])
const chatConversations = ref([])
const chatThread = ref(null)
const currentConversationKey = ref('')
const expandedItemGroups = ref([])
const chatDraft = ref('')
const chatSubmitting = ref(false)
const chatError = ref('')
const messageScroller = ref(null)
const systemSummary = reactive({ totalCount: 0, unreadCount: 0 })
const chatSummary = reactive({ totalCount: 0, unreadCount: 0 })
const itemMap = ref({})

const filteredMessages = computed(() => (
  showUnreadOnly.value
    ? systemMessages.value.filter((item) => !item.read)
    : systemMessages.value
))

const sortedConversations = computed(() =>
  [...chatConversations.value].sort((left, right) => {
    const unreadDiff = (right.unreadCount || 0) - (left.unreadCount || 0)
    if (unreadDiff !== 0) return unreadDiff
    return String(right.lastTime || '').localeCompare(String(left.lastTime || ''))
  })
)

const filteredConversations = computed(() => {
  const keyword = chatKeyword.value.toLowerCase()
  return sortedConversations.value.filter((item) => {
    if (showUnreadChatsOnly.value && !(item.unreadCount > 0)) return false
    if (!keyword) return true
    return [item.counterpartLabel, item.itemTitle, item.lastMessage, item.itemId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  })
})

const groupedConversations = computed(() => {
  const groups = []
  const map = new Map()
  for (const conversation of filteredConversations.value) {
    if (!map.has(conversation.itemId)) {
      const itemDetail = itemMap.value[conversation.itemId]
      const group = {
        itemId: conversation.itemId,
        itemTitle: conversation.itemTitle,
        location: itemDetail?.location || '',
        unreadCount: 0,
        conversations: [],
      }
      map.set(conversation.itemId, group)
      groups.push(group)
    }
    const group = map.get(conversation.itemId)
    group.conversations.push(conversation)
    group.unreadCount += conversation.unreadCount || 0
  }
  return groups
})

const currentConversation = computed(() =>
  chatConversations.value.find((item) => conversationKey(item) === currentConversationKey.value) || null
)

const currentItemDetail = computed(() =>
  currentConversation.value ? itemMap.value[currentConversation.value.itemId] || null : null
)

const timelineMessages = computed(() => {
  if (!chatThread.value?.messages?.length) return []
  const result = []
  let previousDay = ''
  chatThread.value.messages.forEach((message, index) => {
    const day = String(message.time || '').slice(0, 10)
    if (day && day !== previousDay) {
      result.push({ kind: 'divider', key: `divider-${day}-${index}`, label: day })
      previousDay = day
    }
    result.push({ kind: 'message', key: `message-${message.id}`, message })
  })
  return result
})

function conversationKey(item) {
  return `${item.itemId}-${item.counterpartUserId}`
}

function isCurrentConversation(item) {
  return conversationKey(item) === currentConversationKey.value
}

function isGroupExpanded(itemId) {
  return expandedItemGroups.value.includes(itemId)
}

function toggleItemGroup(itemId) {
  if (isGroupExpanded(itemId)) {
    expandedItemGroups.value = expandedItemGroups.value.filter((value) => value !== itemId)
    return
  }
  expandedItemGroups.value = [...expandedItemGroups.value, itemId]
}

function ensureGroupExpanded(itemId) {
  if (itemId && !isGroupExpanded(itemId)) {
    expandedItemGroups.value = [...expandedItemGroups.value, itemId]
  }
}

function syncExpandedGroups() {
  const preferred = groupedConversations.value
    .filter((group) => group.unreadCount > 0)
    .map((group) => group.itemId)
  const currentItemId = currentConversation.value?.itemId
  if (currentItemId && !preferred.includes(currentItemId)) {
    preferred.unshift(currentItemId)
  }
  expandedItemGroups.value = [...new Set(preferred)].filter((itemId) =>
    groupedConversations.value.some((group) => group.itemId === itemId)
  )
}

function conversationStatusText(item) {
  if (item.unreadCount > 0) return `${item.unreadCount} 条未读`
  return '最近已读'
}

function messageTypeLabel(type) {
  if (type === 'ANNOUNCEMENT') return '公告'
  if (type === 'CLAIM') return '认领'
  if (type === 'MATCH') return '匹配'
  if (type === 'REVIEW') return '审核'
  if (type === 'CHAT') return '对话'
  return type || '通知'
}

function messageTypeClass(type) {
  if (type === 'ANNOUNCEMENT' || type === 'MATCH') return 'success'
  if (type === 'CLAIM') return 'pending'
  if (type === 'REVIEW') return 'danger'
  return 'pending'
}

function shortTime(value) {
  if (!value) return '-'
  return value.slice(5, 16)
}

async function loadItems() {
  const [publicItems, myItems] = await Promise.all([
    searchItems({}),
    getMyItems().catch(() => []),
  ])
  const merged = [...publicItems, ...myItems].map(normalizeItem)
  itemMap.value = merged.reduce((accumulator, item) => {
    accumulator[item.id] = item
    return accumulator
  }, {})
}

async function loadData() {
  try {
    const [messages, messageSummary, conversations, conversationSummary] = await Promise.all([
      getMessages(),
      getMessageSummary(),
      getChatConversations(),
      getChatSummary(),
    ])
    systemMessages.value = messages
    chatConversations.value = conversations
    systemSummary.totalCount = messageSummary.totalCount || 0
    systemSummary.unreadCount = messageSummary.unreadCount || 0
    chatSummary.totalCount = conversationSummary.totalCount || 0
    chatSummary.unreadCount = conversationSummary.unreadCount || 0
    pickInitialConversation()
  } catch (error) {
    safeAlert(error.message)
  }
}

function pickInitialConversation() {
  if (!chatConversations.value.length) {
    currentConversationKey.value = ''
    chatThread.value = null
    return
  }
  const itemId = typeof route.query.itemId === 'string' ? route.query.itemId : ''
  const counterpartUserId = Number(route.query.counterpartUserId || 0) || null
  const queried = chatConversations.value.find((item) =>
    item.itemId === itemId && (!counterpartUserId || item.counterpartUserId === counterpartUserId)
  )
  const target = queried
    || chatConversations.value.find((item) => conversationKey(item) === currentConversationKey.value)
    || sortedConversations.value[0]
  currentConversationKey.value = target ? conversationKey(target) : ''
  if (target) ensureGroupExpanded(target.itemId)
}

async function scrollMessagesToBottom() {
  await nextTick()
  if (messageScroller.value) {
    messageScroller.value.scrollTop = messageScroller.value.scrollHeight
  }
}

async function loadCurrentThread() {
  if (!currentConversation.value) {
    chatThread.value = null
    return
  }
  chatError.value = ''
  try {
    chatThread.value = await getItemChatMessages(
      currentConversation.value.itemId,
      currentConversation.value.counterpartUserId
    )
    emitUnreadChanged()
    await scrollMessagesToBottom()
  } catch (error) {
    chatThread.value = null
    chatError.value = error.message || '加载聊天记录失败'
  }
}

async function handleRead(id) {
  try {
    await markMessageRead(id)
    emitUnreadChanged()
    await loadData()
  } catch (error) {
    safeAlert(error.message)
  }
}

async function handleReadAll() {
  try {
    await markAllMessagesRead()
    emitUnreadChanged()
    await loadData()
  } catch (error) {
    safeAlert(error.message)
  }
}

async function handleOpenNotice(item) {
  if (!item.read) {
    await handleRead(item.id)
  }
  if (item.targetPath) {
    router.push(item.targetPath)
    return
  }
  if (item.type === 'MATCH') {
    router.push('/search')
    return
  }
  if (item.type === 'CHAT') {
    router.push('/messages')
    return
  }
  router.push('/user-center')
}

function openConversation(item) {
  currentConversationKey.value = conversationKey(item)
  ensureGroupExpanded(item.itemId)
  activeTab.value = 'chats'
  router.replace({
    path: '/messages',
    query: {
      itemId: item.itemId,
      counterpartUserId: item.counterpartUserId,
    },
  })
}

function goItemDetail() {
  if (!currentConversation.value) return
  router.push({
    path: '/item-detail',
    query: {
      id: currentConversation.value.itemId,
      counterpartUserId: currentConversation.value.counterpartUserId,
    },
  })
}

async function submitChatMessage() {
  if (!currentConversation.value) return
  if (!chatDraft.value.trim()) {
    chatError.value = '请输入消息内容'
    return
  }
  chatSubmitting.value = true
  chatError.value = ''
  try {
    await sendItemChatMessage(currentConversation.value.itemId, {
      counterpartUserId: currentConversation.value.counterpartUserId,
      content: chatDraft.value.trim(),
    })
    chatDraft.value = ''
    emitUnreadChanged()
    await loadData()
    await loadCurrentThread()
  } catch (error) {
    chatError.value = error.message || '发送消息失败'
  } finally {
    chatSubmitting.value = false
  }
}

function handleComposerKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  if (!chatSubmitting.value) {
    submitChatMessage()
  }
}

watch(
  () => currentConversationKey.value,
  () => {
    if (activeTab.value === 'chats') {
      loadCurrentThread()
    }
  }
)

watch(
  () => route.query,
  () => {
    if (route.query.itemId) {
      activeTab.value = 'chats'
      pickInitialConversation()
      loadCurrentThread()
    }
  }
)

watch(
  () => filteredConversations.value.map((item) => item.itemId).join(','),
  () => {
    if (!currentConversation.value && filteredConversations.value.length) {
      pickInitialConversation()
    }
    syncExpandedGroups()
  }
)

onMounted(async () => {
  await Promise.all([loadItems(), loadData()])
  if (activeTab.value === 'chats') {
    await loadCurrentThread()
  }
})
</script>

<style scoped>
.message-shell {
  padding: 30px;
}

.avatar-view {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
  display: grid;
  place-items: center;
  color: #173854;
  font-weight: 700;
  flex: 0 0 42px;
}

.avatar-view img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-view--large {
  width: 54px;
  height: 54px;
  flex-basis: 54px;
}

.message-center-head {
  align-items: stretch;
}

.message-summary-cards {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.message-summary-cards .stat-card {
  min-height: 148px;
  display: grid;
  align-content: space-between;
}

.message-summary-cards .stat-card strong {
  font-size: 38px;
}

.chat-workbench {
  grid-template-columns: 320px minmax(0, 1.2fr) 300px;
  gap: 20px;
  align-items: stretch;
}

.chat-sidebar,
.chat-detail-card {
  position: sticky;
  top: 110px;
}

.chat-sidebar {
  padding: 18px;
}

.chat-conversation-card__body {
  width: 100%;
  display: grid;
  gap: 10px;
}

.chat-conversation-card__identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-conversation-card__identity-text {
  display: grid;
  gap: 2px;
  text-align: left;
}

.chat-conversation-card__identity-text span {
  color: #607285;
  font-size: 12px;
}

.chat-conversation-card__preview {
  min-height: 42px;
}

.chat-main-panel__header--with-avatar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-main-panel__identity {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chat-main-panel__identity > div {
  display: grid;
  gap: 4px;
}

.chat-main-panel__identity span {
  color: #607285;
  font-size: 13px;
}

.chat-main-panel__messages {
  padding-right: 14px;
}

.chat-message {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.chat-message.mine {
  flex-direction: row-reverse;
}

.chat-message__content {
  max-width: min(72%, 560px);
}

.chat-message__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.chat-message.mine .chat-message__meta {
  justify-content: flex-end;
}

.chat-message__avatar {
  margin-bottom: 2px;
}

.chat-message__bubble {
  box-shadow: 0 10px 24px rgba(74, 49, 24, 0.06);
}

.chat-detail-person {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-detail-person__body {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1200px) {
  .chat-workbench {
    grid-template-columns: 1fr;
  }

  .chat-sidebar,
  .chat-detail-card {
    position: static;
  }
}
</style>
