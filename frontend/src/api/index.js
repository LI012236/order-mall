import request from './request'

// ---------- 认证
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getMe = () => request.get('/auth/me')

// ---------- 商品
export const listProducts = (params) => request.get('/products', { params })
export const getProduct = (id) => request.get(`/products/${id}`)

// ---------- 购物车
export const getCart = () => request.get('/cart')
export const addCart = (data) => request.post('/cart', data)
export const updateCart = (id, data) => request.put(`/cart/${id}`, data)
export const removeCart = (id) => request.delete(`/cart/${id}`)

// ---------- 订单
export const createOrder = (data) => request.post('/orders', data)
export const listOrders = (params) => request.get('/orders', { params })
export const getOrderDetail = (orderNo) => request.get(`/orders/${orderNo}`)
export const payOrder = (orderNo) => request.post(`/orders/${orderNo}/pay`)
export const cancelOrder = (orderNo) => request.post(`/orders/${orderNo}/cancel`)
export const confirmOrder = (orderNo) => request.post(`/orders/${orderNo}/confirm`)

// ---------- 后台
export const adminListProducts = (params) => request.get('/admin/products', { params })
export const adminCreateProduct = (data) => request.post('/admin/products', data)
export const adminUpdateProduct = (id, data) => request.put(`/admin/products/${id}`, data)
export const adminOfflineProduct = (id) => request.delete(`/admin/products/${id}`)
export const adminListOrders = (params) => request.get('/admin/orders', { params })
export const adminShipOrder = (orderNo) => request.put(`/admin/orders/${orderNo}/ship`)
