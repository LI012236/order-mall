<template>
  <div>
    <h2 class="page-title">后台管理</h2>

    <el-tabs v-model="tab">
      <!-- 商品管理 -->
      <el-tab-pane label="商品管理" name="products">
        <div class="toolbar">
          <el-input v-model="productQuery.keyword" placeholder="搜索商品" style="width: 200px" clearable @keyup.enter="loadProducts(1)" />
          <el-button type="primary" @click="loadProducts(1)">搜索</el-button>
          <el-button type="success" @click="openCreate">新增商品</el-button>
        </div>

        <el-table :data="products" border v-loading="productLoading">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="价格" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '已下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 1" link type="danger" @click="offline(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="center">
          <el-pagination background layout="prev, pager, next, total" :total="productTotal"
            :page-size="productQuery.size" :current-page="productQuery.page" @current-change="loadProducts" />
        </div>
      </el-tab-pane>

      <!-- 订单管理 -->
      <el-tab-pane label="订单管理" name="orders">
        <div class="toolbar">
          <el-select v-model="orderStatus" placeholder="全部状态" style="width: 150px" clearable @change="loadOrders(1)">
            <el-option label="待支付" value="CREATED" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" @click="loadOrders(1)">刷新</el-button>
        </div>

        <el-table :data="orders" border v-loading="orderLoading">
          <el-table-column prop="order_no" label="订单号" width="220" />
          <el-table-column prop="user_id" label="用户" width="80" />
          <el-table-column label="金额" width="110">
            <template #default="{ row }">¥{{ row.total_amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiver_name" label="收货人" width="100" />
          <el-table-column prop="created_at" label="下单时间" min-width="170" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PAID'" link type="success" @click="ship(row)">发货</el-button>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="center">
          <el-pagination background layout="prev, pager, next, total" :total="orderTotal"
            :page-size="orderSize" :current-page="orderPage" @current-change="loadOrders" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 商品新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑商品' : '新增商品'" width="480px">
      <el-form :model="editing" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editing.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="editing.category" placeholder="数码 / 家居 / 配件" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editing.price" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="editing.stock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editing.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminCreateProduct,
  adminListOrders,
  adminListProducts,
  adminOfflineProduct,
  adminShipOrder,
  adminUpdateProduct
} from '../api'

const tab = ref('products')

// ---------- 商品
const productLoading = ref(false)
const products = ref([])
const productTotal = ref(0)
const productQuery = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const editing = reactive({ id: null, name: '', category: '', price: 1, stock: 0, description: '' })

const STATUS_TEXT = {
  CREATED: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消'
}
const STATUS_TYPE = {
  CREATED: 'warning', PAID: 'primary', SHIPPED: 'info', COMPLETED: 'success', CANCELLED: 'danger'
}
const statusText = (s) => STATUS_TEXT[s] || s
const statusType = (s) => STATUS_TYPE[s] || ''

async function loadProducts(p = productQuery.page) {
  productQuery.page = p
  productLoading.value = true
  try {
    const data = await adminListProducts({
      page: p, size: productQuery.size, keyword: productQuery.keyword || undefined
    })
    products.value = data.list
    productTotal.value = data.total
  } finally {
    productLoading.value = false
  }
}

function openCreate() {
  Object.assign(editing, { id: null, name: '', category: '', price: 1, stock: 0, description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(editing, {
    id: row.id, name: row.name, category: row.category,
    price: Number(row.price), stock: row.stock, description: row.description || ''
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      name: editing.name,
      category: editing.category,
      price: editing.price,
      stock: editing.stock,
      description: editing.description
    }
    if (editing.id) {
      await adminUpdateProduct(editing.id, payload)
      ElMessage.success('已更新')
    } else {
      await adminCreateProduct(payload)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    loadProducts(editing.id ? productQuery.page : 1)
  } finally {
    saving.value = false
  }
}

async function offline(row) {
  await ElMessageBox.confirm(`确定下架「${row.name}」？下架后前台不可见。`, '提示', { type: 'warning' })
  await adminOfflineProduct(row.id)
  ElMessage.success('已下架')
  loadProducts()
}

// ---------- 订单
const orderLoading = ref(false)
const orders = ref([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderSize = 10
const orderStatus = ref('')

async function loadOrders(p = orderPage.value) {
  orderPage.value = p
  orderLoading.value = true
  try {
    const data = await adminListOrders({
      page: p, size: orderSize, status: orderStatus.value || undefined
    })
    orders.value = data.list
    orderTotal.value = data.total
  } finally {
    orderLoading.value = false
  }
}

async function ship(row) {
  await ElMessageBox.confirm(`确认对订单 ${row.order_no} 发货？`, '提示', { type: 'warning' })
  await adminShipOrder(row.order_no)
  ElMessage.success('已发货')
  loadOrders()
}

onMounted(() => {
  loadProducts(1)
  loadOrders(1)
})
</script>
