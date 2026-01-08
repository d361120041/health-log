import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/services/apiClient'

/**
 * 認證 Store
 * 管理使用者認證狀態和 Token
 */
export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref(null)
  const user = ref(null)

  // Computed
  const isAuthenticated = computed(() => {
    return accessToken.value !== null && accessToken.value !== ''
  })

  /**
   * 解析 JWT Token 獲取用戶資訊
   * @param {string} token JWT Token
   * @returns {Object|null} 用戶資訊或 null
   */
  const parseJwtToken = (token) => {
    try {
      const base64Url = token.split('.')[1]
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      )
      return JSON.parse(jsonPayload)
    } catch (error) {
      console.error('Failed to parse JWT token:', error)
      return null
    }
  }

  /**
   * 從 Token 中提取用戶資訊
   * @param {string} token JWT Token
   */
  const extractUserFromToken = (token) => {
    const payload = parseJwtToken(token)
    if (payload) {
      user.value = {
        id: payload.userId || payload.sub,
        email: payload.email,
        role: payload.role || payload.authorities?.[0]?.replace('ROLE_', ''),
      }
    }
  }

  /**
   * 設定 Access Token
   * @param {string} token Access Token
   */
  const setAccessToken = (token) => {
    accessToken.value = token
    if (token) {
      extractUserFromToken(token)
    } else {
      user.value = null
    }
  }

  /**
   * 使用者註冊
   * @param {Object} registerData 註冊資料 { email, password, confirmPassword }
   * @returns {Promise<void>}
   */
  const register = async (registerData) => {
    try {
      const response = await apiClient.post('/auth/register', registerData)
      return response.data
    } catch (error) {
      console.error('Registration failed:', error)
      throw error
    }
  }

  /**
   * 驗證電子郵件
   * @param {string} token 驗證 Token
   * @returns {Promise<void>}
   */
  const verifyEmail = async (token) => {
    try {
      const response = await apiClient.post('/auth/verify-email', { token })
      return response.data
    } catch (error) {
      console.error('Email verification failed:', error)
      throw error
    }
  }

  /**
   * 使用者登入
   * @param {string} email 電子郵件
   * @param {string} password 密碼
   * @returns {Promise<void>}
   */
  const login = async (email, password) => {
    try {
      const response = await apiClient.post('/auth/login', {
        email,
        password,
      })

      const { accessToken: token } = response.data
      setAccessToken(token)

      return response.data
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    }
  }

  /**
   * 使用者登出
   * @returns {Promise<void>}
   */
  const logout = async () => {
    try {
      // 調用登出 API（清除後端的 Refresh Token）
      await apiClient.post('/auth/logout')
    } catch (error) {
      console.error('Logout API call failed:', error)
      // 即使 API 調用失敗，也要清除本地狀態
    } finally {
      // 清除本地狀態
      accessToken.value = null
      user.value = null
    }
  }

  /**
   * 手動刷新 Access Token
   * @returns {Promise<void>}
   */
  const refreshAccessToken = async () => {
    try {
      const response = await apiClient.post('/auth/refresh')
      const { accessToken: token } = response.data
      setAccessToken(token)
      return response.data
    } catch (error) {
      console.error('Token refresh failed:', error)
      // 刷新失敗時清除狀態
      accessToken.value = null
      user.value = null
      throw error
    }
  }

  /**
   * 初始化：從 localStorage 恢復 Token（可選）
   * 注意：Refresh Token 存在 Cookie 中，不需要手動恢復
   */
  const init = () => {
    // 可以從 localStorage 恢復 accessToken（如果需要的話）
    // const savedToken = localStorage.getItem('accessToken')
    // if (savedToken) {
    //   setAccessToken(savedToken)
    // }
  }

  return {
    // State
    accessToken,
    user,
    // Computed
    isAuthenticated,
    // Actions
    register,
    verifyEmail,
    login,
    logout,
    refreshAccessToken,
    setAccessToken,
    init,
  }
})

