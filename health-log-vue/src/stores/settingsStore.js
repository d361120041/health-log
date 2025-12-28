import { ref } from 'vue'
import { defineStore } from 'pinia'
import apiClient from '@/services/apiClient'

/**
 * 欄位設定 Store
 * 管理動態表單欄位設定
 */
export const useSettingsStore = defineStore('settings', () => {
  // State
  const fieldSettings = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  /**
   * 獲取所有啟用的欄位設定（公開端點）
   * @returns {Promise<void>}
   */
  const fetchFieldSettings = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/settings/fields')
      fieldSettings.value = response.data || []
    } catch (err) {
      error.value = err
      console.error('Failed to fetch field settings:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 獲取所有欄位設定（包含未啟用的，僅 Admin）
   * @returns {Promise<void>}
   */
  const fetchAllFieldSettings = async () => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/admin/settings/fields')
      fieldSettings.value = response.data || []
    } catch (err) {
      error.value = err
      console.error('Failed to fetch all field settings:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 根據 ID 獲取欄位設定（僅 Admin）
   * @param {number} id 欄位設定 ID
   * @returns {Promise<Object>}
   */
  const fetchFieldSettingById = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get(`/admin/settings/fields/${id}`)
      return response.data
    } catch (err) {
      error.value = err
      console.error(`Failed to fetch field setting ${id}:`, err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 創建欄位設定（僅 Admin）
   * @param {Object} fieldSetting 欄位設定物件
   * @returns {Promise<Object>}
   */
  const createFieldSetting = async (fieldSetting) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.post('/admin/settings/fields', fieldSetting)
      // 重新獲取所有欄位設定以更新列表
      await fetchAllFieldSettings()
      return response.data
    } catch (err) {
      error.value = err
      console.error('Failed to create field setting:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 更新欄位設定（僅 Admin）
   * @param {number} id 欄位設定 ID
   * @param {Object} fieldSetting 欄位設定物件
   * @returns {Promise<Object>}
   */
  const updateFieldSetting = async (id, fieldSetting) => {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.put(`/admin/settings/fields/${id}`, fieldSetting)
      // 更新本地狀態
      const index = fieldSettings.value.findIndex((fs) => fs.settingId === id)
      if (index !== -1) {
        fieldSettings.value[index] = response.data
      }
      return response.data
    } catch (err) {
      error.value = err
      console.error(`Failed to update field setting ${id}:`, err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 刪除欄位設定（軟刪除，僅 Admin）
   * @param {number} id 欄位設定 ID
   * @returns {Promise<void>}
   */
  const deleteFieldSetting = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      await apiClient.delete(`/admin/settings/fields/${id}`)
      // 從本地狀態中移除
      fieldSettings.value = fieldSettings.value.filter((fs) => fs.settingId !== id)
    } catch (err) {
      error.value = err
      console.error(`Failed to delete field setting ${id}:`, err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 根據欄位名稱獲取欄位設定
   * @param {string} fieldName 欄位名稱
   * @returns {Object|undefined}
   */
  const getFieldSettingByName = (fieldName) => {
    return fieldSettings.value.find((fs) => fs.fieldName === fieldName)
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
    fieldSettings.value = []
    isLoading.value = false
    error.value = null
  }

  return {
    // State
    fieldSettings,
    isLoading,
    error,
    // Actions
    fetchFieldSettings,
    fetchAllFieldSettings,
    fetchFieldSettingById,
    createFieldSetting,
    updateFieldSetting,
    deleteFieldSetting,
    getFieldSettingByName,
    clearError,
    reset,
  }
})

