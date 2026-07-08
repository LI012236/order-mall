import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/products' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/products', component: () => import('../views/Products.vue') },
  { path: '/cart', component: () => import('../views/Cart.vue') },
  { path: '/orders', component: () => import('../views/Orders.vue') },
  { path: '/admin', component: () => import('../views/Admin.vue'), meta: { admin: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.meta.admin && user?.role !== 'ADMIN') {
    return '/products'
  }
  if (to.path === '/login' && token) {
    return '/products'
  }
  return true
})

export default router
