import { ref } from 'vue'

/**
 * Pagination Composable
 * 提供分頁狀態管理和相關方法
 * 
 * @param {number} initialPageSize - 初始每頁大小，預設為 10
 * @returns {Object} 包含 pagination ref 和相關方法
 */
export function usePagination(initialPageSize = 5) {
  // 分頁狀態
  const pagination = ref({
    currentPage: 0, // 當前頁碼（從0開始）
    pageSize: initialPageSize, // 每頁大小
    totalElements: 0, // 總記錄數
    totalPages: 0, // 總頁數
    first: true, // 是否為第一頁
    last: false, // 是否為最後一頁
    numberOfElements: 0 // 當前頁實際元素數量
  })

  /**
   * 從後端 API 回應更新分頁資訊
   * @param {Object} pageData - 後端返回的分頁資料
   * @param {number} pageData.number - 當前頁碼
   * @param {number} pageData.size - 每頁大小
   * @param {number} pageData.totalElements - 總記錄數
   * @param {number} pageData.totalPages - 總頁數
   * @param {boolean} pageData.first - 是否為第一頁
   * @param {boolean} pageData.last - 是否為最後一頁
   * @param {number} pageData.numberOfElements - 當前頁實際元素數量
   */
  const updateFromApiResponse = (pageData) => {
    if (!pageData) return
    
    pagination.value = {
      currentPage: pageData.number ?? pagination.value.currentPage,
      pageSize: pageData.size ?? pagination.value.pageSize,
      totalElements: pageData.totalElements ?? 0,
      totalPages: pageData.totalPages ?? 0,
      first: pageData.first ?? true,
      last: pageData.last ?? false,
      numberOfElements: pageData.numberOfElements ?? 0
    }
  }

  /**
   * 重置分頁狀態為初始值
   */
  const reset = () => {
    pagination.value = {
      currentPage: 0,
      pageSize: initialPageSize,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: false,
      numberOfElements: 0
    }
  }

  /**
   * 切換到指定頁碼（僅更新狀態，不發送請求）
   * 注意：此方法只更新 currentPage，first 和 last 狀態應由 API 回應決定
   * @param {number} page - 頁碼（從0開始）
   */
  const goToPage = (page) => {
    if (page >= 0 && (pagination.value.totalPages === 0 || page < pagination.value.totalPages)) {
      pagination.value.currentPage = page
    }
  }

  /**
   * 改變每頁大小（僅更新狀態，不發送請求）
   * 注意：此方法只更新 pageSize，其他狀態應由 API 回應決定
   * @param {number} size - 每頁大小
   */
  const changePageSize = (size) => {
    if (size > 0) {
      pagination.value.pageSize = size
    }
  }

  return {
    pagination,
    updateFromApiResponse,
    reset,
    goToPage,
    changePageSize
  }
}
