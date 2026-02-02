<template>
  <div class="home-container">
    <div class="header">
      <h1>每日記錄</h1>
      <button @click="openNewRecordDialog" class="btn btn-primary">
        新增記錄
      </button>
    </div>

    <div class="calendar-section">
      <Calendar
        :current-month="currentMonth"
        :recorded-dates="recordedDates"
        @date-click="handleDateClick"
        @month-change="handleMonthChange"
      />
    </div>

    <!-- 日期確認對話框 -->
    <DateDialog
      v-if="showDialog"
      :date="selectedDate"
      :has-record="hasRecord"
      :position="dialogPosition"
      :show="showDialog"
      @close="closeDialog"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { useRecordsStore } from '@/stores/recordsStore'
import Calendar from '@/components/common/Calendar.vue'
import DateDialog from '@/components/common/DateDialog.vue'

const router = useRouter()
const recordsStore = useRecordsStore()

// 當前顯示的月份
const currentMonth = ref(new Date())

// 對話框狀態
const showDialog = ref(false)
const selectedDate = ref(null)
const selectedRecord = ref(null)
const dialogPosition = ref(null)

// 月份記錄列表（用於標記月曆）
const monthRecords = ref([])

// 獲取當前月份有記錄的日期列表
const recordedDates = computed(() => {
  return monthRecords.value.map(record => record.recordDate)
})

// 判斷是否有記錄
const hasRecord = computed(() => !!selectedRecord.value)

// 格式化日期為 YYYY-MM-DD
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

// 處理日期點擊
const handleDateClick = async (date, position) => {
  selectedDate.value = date
  dialogPosition.value = position
  
  // 確保日期格式一致（轉換為字符串格式 YYYY-MM-DD）
  const dateStr = typeof date === 'string' ? date : formatDate(date)
  
  // 檢查點擊的日期是否屬於當前月份
  const clickedDate = new Date(dateStr)
  const isCurrentMonth = clickedDate.getFullYear() === currentMonth.value.getFullYear() &&
                         clickedDate.getMonth() === currentMonth.value.getMonth()
  
  // 先從已載入的月份記錄中查找
  const existingRecord = monthRecords.value.find(
    record => {
      // 確保比較的都是字符串格式
      const recordDateStr = typeof record.recordDate === 'string' 
        ? record.recordDate 
        : formatDate(record.recordDate)
      return recordDateStr === dateStr
    }
  )
  
  if (existingRecord) {
    // 如果找到了，直接使用，不需要再打 API
    selectedRecord.value = existingRecord
    showDialog.value = true
  } else if (isCurrentMonth) {
    // 如果屬於當前月份但沒找到，表示該日期確實沒有記錄
    // 不需要再打 API，因為 monthRecords 已經包含了該月份的所有記錄
    selectedRecord.value = null
    showDialog.value = true
  } else {
    // 如果不屬於當前月份，需要發送 API 請求（因為該月份的記錄還沒載入）
    try {
      const record = await recordsStore.fetchRecordByDate(dateStr)
      selectedRecord.value = record
      showDialog.value = true
    } catch (err) {
      // 如果沒有記錄，selectedRecord 會是 null
      selectedRecord.value = null
      showDialog.value = true
    }
  }
}

// 處理月份變更
const handleMonthChange = async (newMonth) => {
  currentMonth.value = newMonth
  await loadMonthRecords()
}

// 載入當前月份的記錄
const loadMonthRecords = async () => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const startDate = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const endDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`

  try {
    // 使用 search API 獲取該月份的所有記錄
    const records = await recordsStore.fetchRecordsByDateRange(startDate, endDate)
    monthRecords.value = records
  } catch (err) {
    console.error('Failed to load month records:', err)
    monthRecords.value = []
  }
}

// 開啟新增記錄
const openNewRecordDialog = () => {
  router.push({ name: 'RecordNew' })
}

// 關閉對話框
const closeDialog = () => {
  showDialog.value = false
  selectedDate.value = null
  selectedRecord.value = null
  dialogPosition.value = null
}

onMounted(async () => {
  await loadMonthRecords()
})

// 當從其他頁面返回時重新載入記錄
onActivated(async () => {
  await loadMonthRecords()
})
</script>

<style scoped>
.home-container {
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

.calendar-section {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
