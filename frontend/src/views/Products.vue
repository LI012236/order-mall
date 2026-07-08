<template>
  <div>
    <h2 class="page-title">商品列表</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索商品名称" style="width: 220px" clearable @keyup.enter="load(1)" />
      <el-select v-model="query.category" placeholder="全部分类" style="width: 140px" clearable @change="load(1)">
        <el-option label="数码" value="数码" />
        <el-option label="家居" value="家居" />
        <el-option label="配件" value="配件" />
      </el-select>
      <el-button type="primary" @click="load(1)">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div v-loading="loading">
      <div class="product-grid" v-if="list.length">
        <div class="product-card" v-for="p in list" :key="p.id">
          <div class="product-name">{{ p.name }}</div>
          <div class="product-meta">{{ p.category }} · 库存 {{ p.stock }}</div>
          <div class="product-price">¥{{ p.price }}</div>
          <el-button type="primary" size="small" :disabled="p.stock <= 0" @click="addToCart(p)">
            {{ p.stock > 0 ? '加入购物车' : '已售罄' }}
          </el-button>
        </div>
      </div>
      <div class="empty" v-else-if="!loading">没有找到商品</div>
    </div>

    <div class="center">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addCart, listProducts } from '../api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 8, keyword: '', category: '' })

async function load(page = query.page) {
  query.page = page
  loading.value = true
  try {
    const data = await listProducts({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.category = ''
  load(1)
}

async function addToCart(product) {
  await addCart({ product_id: product.id, quantity: 1 })
  ElMessage.success('已加入购物车')
}

onMounted(() => load(1))
</script>
