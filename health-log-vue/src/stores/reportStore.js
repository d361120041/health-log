import { ref } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/services/apiClient'

/**
 * 報表 Store
 * 管理報表數據查詢
 * 支援 NUMBER, ENUM, TEXT 三種類型的報表
 */
export const useReportStore = defineStore('report', () => {
  // State
  const trendData = ref([]) // NUMBER 趨勢數據陣列
  const numberReport = ref(null) // NUMBER 完整報表（包含統計）
  const enumDistribution = ref(null) // ENUM 分佈統計
  const enumTrend = ref(null) // ENUM 時間序列趨勢
  const textAnalysis = ref(null) // TEXT 文字分析
  const isLoading = ref(false)
  const error = ref(null)

  // ==================== 共享基礎方法 ====================

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
   * 驗證必填參數
   * @param {Object} params 參數物件
   * @throws {Error} 如果參數不完整
   */
  const validateParams = (params) => {
    if (!params.fieldName || !params.startDate || !params.endDate) {
      throw new Error('fieldName, startDate, and endDate are required')
    }
  }

  /**
   * 構建基礎查詢參數
   * @param {Object} params 原始參數
   * @returns {Object} 格式化後的查詢參數
   */
  const buildBaseQueryParams = (params) => {
    return {
      fieldName: params.fieldName,
      startDate: typeof params.startDate === 'string' 
        ? params.startDate 
        : formatDate(params.startDate),
      endDate: typeof params.endDate === 'string' 
        ? params.endDate 
        : formatDate(params.endDate),
    }
  }

  /**
   * 基礎 API 請求方法
   * @param {string} endpoint API 端點
   * @param {Object} params 查詢參數
   * @returns {Promise<any>} API 響應數據
   */
  const baseApiCall = async (endpoint, params) => {
    isLoading.value = true
    error.value = null
    try {
      validateParams(params)
      const queryParams = buildBaseQueryParams(params)
      
      const response = await apiClient.get(endpoint, {
        params: queryParams,
      })

      return response.data
    } catch (err) {
      error.value = err
      console.error(`Failed to fetch data from ${endpoint}:`, err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  // ==================== NUMBER 類型報表 ====================

  /**
   * 獲取 NUMBER 類型欄位的完整報表（包含趨勢和統計）
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填）
   * @param {string|Date} params.endDate 結束日期（必填）
   * @returns {Promise<Object>}
   */
  const fetchNumberReport = async (params) => {
    const data = await baseApiCall('/reports/number', params)
    numberReport.value = data
    trendData.value = data?.trendData || []
    return data
  }

  /**
   * 獲取趨勢數據（NUMBER 類型，向後兼容）
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填）
   * @param {string|Date} params.endDate 結束日期（必填）
   * @param {boolean} params.includeNulls 是否包含空值（可選，預設 false）
   * @returns {Promise<Array>}
   */
  const fetchTrendData = async (params) => {
    isLoading.value = true
    error.value = null
    try {
      validateParams(params)

      const startDateStr = typeof params.startDate === 'string' 
        ? params.startDate 
        : formatDate(params.startDate)
      const endDateStr = typeof params.endDate === 'string' 
        ? params.endDate 
        : formatDate(params.endDate)

      const queryParams = {
        fieldName: params.fieldName,
        startDate: startDateStr,
        endDate: endDateStr,
      }

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

  // ==================== ENUM 類型報表 ====================

  /**
   * 獲取 ENUM 類型欄位的分佈統計
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填）
   * @param {string|Date} params.endDate 結束日期（必填）
   * @returns {Promise<Object>}
   */
  const fetchEnumDistribution = async (params) => {
    const data = await baseApiCall('/reports/enum/distribution', params)
    enumDistribution.value = data
    return data
  }

  /**
   * 獲取 ENUM 類型欄位的時間序列趨勢
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填）
   * @param {string|Date} params.endDate 結束日期（必填）
   * @returns {Promise<Object>}
   */
  const fetchEnumTrend = async (params) => {
    const data = await baseApiCall('/reports/enum/trend', params)
    enumTrend.value = data
    return data
  }

  // ==================== TEXT 類型報表 ====================

  /**
   * 獲取 TEXT 類型欄位的文字分析報表
   * @param {Object} params 查詢參數
   * @param {string} params.fieldName 欄位名稱（必填）
   * @param {string|Date} params.startDate 開始日期（必填）
   * @param {string|Date} params.endDate 結束日期（必填）
   * @returns {Promise<Object>}
   */
  const fetchTextAnalysis = async (params) => {
    const data = await baseApiCall('/reports/text/analysis', params)
    textAnalysis.value = data
    return data
  }

  // ==================== 工具方法 ====================

  /**
   * 清除趨勢數據
   */
  const clearTrendData = () => {
    trendData.value = []
  }

  /**
   * 清除所有報表數據
   */
  const clearAllData = () => {
    trendData.value = []
    numberReport.value = null
    enumDistribution.value = null
    enumTrend.value = null
    textAnalysis.value = null
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
    numberReport.value = null
    enumDistribution.value = null
    enumTrend.value = null
    textAnalysis.value = null
    isLoading.value = false
    error.value = null
  }

  return {
    // State
    trendData,
    numberReport,
    enumDistribution,
    enumTrend,
    textAnalysis,
    isLoading,
    error,
    // Actions - NUMBER
    fetchTrendData,
    fetchNumberReport,
    // Actions - ENUM
    fetchEnumDistribution,
    fetchEnumTrend,
    // Actions - TEXT
    fetchTextAnalysis,
    // Actions - 工具方法
    clearTrendData,
    clearAllData,
    clearError,
    reset,
  }
})

