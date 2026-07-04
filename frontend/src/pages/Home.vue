<template>
  <PublicLayout>
    <section class="hero-card hero-home">
      <div class="hero-home__intro">
        <span class="hero-home__eyebrow">校园失物招领平台</span>
        <h1 class="hero-home__title">失物不滞留，寻物更快捷</h1>
        <p class="hero-home__subtitle">
          输入物品名称、地点或描述关键词，快速检索已公开的遗失与招领信息。
        </p>
      </div>

      <div class="search-bar hero-home__search">
        <input
          v-model="keyword"
          class="field"
          placeholder="请输入物品名称、描述、地点关键词"
          @keyup.enter="() => runSearch()"
        />
        <button class="primary-button" type="button" @click="() => runSearch()">搜索</button>
      </div>

      <div class="chip-row hero-home__chips">
        <span class="hero-home__chip-label">快捷分类</span>
        <button
          v-for="item in categories"
          :key="item.id || item.name"
          class="chip"
          type="button"
          @click="runSearch(item.name)"
        >
          {{ item.name }}
        </button>
      </div>
    </section>

    <section class="dual-grid home-feed">
      <div class="content-card home-section-card">
        <div class="home-section-card__head">
          <div>
            <h2 class="section-title">最新遗失物品</h2>
            <p class="section-subtitle">优先展示最近公开通过审核的遗失信息</p>
          </div>
        </div>

        <div class="card-grid home-item-grid">
          <article v-for="item in latestLostItems" :key="item.id" class="item-card home-item-card">
            <div class="item-card__cover home-item-card__cover">
              <img
                v-if="item.imageUrl"
                :src="toBackendAssetUrl(item.imageUrl)"
                :alt="item.title"
                class="home-item-card__image"
              />
              <span v-else>暂无图片</span>
            </div>
            <div class="item-card__body">
              <div class="meta-list">
                <div><strong>物品名称：</strong>{{ item.title }}</div>
                <div><strong>遗失地点：</strong>{{ item.location }}</div>
                <div><strong>发布时间：</strong>{{ item.time }}</div>
                <div><strong>状态：</strong>{{ item.statusLabel }}</div>
              </div>
              <div class="publisher-inline">
                <div class="publisher-inline__avatar">
                  <img
                    v-if="item.publisherAvatarUrl"
                    :src="toBackendAssetUrl(item.publisherAvatarUrl)"
                    :alt="item.publisher"
                  />
                  <span v-else>{{ avatarText(item.publisher) }}</span>
                </div>
                <span>{{ item.publisher }}</span>
              </div>
              <div class="action-row home-item-card__actions">
                <RouterLink class="primary-button nav-button" :to="{ path: '/item-detail', query: { id: item.id } }">
                  查看详情
                </RouterLink>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div class="content-card home-section-card">
        <div class="home-section-card__head">
          <div>
            <h2 class="section-title">最新招领物品</h2>
            <p class="section-subtitle">帮助失主快速看到最新的招领线索</p>
          </div>
        </div>

        <div class="card-grid home-item-grid">
          <article v-for="item in latestFoundItems" :key="item.id" class="item-card home-item-card">
            <div class="item-card__cover home-item-card__cover">
              <img
                v-if="item.imageUrl"
                :src="toBackendAssetUrl(item.imageUrl)"
                :alt="item.title"
                class="home-item-card__image"
              />
              <span v-else>暂无图片</span>
            </div>
            <div class="item-card__body">
              <div class="meta-list">
                <div><strong>物品名称：</strong>{{ item.title }}</div>
                <div><strong>捡到地点：</strong>{{ item.location }}</div>
                <div><strong>发布时间：</strong>{{ item.time }}</div>
                <div><strong>状态：</strong>{{ item.statusLabel }}</div>
              </div>
              <div class="publisher-inline">
                <div class="publisher-inline__avatar">
                  <img
                    v-if="item.publisherAvatarUrl"
                    :src="toBackendAssetUrl(item.publisherAvatarUrl)"
                    :alt="item.publisher"
                  />
                  <span v-else>{{ avatarText(item.publisher) }}</span>
                </div>
                <span>{{ item.publisher }}</span>
              </div>
              <div class="action-row home-item-card__actions">
                <RouterLink class="primary-button nav-button" :to="{ path: '/item-detail', query: { id: item.id } }">
                  查看详情
                </RouterLink>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <div class="notice-bar home-notice">
      用户发布信息需经过管理员审核通过后方可展示，未审核信息暂不对外公开。
      <span v-if="announcements.length">最新公告：{{ announcements[0].title }}</span>
    </div>
  </PublicLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { toBackendAssetUrl } from '../api/client'
import { getCategories } from '../api/category'
import { keywordSearchItems } from '../api/item'
import { getAnnouncements } from '../api/system'
import { normalizeItem } from '../utils/items'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const keyword = ref('')
const categories = ref([])
const announcements = ref([])
const items = ref([])

const latestLostItems = computed(() => items.value.filter((item) => item.type === 'lost').slice(0, 2))
const latestFoundItems = computed(() => items.value.filter((item) => item.type === 'found').slice(0, 2))

function avatarText(value) {
  const source = String(value || '').trim()
  return source ? source.slice(0, 1).toUpperCase() : 'U'
}

async function loadHomeData() {
  try {
    const [categoryRows, publicRows, announcementRows] = await Promise.all([
      getCategories(),
      keywordSearchItems({}),
      getAnnouncements(),
    ])
    categories.value = categoryRows
    announcements.value = announcementRows
    items.value = publicRows.map(normalizeItem)
  } catch (error) {
    safeAlert(error.message)
  }
}

function runSearch(term) {
  const value = (typeof term === 'string' ? term : keyword.value).trim()
  router.push({ path: '/items', query: value ? { keyword: value } : {} })
}

onMounted(loadHomeData)
</script>

<style scoped>
.hero-home {
  display: grid;
  gap: 22px;
}

.hero-home__intro {
  display: grid;
  gap: 10px;
  max-width: 760px;
}

.hero-home__eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(159, 90, 34, 0.08);
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-home__title {
  margin: 0;
  font-size: 44px;
  line-height: 1.08;
  color: var(--primary-deep);
}

.hero-home__subtitle {
  margin: 0;
  font-size: 17px;
  line-height: 1.8;
  color: var(--muted);
}

.hero-home__search {
  align-items: stretch;
}

.hero-home__chips {
  align-items: center;
  margin-top: 4px;
}

.hero-home__chip-label {
  color: var(--muted);
  font-weight: 700;
}

.home-feed {
  margin-top: 22px;
}

.home-section-card {
  display: grid;
  gap: 20px;
}

.home-section-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.home-item-grid {
  gap: 20px;
}

.home-item-card {
  min-height: 100%;
}

.home-item-card__cover {
  min-height: 200px;
}

.home-item-card__image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.home-item-card__actions {
  justify-content: flex-start;
  margin-top: 18px;
}

.publisher-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  color: #5e6f81;
  font-weight: 600;
}

.publisher-inline__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #d7ebfb, #f8d9b2);
  display: grid;
  place-items: center;
  color: #163652;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(22, 54, 82, 0.12);
}

.publisher-inline__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .hero-home__title {
    font-size: 34px;
  }

  .hero-home__subtitle {
    font-size: 15px;
  }
}
</style>
