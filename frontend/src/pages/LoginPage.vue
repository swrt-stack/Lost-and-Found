<template>
  <PublicLayout>
    <section class="form-card">
      <h1 class="page-heading">System Login</h1>
      <div class="form-grid">
        <div class="form-row">
          <label>Username:</label>
          <input v-model.trim="form.username" class="field" placeholder="Enter username" />
        </div>
        <div class="form-row">
          <label>Password:</label>
          <input v-model="form.password" class="field" type="password" placeholder="Enter password" />
        </div>
        <div class="form-row">
          <label>Captcha:</label>
          <div class="captcha-row">
            <input v-model.trim="form.captchaCode" class="field" style="max-width: 180px;" placeholder="Enter captcha" />
            <button type="button" class="captcha-box" :disabled="captchaLoading" @click="refreshCaptcha">
              <img
                v-if="captcha.imageData"
                :src="captcha.imageData"
                alt="captcha"
                style="height: 44px; width: 132px; object-fit: cover;"
              />
              <span v-else>{{ captchaLoading ? 'Loading...' : 'Refresh' }}</span>
            </button>
          </div>
        </div>
        <div class="action-row">
          <button class="primary-button" :disabled="submitting" @click="handleLogin">
            {{ submitting ? 'Signing in...' : 'Sign in' }}
          </button>
        </div>
        <div style="text-align: center; color: var(--muted);">
          No account?
          <RouterLink to="/register" style="color: var(--primary);">Create one</RouterLink>
        </div>
      </div>
    </section>
  </PublicLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { saveAuthSession } from '../api/client'
import { getCaptcha, login } from '../api/auth'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const captchaLoading = ref(false)
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
})
const captcha = reactive({
  imageData: '',
})

onMounted(() => {
  refreshCaptcha()
})

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const result = await getCaptcha()
    captcha.imageData = result.imageData
    form.captchaId = result.captchaId
    form.captchaCode = ''
  } catch (error) {
    safeAlert(error.message)
  } finally {
    captchaLoading.value = false
  }
}

async function handleLogin() {
  if (!form.username || !form.password || !form.captchaId || !form.captchaCode) {
    safeAlert('Please enter username, password, and captcha')
    return
  }
  submitting.value = true
  try {
    const result = await login({
      username: form.username,
      password: form.password,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
    })
    saveAuthSession(result)
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect) {
      router.push(decodeURIComponent(redirect))
      return
    }
    if (result.role === 'SYS_ADMIN') {
      router.push('/admin-dashboard')
      return
    }
    if (result.role === 'REVIEW_ADMIN') {
      router.push('/review-dashboard')
      return
    }
    router.push('/user-center')
  } catch (error) {
    safeAlert(error.message)
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>
