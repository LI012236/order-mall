<template>
  <div>
    <h2 class="page-title">购物车</h2>

    <div v-loading="loading">
      <el-table :data="list" border style="width: 100%" v-if="list.length">
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-checkbox
              :model-value="row.checked === 1"
              @change="(val) => toggleChecked(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="product_name" label="商品" min-width="220" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="180">
          <template #default="{ row }">
            <el-input-number
              :model-value="row.quantity"
              :min="1"
              :max="Math.min(99, row.stock)"
              size="small"
              @change="(val) => changeQuantity(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="140">
          <template #default="{ row }"><span class="price">¥{{ row.amount }}</span></template>
        </el-table-column>
        <el-table-column width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="empty" v-else-if="!loading">购物车还是空的，去挑点东西吧</div>
    </div>

    <div style="display: flex; justify-content: flex-end; align-items: center; gap: 20px; margin-top: 20px">
      <span>
        已选 <b>{{ checkedQuantity }}</b> 件，合计
        <span class="product-price">¥{{ totalAmount }}</span>
      </span>
      <el-button type="primary" size="large" :disabled="checkedQuantity === 0" @click="openCheckout">
        去结算
      </el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="填写收货信息" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="form.receiver_name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.receiver_phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.receiver_address" type="textarea" :rows="2" placeholder="省市区 + 详细地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, getCart, removeCart, updateCart } from '../api'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ receiver_name: '', receiver_phone: '', receiver_address: '' })

const checkedItems = computed(() => list.value.filter((x) => x.checked === 1))
const checkedQuantity = computed(() => checkedItems.value.reduce((s, x) => s + x.quantity, 0))
const totalAmount = computed(() =>
  checkedItems.value.reduce((s, x) => s + Number(x.amount), 0).toFixed(2)
)

async function load() {
  loading.value = true
  try {
    const data = await getCart()
    list.value = data.list
  } finally {
    loading.value = false
  }
}

async function toggleChecked(row, val) {
  await updateCart(row.id, { checked: val ? 1 : 0 })
  row.checked = val ? 1 : 0
  row.amount = (Number(row.price) * row.quantity).toFixed(2)
}

async function changeQuantity(row, val) {
  if (!val) return
  const data = await updateCart(row.id, { quantity: val })
  row.quantity = data.quantity
  row.amount = data.amount
}

async function remove(row) {
  await ElMessageBox.confirm('确定从购物车移除该商品？', '提示', { type: 'warning' })
  await removeCart(row.id)
  ElMessage.success('已删除')
  load()
}

function openCheckout() {
  dialogVisible.value = true
}

async function submitOrder() {
  if (!form.receiver_name || !form.receiver_phone || !form.receiver_address) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  submitting.value = true
  try {
    const order = await createOrder({
      request_id: crypto.randomUUID().replaceAll('-', ''),
      ...form
    })
    dialogVisible.value = false
    ElMessage.success('下单成功：' + order.order_no)
    router.push('/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
