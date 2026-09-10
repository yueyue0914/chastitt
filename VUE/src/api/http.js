import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('yue-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  },
)

export default api
