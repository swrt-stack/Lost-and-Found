<template>
  <div class="admin-login-page">
    <section class="admin-login-card">
      <h1 class="page-heading">管理后台登录</h1>
      <div class="form-grid">
        <div class="form-row">
          <label>用户名</label>
          <input v-model.trim="form.username" class="field" placeholder="请输入管理员用户名" />
        </div>
        <div class="form-row">
          <label>密码</label>
          <input v-model="form.password" class="field" type="password" placeholder="请输入密码" />
        </div>
        <div class="form-row">
          <label>验证码</label>
          <div class="captcha-row">
            <input v-model.trim="form.captchaCode" class="field" style="max-width: 180px;" placeholder="请输入验证码" />
            <button type="button" class="captcha-box" :disabled="captchaLoading" @click="refreshCaptcha">
              <img
                v-if="captcha.imageData"
                :src="captcha.imageData"
                alt="验证码"
                style="height: 44px; width: 132px; object-fit: cover;"
              />
              <span v-else>{{ captchaLoading ? '加载中…' : '点击刷新' }}</span>
            </button>
          </div>
        </div>
        <div class="form-row">
          <label>登录身份</label>
          <div class="identity-select">
            <label class="identity-option"><input v-model="form.role" type="radio" value="SYS_ADMIN" /> 超级管理员</label>
            <label class="identity-option"><input v-model="form.role" type="radio" value="REVIEW_ADMIN" /> 审核管理员</label>
          </div>
        </div>
        <div class="action-row">
          <button class="primary-button" :disabled="submitting" @click="handleLogin">
            {{ submitting ? '登录中…' : '登录管理后台' }}
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
    safeAlert('请填写用户名、密码和验证码')
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
      safeAlert('该账号没有管理后台访问权限')
      await refreshCaptcha()
      return
    }
    if (result.role !== form.role) {
      safeAlert('所选身份与账号角色不一致')
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
