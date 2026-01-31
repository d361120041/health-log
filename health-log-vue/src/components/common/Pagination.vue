<template>
  <div v-if="showPagination" class="pagination-wrapper">
    <!-- 分頁資訊和控制（頂部） -->
    <div v-if="showInfo" class="pagination-controls">
      <div class="pagination-info">
        <slot name="info">
          顯示第 {{ startIndex }} - {{ endIndex }} 筆，
          共 {{ totalElements }} 筆記錄
        </slot>
      </div>
      <div v-if="showPageSizeSelector" class="pagination-size-selector">
        <label>每頁顯示：</label>
        <select 
          :value="pagination.pageSize" 
          @change="handlePageSizeChange" 
          class="page-size-select"
        >
          <option 
            v-for="size in pageSizeOptions" 
            :key="size" 
            :value="size"
          >
            {{ size }}
          </option>
        </select>
      </div>
    </div>

    <!-- 分頁導航（底部） -->
    <div v-if="showNav && pagination.totalPages > 1" class="pagination-nav">
      <button
        @click="handlePrevPage"
        :disabled="pagination.first"
        class="pagination-btn"
        :class="{ disabled: pagination.first }"
      >
        ← 上一頁
      </button>
      
      <div class="pagination-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          @click="handlePageChange(page)"
          :class="['pagination-number', { active: page === pagination.currentPage }]"
        >
          {{ page + 1 }}
        </button>
      </div>
      
      <button
        @click="handleNextPage"
        :disabled="pagination.last"
        class="pagination-btn"
        :class="{ disabled: pagination.last }"
      >
        下一頁 →
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 分頁數據（整合成一個物件）
  pagination: {
    type: Object,
    required: true,
    default: () => ({
      currentPage: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: false,
      numberOfElements: 0
    })
  },
  // 顯示控制
  showInfo: {
    type: Boolean,
    default: true
  },
  showNav: {
    type: Boolean,
    default: true
  },
  showPageSizeSelector: {
    type: Boolean,
    default: true
  },
  // 每頁大小選項
  pageSizeOptions: {
    type: Array,
    default: () => [5, 10, 20, 50]
  },
  // 最多顯示的頁碼數
  maxVisiblePages: {
    type: Number,
    default: 7
  }
})

const emit = defineEmits(['page-change', 'page-size-change'])

// 計算屬性
const showPagination = computed(() => {
  return props.pagination.totalPages > 0 || props.pagination.totalElements > 0
})

const startIndex = computed(() => {
  return props.pagination.currentPage * props.pagination.pageSize + 1
})

const endIndex = computed(() => {
  return Math.min(
    (props.pagination.currentPage + 1) * props.pagination.pageSize, 
    props.pagination.totalElements
  )
})

const totalElements = computed(() => props.pagination.totalElements)

// 計算可見的頁碼
const visiblePages = computed(() => {
  const total = props.pagination.totalPages
  const current = props.pagination.currentPage
  const maxVisible = props.maxVisiblePages
  
  if (total <= maxVisible) {
    return Array.from({ length: total }, (_, i) => i)
  }
  
  let start = Math.max(0, current - Math.floor(maxVisible / 2))
  let end = Math.min(total - 1, start + maxVisible - 1)
  
  if (end - start < maxVisible - 1) {
    start = Math.max(0, end - maxVisible + 1)
  }
  
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

// 事件處理
const handlePageChange = (page) => {
  if (page !== props.pagination.currentPage && page >= 0 && page < props.pagination.totalPages) {
    emit('page-change', page)
  }
}

const handleNextPage = () => {
  if (!props.pagination.last) {
    emit('page-change', props.pagination.currentPage + 1)
  }
}

const handlePrevPage = () => {
  if (!props.pagination.first) {
    emit('page-change', props.pagination.currentPage - 1)
  }
}

const handlePageSizeChange = (event) => {
  const newSize = parseInt(event.target.value)
  emit('page-size-change', newSize)
}
</script>

<style scoped>
.pagination-wrapper {
  width: 100%;
}

.pagination-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.pagination-info {
  color: #666;
  font-size: 0.9rem;
}

.pagination-size-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-size-selector label {
  color: #666;
  font-size: 0.9rem;
}

.page-size-select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  background-color: white;
}

.page-size-select:hover {
  border-color: #4a90e2;
}

.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
  padding: 1rem;
}

.pagination-btn {
  padding: 0.75rem 1.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  color: #333;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  min-width: 100px;
}

.pagination-btn:hover:not(.disabled) {
  background-color: #f5f5f5;
  border-color: #4a90e2;
  color: #4a90e2;
}

.pagination-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 0.25rem;
}

.pagination-number {
  min-width: 2.5rem;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  color: #333;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.pagination-number:hover {
  background-color: #f5f5f5;
  border-color: #4a90e2;
}

.pagination-number.active {
  background-color: #4a90e2;
  color: white;
  border-color: #4a90e2;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .pagination-controls {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .pagination-nav {
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .pagination-numbers {
    order: -1;
    width: 100%;
    justify-content: center;
  }

  .pagination-btn {
    min-width: auto;
    padding: 0.5rem 1rem;
  }
}
</style>
