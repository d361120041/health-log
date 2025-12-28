import { ref } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/services/apiClient'

/**
 * 報表 Store
 * 管理報表數據查詢
 */
export const useReportStore = defineStore('report', () => {
  // State
  const trendData = ref([]) // 趨勢數據陣列
  const isLoading = ref(false)
  const error = ref(null)

  /**
   * 獲取趨勢數據
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填，格式：YYYY-MM-DD）
   * @param {string|Date} params.endDate 結束日期（必填，格式：YYYY-MM-DD）
   * @param {boolean} params.includeNulls 是否包含空值（可選，預設 false）
   * @returns {Promise<Array>}
   */
  const fetchTrendData = async (params) => {
    isLoading.value = true
    error.value = null
    try {
      // 驗證必填參數
      if (!params.fieldName || !params.startDate || !params.endDate) {
        throw new Error('fieldName, startDate, and endDate are required')
      }

      // 格式化日期
      const startDateStr = typeof params.startDate === 'string' 
        ? params.startDate 
        : formatDate(params.startDate)
      const endDateStr = typeof params.endDate === 'string' 
        ? params.endDate 
        : formatDate(params.endDate)

      // 構建查詢參數
      const queryParams = {
        fieldName: params.fieldName,
        startDate: startDateStr,
        endDate: endDateStr,
      }

      // 如果指定了 includeNulls，添加到查詢參數
      if (params.includeNulls !== undefined) {
        queryParams.includeNulls = params.includeNulls
      }

      const response = await apiClient.get('/reports/trend', {
        params: queryParams,
      })

      trendData.value = response.data || []
      return trendData.value
    } catch (err) {
      error.value = err
      console.error('Failed to fetch trend data:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 格式化日期為 YYYY-MM-DD
   * @param {Date|string} date 日期
   * @returns {string} 格式化後的日期字串
   */
  const formatDate = (date) => {
    if (typeof date === 'string') {
      return date
    }
    if (date instanceof Date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    throw new Error('Invalid date format')
  }

  /**
   * 清除趨勢數據
   */
  const clearTrendData = () => {
    trendData.value = []
  }

  /**
   * 清除錯誤狀態
   */
  const clearError = () => {
    error.value = null
  }

  /**
   * 重置 Store 狀態
   */
  const reset = () => {
    trendData.value = []
    isLoading.value = false
    error.value = null
  }

  return {
    // State
    trendData,
    isLoading,
    error,
    // Actions
    fetchTrendData,
    clearTrendData,
    clearError,
    reset,
  }
})

