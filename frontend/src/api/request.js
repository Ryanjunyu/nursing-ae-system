import axios from 'axios'
import { ElMessage } from 'element-plus'
import { auth } from '../store/auth'

const request = axios.create({ baseURL: '/api', timeout: 10000 })

request.interceptors.request.use((config) => {
  const t = auth.getToken()
  if (t) config.headers.Authorization = 'Bearer ' + t
  return config
})

request.interceptors.response.use(
  (resp) => {
    const data = resp.data
    // 后端统一返回 { code, message, data }
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      auth.clear()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error((error.response && error.response.data && error.response.data.message) || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
