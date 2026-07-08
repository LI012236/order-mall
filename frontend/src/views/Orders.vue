<template>
  <div>
    <h2 class="page-title">我的订单</h2>

    <el-tabs v-model="status" @tab-change="load(1)">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="CREATED" />
      <el-tab-pane label="已支付" name="PAID" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <div v-loading="loading">
      <el-table :data="list" border v-if="list.length">
        <el-table-column prop="order_no" label="订单号" width="220" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }"><span class="price">¥{{ row.total_amount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiver_name" label="收货人" width="100" />
        <el-table-column prop="created_at" label="下单时间" min-width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'CREATED'" link type="success" @click="pay(row)">支付</el-button>
            <el-button v-if="row.status === 'SHIPPED'" link type="success" @click="confirm(row)">确认收货</el-button>
            <el-button
              v-if="row.status === 'CREATED' || row.status === 'PAID'"
              link
              type="danger"
              @click="cancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty" v-else-if="!loading">还没有订单</div>
    </div>

    <div class="center">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="620px">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.order.order_no }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.order.status) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.order.receiver_name }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.order.receiver_phone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.order.receiver_address }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" border style="margin-top: 16px">
          <el-table-column prop="product_name" label="商品" min-width="200" />
          <el-table-column label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="110">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, confirmOrder, getOrderDetail, listOrders, payOrder } from '../api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const detailVisible = ref(false)
const detail = ref(null)

const STATUS_TEXT = {
  CREATED: '待支付',
  PAID: '已支付',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}
const STATUS_TYPE = {
  CREATED: 'warning',
  PAID: 'primary',
  SHIPPED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}
const statusText = (s) => STATUS_TEXT[s] || s
const statusType = (s) => STATUS_TYPE[s] || ''

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const data = await listOrders({ page: p, size: size.value, status: status.value || undefined })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  detail.value = await getOrderDetail(row.order_no)
  detailVisible.value = true
}

async function pay(row) {
  await payOrder(row.order_no)
  ElMessage.success('支付成功')
  load()
}

async function cancel(row) {
  await ElMessageBox.confirm('取消后库存会回补，确定取消该订单？', '提示', { type: 'warning' })
  await cancelOrder(row.order_no)
  ElMessage.success('已取消')
  load()
}

async function confirm(row) {
  await confirmOrder(row.order_no)
  ElMessage.success('已确认收货')
  load()
}

onMounted(() => load(1))
</script>
