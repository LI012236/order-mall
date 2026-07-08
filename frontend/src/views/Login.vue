<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <template #header>
        <div style="text-align: center; font-weight: 600; font-size: 16px">订单商城</div>
      </template>

      <el-tabs v-model="tab" stretch>
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="0" @keyup.enter="handleLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
              登录
            </el-button>
            <div class="text-muted" style="margin-top: 12px; text-align: center">
              管理员：admin / admin123
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-width="0">
            <el-form-item>
              <el-input v-model="registerForm.username" placeholder="用户名（3-20 位字母数字）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少 6 位）" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.nickname" placeholder="昵称（可选）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.phone" placeholder="手机号（可选）" size="large" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })

function saveSession(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  ElMessage.success('欢迎，' + (data.user.nickname || data.user.username))
  router.push(data.user.role === 'ADMIN' ? '/admin' : '/products')
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    saveSession(await login(loginForm))
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  loading.value = true
  try {
    saveSession(await register(registerForm))
  } finally {
    loading.value = false
  }
}
</script>
