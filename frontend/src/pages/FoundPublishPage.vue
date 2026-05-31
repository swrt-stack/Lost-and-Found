<template>
  <PublicLayout>
    <section class="page-panel">
      <h1 class="page-heading">发布招领物品信息</h1>
      <div v-if="prefillHint" class="inline-message info" style="margin-bottom: 18px;">
        {{ prefillHint }}
      </div>
      <div class="form-grid">
        <div class="dual-grid">
          <div class="form-row">
            <label>物品名称</label>
            <input v-model="form.title" class="field" placeholder="请输入物品名称" />
          </div>
          <div class="form-row">
            <label>物品分类</label>
            <select v-model="form.categoryId" class="select-field">
              <option value="">请选择分类</option>
              <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </div>
          <div class="form-row">
            <label>捡到地点</label>
            <input v-model="form.location" class="field" placeholder="请输入校园地点" />
          </div>
          <div class="form-row">
            <label>捡到时间</label>
            <input v-model="form.eventTime" class="field" type="datetime-local" />
          </div>
        </div>

        <div class="form-row">
          <label>物品详细描述</label>
          <textarea v-model="form.description" class="textarea-field" placeholder="请补充物品外观、颜色、品牌和拾取场景"></textarea>
        </div>

        <div class="dual-grid">
          <div class="form-row">
            <label>物品图片上传</label>
            <div class="upload-row">
              <input class="field" type="file" multiple accept="image/*" @change="handleFileChange" />
              <span style="color: var(--muted);">支持多张图片上传</span>
            </div>
          </div>
          <div class="form-row">
            <label>领取方式</label>
            <input v-model="form.pickupMethod" class="field" placeholder="请输入领取方式或联系方式" />
          </div>
        </div>

        <div v-if="previewUrls.length || existingPreviewUrls.length" class="card-grid">
          <div v-for="url in existingPreviewUrls" :key="url" class="preview-box" style="min-height: 120px;">
            <img :src="toBackendAssetUrl(url)" alt="existing" style="width: 100%; height: 120px; object-fit: cover; border-radius: 12px;" />
          </div>
          <div v-for="url in previewUrls" :key="url" class="preview-box" style="min-height: 120px;">
            <img :src="url" alt="preview" style="width: 100%; height: 120px; object-fit: cover; border-radius: 12px;" />
          </div>
        </div>

        <div class="form-row">
          <label class="identity-option">
            <input v-model="form.anonymous" type="checkbox" />
            是否匿名展示：匿名
          </label>
        </div>

        <div class="action-row">
          <button class="ghost-button" type="button" @click="resetForm">重置</button>
          <button class="primary-button" :disabled="submitting" @click="submitForm">
            {{ submitting ? '提交中...' : '提交发布' }}
          </button>
        </div>

        <div class="notice-bar">
          提示：提交后信息进入审核队列，等待审核管理员审核，审核通过后公开展示。
        </div>
      </div>
    </section>
  </PublicLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getCategories } from '../api/category'
import { toBackendAssetUrl } from '../api/client'
import { publishFound } from '../api/item'
import { uploadImage } from '../api/upload'
import { toBackendDateTime } from '../utils/items'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const route = useRoute()
const categories = ref([])
const previewUrls = ref([])
const existingPreviewUrls = ref([])
const files = ref([])
const submitting = ref(false)
const prefillHint = ref('')
const form = reactive({
  title: '',
  categoryId: '',
  location: '',
  eventTime: '',
  description: '',
  pickupMethod: '',
  anonymous: false,
})

async function loadCategories() {
  categories.value = await getCategories()
}

function handleFileChange(event) {
  files.value = Array.from(event.target.files || [])
  previewUrls.value = files.value.map((file) => URL.createObjectURL(file))
}

function resetForm() {
  form.title = ''
  form.categoryId = ''
  form.location = ''
  form.eventTime = ''
  form.description = ''
  form.pickupMethod = ''
  form.anonymous = false
  files.value = []
  previewUrls.value = []
  existingPreviewUrls.value = []
  prefillHint.value = ''
}

function applyPrefillFromQuery() {
  const title = typeof route.query.title === 'string' ? route.query.title : ''
  const categoryName = typeof route.query.category === 'string' ? route.query.category : ''
  const location = typeof route.query.location === 'string' ? route.query.location : ''
  const description = typeof route.query.description === 'string' ? route.query.description : ''
  const images = typeof route.query.images === 'string' ? route.query.images : ''
  const fromLostId = typeof route.query.fromLostId === 'string' ? route.query.fromLostId : ''

  if (!title && !categoryName && !location && !description && !images) return

  form.title = title
  form.location = location
  form.description = description
  existingPreviewUrls.value = images
    ? images.split(',').map((item) => item.trim()).filter(Boolean)
    : []
  const matchedCategory = categories.value.find((item) => item.name === categoryName)
  form.categoryId = matchedCategory ? itemIdString(matchedCategory.id) : ''
  prefillHint.value = fromLostId
    ? `已根据遗失信息 ${fromLostId} 预填基础内容，请补充你实际捡到时的时间、图片和领取方式。`
    : '已根据遗失信息预填基础内容，请补充你实际捡到时的时间、图片和领取方式。'
}

function itemIdString(value) {
  return value == null ? '' : String(value)
}

async function submitForm() {
  if (!form.title || !form.categoryId || !form.location || !form.eventTime || !form.description) {
    safeAlert('请完整填写发布信息')
    return
  }
  submitting.value = true
  try {
    const uploadResults = []
    for (const file of files.value) {
      uploadResults.push(await uploadImage(file))
    }
    const images = [
      ...existingPreviewUrls.value,
      ...uploadResults.map((item) => item.url),
    ].filter(Boolean).join(',')
    const result = await publishFound({
      title: form.title,
      description: form.description,
      categoryId: Number(form.categoryId),
      location: form.location,
      eventTime: toBackendDateTime(form.eventTime),
      pickupMethod: form.pickupMethod || (form.anonymous ? '匿名领取方式' : ''),
      images,
    })
    safeAlert(result.result || '发布成功')
    router.push('/user-center')
  } catch (error) {
    safeAlert(error.message)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    await loadCategories()
    applyPrefillFromQuery()
  } catch (error) {
    safeAlert(error.message)
  }
})
</script>
