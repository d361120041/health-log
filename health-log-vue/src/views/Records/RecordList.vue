<template>
  <div class="record-list-container">
    <div class="header">
      <h1>每日記錄</h1>
      <router-link to="/records/new" class="btn btn-primary">
        新增記錄
      </router-link>
    </div>

    <!-- 使用分頁元件 -->
    <Pagination
      v-if="!isLoading && pagination.totalElements > 0"
      :pagination="pagination"
      :page-size-options="[5, 10, 20, 50]"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />

    <div v-if="isLoading" class="loading-message">載入中...</div>
    <div v-else-if="error" class="error-message">
      {{ errorMessage }}
    </div>
    <div v-else-if="recordsList.length === 0 && pagination.currentPage === 0" class="empty-message">
      目前沒有記錄，<router-link to="/records/new">點擊這裡新增第一筆記錄</router-link>
    </div>
    <div v-else-if="recordsList.length === 0" class="empty-message">
      此頁沒有記錄
    </div>
    <div v-else class="records-grid">
      <div
        v-for="record in recordsList"
        :key="record.recordId"
        class="record-card"
      >
        <div class="record-header">
          <h3 class="record-date">{{ formatDate(record.recordDate) }}</h3>
          <div class="record-actions">
            <router-link
              :to="`/records/${record.recordDate}`"
              class="btn-icon"
              title="編輯"
            >
              ✏️
            </router-link>
            <button
              @click="handleDelete(record.recordDate)"
              class="btn-icon btn-danger"
              title="刪除"
              :disabled="isDeleting"
            >
              🗑️
            </button>
          </div>
        </div>
        <div class="record-content">
          <div
            v-for="(value, fieldName) in record.fieldValues"
            :key="fieldName"
            class="record-field"
          >
            <span class="field-name">{{ fieldName }}:</span>
            <span class="field-value">{{ value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRecordsStore } from '@/stores/recordsStore'
import Pagination from '@/components/common/Pagination.vue'

const router = useRouter()
const recordsStore = useRecordsStore()

const isDeleting = ref(false)

const recordsList = computed(() => recordsStore.recordsList)
const isLoading = computed(() => recordsStore.isLoading)
const error = computed(() => recordsStore.error)
const pagination = computed(() => recordsStore.pagination)

// 優化錯誤訊息顯示
const errorMessage = computed(() => {
  if (!error.value) return ''
  if (typeof error.value === 'string') return error.value
  if (error.value?.message) return error.value.message
  if (error.value?.response?.data?.message) return error.value.response.data.message
  return '載入記錄時發生錯誤，請稍後再試'
})

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

const handlePageChange = async (page) => {
  await recordsStore.goToPage(page)
}

const handlePageSizeChange = async (size) => {
  await recordsStore.changePageSize(size)
}

const handleDelete = async (date) => {
  if (!confirm('確定要刪除這筆記錄嗎？')) {
    return
  }

  isDeleting.value = true
  try {
    await recordsStore.deleteRecord(date)
  } catch (err) {
    console.error('Delete error:', err)
    alert('刪除失敗，請稍後再試')
  } finally {
    isDeleting.value = false
  }
}

onMounted(async () => {
  // 載入第一頁數據
  await recordsStore.fetchRecordsList({
    page: 0,
    size: 5
  })
})
</script>

<style scoped>
.record-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.header h1 {
  margin: 0;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover {
  background-color: #357abd;
}

.loading-message,
.empty-message,
.error-message {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-message {
  color: #e74c3c;
  background-color: #fee;
  border: 1px solid #e74c3c;
  border-radius: 4px;
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.record-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  transition: box-shadow 0.2s;
}

.record-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.record-date {
  margin: 0;
  color: #333;
  font-size: 1.25rem;
}

.record-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  transition: transform 0.2s;
}

.btn-icon:hover:not(:disabled) {
  transform: scale(1.1);
}

.btn-icon:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.record-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.record-field {
  display: flex;
  gap: 0.5rem;
}

.field-name {
  font-weight: 500;
  color: #666;
}

.field-value {
  color: #333;
}
</style>

