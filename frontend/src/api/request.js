import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      if (data.code === 401) {
        useUserStore().logout()
        router.push('/login')
      }
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  err => {
    ElMessage.error(err.response?.data?.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
