<template>
  <PublicLayout>
    <section class="page-panel">
      <h1 class="page-heading">发布遗失物品信息</h1>
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
            <label>遗失地点</label>
            <input v-model="form.location" class="field" placeholder="请输入校园地点" />
          </div>
          <div class="form-row">
            <label>遗失时间</label>
            <input v-model="form.eventTime" class="field" type="datetime-local" />
          </div>
        </div>

        <div class="form-row">
          <label>物品详细描述</label>
          <textarea v-model="form.description" class="textarea-field" placeholder="请输入更具体的颜色、品牌、特征信息"></textarea>
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
            <label>联系方式</label>
            <input v-model="form.contact" class="field" placeholder="请输入联系方式" />
          </div>
        </div>

        <div v-if="previewUrls.length" class="card-grid">
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
import { useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getCategories } from '../api/category'
import { publishLost } from '../api/item'
import { uploadImage } from '../api/upload'
import { toBackendDateTime } from '../utils/items'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const categories = ref([])
const previewUrls = ref([])
const files = ref([])
const submitting = ref(false)
const form = reactive({
  title: '',
  categoryId: '',
  location: '',
  eventTime: '',
  description: '',
  contact: '',
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
  form.contact = ''
  form.anonymous = false
  files.value = []
  previewUrls.value = []
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
    const result = await publishLost({
      title: form.title,
      description: form.description,
      categoryId: Number(form.categoryId),
      location: form.location,
      eventTime: toBackendDateTime(form.eventTime),
      contact: form.contact || (form.anonymous ? '匿名用户' : ''),
      images: uploadResults.map((item) => item.url).join(','),
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
  } catch (error) {
    safeAlert(error.message)
  }
})
</script>
