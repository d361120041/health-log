import { ref } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/services/apiClient'

/**
 * 記錄 Store
 * 管理每日記錄的 CRUD 操作
 */
export const useRecordsStore = defineStore('records', () => {
  // State
  const dailyRecord = ref(null) // 當前記錄（單一記錄）
  const recordsList = ref([]) // 記錄列表
  const isLoading = ref(false)
  const error = ref(null)
  
  // 分頁相關狀態
  const pagination = ref({
    currentPage: 0, // 當前頁碼（從0開始）
    pageSize: 10, // 每頁大小
    totalElements: 0, // 總記錄數
    totalPages: 0, // 總頁數
    first: true, // 是否為第一頁
    last: false, // 是否為最後一頁
    numberOfElements: 0 // 當前頁實際元素數量
  })

  /**
   * 根據日期獲取單日記錄
   * @param {string|Date} date 日期（格式：YYYY-MM-DD 或 Date 物件）
   * @returns {Promise<Object|null>}
   */
  const fetchRecordByDate = async (date) => {
    isLoading.value = true
    error.value = null
    try {
      // 將日期轉換為 YYYY-MM-DD 格式
      const dateStr = typeof date === 'string' ? date : formatDate(date)
      const response = await apiClient.get(`/records/${dateStr}`)
      dailyRecord.value = response.data
      return response.data
    } catch (err) {
      error.value = err
      // 404 表示該日期沒有記錄，這是正常情況
      if (err.response?.status === 404) {
        dailyRecord.value = null
        return null
      }
      console.error(`Failed to fetch record for date ${date}:`, err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 獲取當前用戶的記錄列表（支援分頁）
   * @param {Object} options - 可選的搜尋參數
   * @param {number} options.page - 頁碼（從0開始），預設使用 pagination.currentPage
   * @param {number} options.size - 每頁大小，預設使用 pagination.pageSize
   * @returns {Promise<Array>} 返回記錄列表
   */
  const fetchRecordsList = async (options = {}) => {
    isLoading.value = true
    error.value = null
    try {
      const page = options.page ?? pagination.value.currentPage
      const size = options.size ?? pagination.value.pageSize

      // 構建搜尋請求體
      const searchRequest = {
        specObj: null, // 不設置查詢條件，後端會自動過濾當前用戶的記錄
        pageObj: {
          isPaged: true,
          page: page,
          size: size,
          isSorted: true,
          orders: [
            {
              field: 'recordDate',
              order: 'DESC' // 按日期降序排序
            }
          ]
        }
      }

      const response = await apiClient.post('/records/search', searchRequest)
      
      // 後端返回格式：{ status, message, data: { content, totalElements, ... } }
      const pageData = response.data?.data || {}
      
      // 更新記錄列表
      recordsList.value = pageData.content || []
      
      // 更新分頁信息
      pagination.value = {
        currentPage: pageData.number ?? page,
        pageSize: pageData.size ?? size,
        totalElements: pageData.totalElements ?? 0,
        totalPages: pageData.totalPages ?? 0,
        first: pageData.first ?? true,
        last: pageData.last ?? false,
        numberOfElements: pageData.numberOfElements ?? 0
      }
      
      return recordsList.value
    } catch (err) {
      error.value = err
      console.error('Failed to fetch records list:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 切換到指定頁碼
   * @param {number} page - 頁碼（從0開始）
   */
  const goToPage = async (page) => {
    if (page < 0 || (pagination.value.totalPages > 0 && page >= pagination.value.totalPages)) {
      return
    }
    await fetchRecordsList({ page, size: pagination.value.pageSize })
  }

  /**
   * 切換到下一頁
   */
  const goToNextPage = async () => {
    if (!pagination.value.last) {
      await goToPage(pagination.value.currentPage + 1)
    }
  }

  /**
   * 切換到上一頁
   */
  const goToPrevPage = async () => {
    if (!pagination.value.first) {
      await goToPage(pagination.value.currentPage - 1)
    }
  }

  /**
   * 改變每頁大小
   * @param {number} size - 每頁大小
   */
  const changePageSize = async (size) => {
    await fetchRecordsList({ page: 0, size }) // 改變大小時重置到第一頁
  }

  /**
   * 保存記錄（創建或更新）
   * @param {Object} record 記錄物件
   * @param {string|Date} record.recordDate 記錄日期
   * @param {Object} record.fieldValues 欄位值對應表 { fieldName: value }
   * @returns {Promise<Object>}
   */
  const saveRecord = async (record) => {
    isLoading.value = true
    error.value = null
    try {
      // 確保日期格式正確
      const recordData = {
        recordDate: typeof record.recordDate === 'string' 
          ? record.recordDate 
          : formatDate(record.recordDate),
        fieldValues: record.fieldValues || {},
      }

      const response = await apiClient.post('/records', recordData)
      
      // 更新本地狀態
      dailyRecord.value = response.data
      
      // 更新列表中的記錄（如果存在）
      const index = recordsList.value.findIndex(
        (r) => r.recordDate === response.data?.recordDate
      )
      if (index !== -1) {
        recordsList.value[index] = response.data
      } else {
        // 如果不在列表中，添加到列表開頭
        recordsList.value.unshift(response.data)
      }

      return response.data
    } catch (err) {
      error.value = err
      console.error('Failed to save record:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 刪除記錄
   * @param {string|Date} date 日期（格式：YYYY-MM-DD 或 Date 物件）
   * @returns {Promise<void>}
   */
  const deleteRecord = async (date) => {
    isLoading.value = true
    error.value = null
    try {
      // 將日期轉換為 YYYY-MM-DD 格式
      const dateStr = typeof date === 'string' ? date : formatDate(date)
      await apiClient.delete(`/records/${dateStr}`)
      
      // 從本地狀態中移除
      if (dailyRecord.value?.recordDate === dateStr) {
        dailyRecord.value = null
      }
      
      // 重新載入當前頁（如果當前頁沒有數據了，會自動載入上一頁）
      const currentPage = pagination.value.currentPage
      await fetchRecordsList({ 
        page: currentPage, 
        size: pagination.value.pageSize 
      })
      
      // 如果當前頁沒有數據且不是第一頁，載入上一頁
      if (recordsList.value.length === 0 && currentPage > 0) {
        await goToPage(currentPage - 1)
      }
    } catch (err) {
      error.value = err
      console.error(`Failed to delete record for date ${date}:`, err)
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
   * 清除當前記錄
   */
  const clearCurrentRecord = () => {
    dailyRecord.value = null
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
    dailyRecord.value = null
    recordsList.value = []
    isLoading.value = false
    error.value = null
    pagination.value = {
      currentPage: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: false,
      numberOfElements: 0
    }
  }

  return {
    // State
    dailyRecord,
    recordsList,
    isLoading,
    error,
    pagination, // 新增分頁狀態
    // Actions
    fetchRecordByDate,
    fetchRecordsList,
    saveRecord,
    deleteRecord,
    clearCurrentRecord,
    clearError,
    reset,
    goToPage, // 新增：切換頁碼
    goToNextPage, // 新增：下一頁
    goToPrevPage, // 新增：上一頁
    changePageSize, // 新增：改變每頁大小
  }
})

