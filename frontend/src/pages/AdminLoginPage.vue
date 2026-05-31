<template>
  <div class="admin-login-page">
    <section class="admin-login-card">
      <h1 class="page-heading">Admin Console Login</h1>
      <div class="form-grid">
        <div class="form-row">
          <label>Username:</label>
          <input v-model.trim="form.username" class="field" placeholder="Enter admin username" />
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
        <div class="form-row">
          <label>Role:</label>
          <div class="identity-select">
            <label class="identity-option"><input v-model="form.role" type="radio" value="SYS_ADMIN" /> Super Admin</label>
            <label class="identity-option"><input v-model="form.role" type="radio" value="REVIEW_ADMIN" /> Review Admin</label>
          </div>
        </div>
        <div class="action-row">
          <button class="primary-button" :disabled="submitting" @click="handleLogin">
            {{ submitting ? 'Signing in...' : 'Sign in to admin' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, login } from '../api/auth'
import { saveAuthSession } from '../api/client'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const submitting = ref(false)
const captchaLoading = ref(false)
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
  role: 'SYS_ADMIN',
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
    if (!['SYS_ADMIN', 'REVIEW_ADMIN'].includes(result.role)) {
      safeAlert('This account does not have admin access')
      await refreshCaptcha()
      return
    }
    if (result.role !== form.role) {
      safeAlert('Selected role does not match this account')
      await refreshCaptcha()
      return
    }
    saveAuthSession(result)
    router.push(result.role === 'SYS_ADMIN' ? '/admin-dashboard' : '/review-dashboard')
  } catch (error) {
    safeAlert(error.message)
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>
