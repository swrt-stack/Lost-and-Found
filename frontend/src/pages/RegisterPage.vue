<template>
  <PublicLayout>
    <section class="form-card">
      <h1 class="page-heading">User Registration</h1>
      <div class="form-grid">
        <div class="form-row">
          <label>Username:</label>
          <input v-model.trim="form.username" class="field" placeholder="Enter username" />
        </div>
        <div class="form-row">
          <label>Phone:</label>
          <input v-model.trim="form.phone" class="field" placeholder="Enter phone number" />
        </div>
        <div class="form-row">
          <label>Password:</label>
          <input v-model="form.password" class="field" type="password" placeholder="Enter password" />
        </div>
        <div class="form-row">
          <label>Confirm Password:</label>
          <input v-model="form.confirmPassword" class="field" type="password" placeholder="Confirm password" />
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
          <button class="primary-button" :disabled="submitting" @click="handleRegister">
            {{ submitting ? 'Registering...' : 'Register' }}
          </button>
        </div>
        <div style="text-align: center; color: var(--muted);">
          Already have an account?
          <RouterLink to="/login" style="color: var(--primary);">Back to login</RouterLink>
        </div>
      </div>
    </section>
  </PublicLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PublicLayout from '../components/PublicLayout.vue'
import { getCaptcha, register } from '../api/auth'
import { safeAlert } from '../utils/ui'

const router = useRouter()
const submitting = ref(false)
const captchaLoading = ref(false)
const form = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
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

async function handleRegister() {
  if (!form.username || !form.phone || !form.password || !form.captchaId || !form.captchaCode) {
    safeAlert('Please fill in username, phone, password, and captcha')
    return
  }
  if (!/^1\\d{10}$/.test(form.phone)) {
    safeAlert('Please enter a valid 11-digit mobile number')
    return
  }
  if (form.password !== form.confirmPassword) {
    safeAlert('The two passwords do not match')
    return
  }
  submitting.value = true
  try {
    await register({
      username: form.username,
      phone: form.phone,
      password: form.password,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
    })
    safeAlert('Registration successful, please sign in')
    router.push('/login')
  } catch (error) {
    safeAlert(error.message)
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}
</script>
