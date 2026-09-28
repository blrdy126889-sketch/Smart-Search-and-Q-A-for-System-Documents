import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE || '',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const raw = localStorage.getItem('docqa_user')
  if (raw) {
    try {
      const token = JSON.parse(raw).token
      if (token) config.headers.Authorization = `Bearer ${token}`
    } catch { /* ignore */ }
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.msg || '请求失败')
        return Promise.reject(new Error(body.msg))
      }
      return body.data
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('docqa_user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(err.response?.data?.msg || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default request
