<template>
  <div class="app">
    <header class="header">
      <div class="logo" @click="$router.push('/products')">订单商城</div>
      <nav class="nav" v-if="token">
        <router-link to="/products">商品</router-link>
        <router-link to="/cart">购物车</router-link>
        <router-link to="/orders">我的订单</router-link>
        <router-link v-if="isAdmin" to="/admin">后台管理</router-link>
      </nav>
      <div class="user" v-if="token">
        <span>{{ nickname }}</span>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </div>
      <div class="user" v-else>
        <router-link to="/login">登录 / 注册</router-link>
      </div>
    </header>
    <main class="main">
      <router-view :key="$route.fullPath" />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// localStorage 不是响应式的，用 tick 触发重新计算
const tick = ref(0)
watch(() => route.fullPath, () => { tick.value++ })

const token = computed(() => (tick.value, localStorage.getItem('token')))
const user = computed(() => {
  tick.value
  return JSON.parse(localStorage.getItem('user') || 'null')
})
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const nickname = computed(() => user.value?.nickname || user.value?.username || '')

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  tick.value++
  router.push('/login')
}
</script>
