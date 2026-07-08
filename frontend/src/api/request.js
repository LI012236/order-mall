import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动带 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：解包 {code, message, data}，统一处理错误
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (err) => {
    const status = err.response?.status
    const message = err.response?.data?.message || '网络异常，请稍后重试'

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    ElMessage.error(message)
    return Promise.reject(err)
  }
)

export default request
