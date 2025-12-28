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
   * 獲取當前用戶的所有記錄
   * @returns {Promise<Array>}
   */
  const fetchRecordsList = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/records')
      recordsList.value = response.data || []
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
      recordsList.value = recordsList.value.filter(
        (r) => r.recordDate !== dateStr
      )
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
  }

  return {
    // State
    dailyRecord,
    recordsList,
    isLoading,
    error,
    // Actions
    fetchRecordByDate,
    fetchRecordsList,
    saveRecord,
    deleteRecord,
    clearCurrentRecord,
    clearError,
    reset,
  }
})

