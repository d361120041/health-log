import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import router from '@/router'

// API 基礎 URL（從環境變數讀取，開發環境使用 .env.development 設定）
// 預設值為 localhost:8080（當環境變數未設定時使用）
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 創建 Axios 實例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
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

    // ============================================
    // 處理 Timeout 錯誤
    // ============================================
    if (error.code === 'ECONNABORTED' && error.message.includes('timeout')) {
      const message = '請求超時，請檢查網路連線或稍後再試'
      alert(message)
      error.message = message
      console.error('Request timeout:', error.config?.url)
      return Promise.reject(error)
    }

    // ============================================
    // 處理網路錯誤（無法連接到伺服器）
    // ============================================
    if (!error.response && error.request && error.code !== 'ECONNABORTED') {
      const message = '網路連接失敗，請檢查網路連線'
      alert(message)
      error.message = message
      console.error('Network error:', error.config?.url)
      return Promise.reject(error)
    }

    // ============================================
    // 處理 HTTP 錯誤響應
    // ============================================
    if (error.response) {
      const status = error.response.status
      const responseData = error.response.data

      // 500-599: 伺服器錯誤
      if (status >= 500) {
        const message = responseData?.message || '伺服器錯誤，請稍後再試'
        alert(message)
        error.message = message
        console.error('Server error:', status, error.config?.url)
        return Promise.reject(error)
      }

      // 401: 未授權（特殊處理：刷新 Token）
      // 如果是 401 錯誤且尚未重試過
      if (status === 401 && !originalRequest._retry) {
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
          
          // 刷新失敗，顯示錯誤訊息
          const message = '登入已過期，請重新登入'
          alert(message)
          refreshError.message = message

          // 導向登入頁（避免無限循環）
          if (router.currentRoute.value.path !== '/login') {
            router.push('/login')
          }
          
          return Promise.reject(refreshError)
        }
      }

      // 403: 沒有權限
      if (status === 403) {
        const message = responseData?.message || '您沒有權限執行此操作'
        alert(message)
        error.message = message
        console.error('Forbidden:', error.config?.url)
        return Promise.reject(error)
      }

      // 404: 資源不存在
      if (status === 404) {
        const message = responseData?.message || '資源不存在'
        alert(message)
        error.message = message
        console.error('Not found:', error.config?.url)
        return Promise.reject(error)
      }

      // 400: 請求錯誤
      if (status === 400) {
        const message = responseData?.message || '請求參數錯誤'
        alert(message)
        error.message = message
        console.error('Bad request:', error.config?.url)
        return Promise.reject(error)
      }

      // 其他 HTTP 錯誤
      const message = responseData?.message || `請求失敗 (${status})`
      alert(message)
      error.message = message
      console.error('HTTP error:', status, error.config?.url)
      return Promise.reject(error)
    }

    // ============================================
    // 處理其他未知錯誤
    // ============================================
    const message = error.message || '發生未知錯誤'
    alert(message)
    console.error('Unknown error:', error)
    return Promise.reject(error)
  }
)

export default apiClient

