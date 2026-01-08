import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

// API 基礎 URL（從環境變數讀取，開發環境使用 .env.development 設定）
// 預設值為 localhost:8080（當環境變數未設定時使用）
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 創建 Axios 實例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true, // 啟用 Cookie 支援（用於 Refresh Token）
  headers: {
    'Content-Type': 'application/json',
  },
})

// 是否正在刷新 Token 的標記
let isRefreshing = false
// 等待刷新完成的請求隊列
let failedQueue = []

// 處理等待隊列中的請求
const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Request 攔截器：自動添加 Access Token
apiClient.interceptors.request.use(
  (config) => {
    // 嘗試從 authStore 獲取 accessToken
    try {
      const authStore = useAuthStore()
      if (authStore.accessToken) {
        config.headers.Authorization = `Bearer ${authStore.accessToken}`
      }
    } catch (error) {
      // authStore 尚未初始化時忽略錯誤
      console.warn('AuthStore not available:', error)
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response 攔截器：處理 401 錯誤並自動刷新 Token
apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  async (error) => {
    const originalRequest = error.config

    // 如果是 401 錯誤且尚未重試過
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 如果正在刷新 Token，將請求加入隊列
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return apiClient(originalRequest)
          })
          .catch((err) => {
            return Promise.reject(err)
          })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const authStore = useAuthStore()
        
        // 調用刷新 Token 端點（Refresh Token 會自動從 Cookie 發送）
        const response = await axios.post(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        )

        const { accessToken } = response.data
        
        // 更新 authStore 中的 accessToken
        authStore.setAccessToken(accessToken)
        
        // 更新原始請求的 Authorization header
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        
        // 處理等待隊列
        processQueue(null, accessToken)
        isRefreshing = false
        
        // 重試原始請求
        return apiClient(originalRequest)
      } catch (refreshError) {
        // 刷新 Token 失敗，清除認證狀態並導向登入頁
        processQueue(refreshError, null)
        isRefreshing = false
        
        const authStore = useAuthStore()
        authStore.logout()
        
        // 導向登入頁（避免無限循環）
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
        
        return Promise.reject(refreshError)
      }
    }

    // 處理其他錯誤
    return Promise.reject(error)
  }
)

export default apiClient

