<template>
  <div class="home-container">
    <div class="header">
      <h1>每日記錄</h1>
      <button @click="openNewRecordDialog" class="btn btn-primary">
        新增記錄
      </button>
    </div>

    <!-- 統計訊息區塊 -->
    <div class="stats-section">
      <div class="stat-card" :class="{ 'warning': !hasTodayRecord }">
        <div class="stat-icon">📝</div>
        <div class="stat-content">
          <div class="stat-label">今日狀態</div>
          <div class="stat-value">
            {{ hasTodayRecord ? '✓ 今日已記錄' : '⚠ 您今日還沒紀錄唷！' }}
          </div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">📅</div>
        <div class="stat-content">
          <div class="stat-label">{{ currentMonthLabel }}記錄</div>
          <div class="stat-value">
            已記錄 <strong>{{ currentMonthRecordCount }}</strong> 天
          </div>
          <div class="stat-subtext">
            共 {{ totalDaysInMonth }} 天
          </div>
        </div>
      </div>
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
      :record="selectedRecord"
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

// 月份記錄列表（用於標記月曆）
const monthRecords = ref([])

// 今日記錄狀態（獨立追蹤，不依賴當前顯示月份）
const todayRecord = ref(null)

// 獲取當前月份有記錄的日期列表
const recordedDates = computed(() => {
  return monthRecords.value.map(record => record.recordDate)
})

// 判斷是否有記錄
const hasRecord = computed(() => !!selectedRecord.value)

// 計算今日是否有記錄
const hasTodayRecord = computed(() => {
  return todayRecord.value !== null
})

// 計算本月已記錄天數
const currentMonthRecordCount = computed(() => {
  return monthRecords.value.length
})

// 計算本月總天數
const totalDaysInMonth = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  return new Date(year, month + 1, 0).getDate()
})

// 當前顯示月份的標籤
const currentMonthLabel = computed(() => {
  const today = new Date()
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const isCurrentMonth = year === today.getFullYear() && month === today.getMonth()
  return isCurrentMonth ? '本月' : `${year}年${month + 1}月`
})

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
const handleDateClick = async (date) => {
  selectedDate.value = date

  // 確保日期格式一致（轉換為字符串格式 YYYY-MM-DD）
  const dateStr = typeof date === 'string' ? date : formatDate(date)
  
  // 檢查點擊的日期是否屬於當前月份
  const clickedDate = new Date(dateStr)
  const isCurrentMonth = clickedDate.getFullYear() === currentMonth.value.getFullYear() &&
                         clickedDate.getMonth() === currentMonth.value.getMonth()
  
  // 如果屬於當前月份，先使用 recordedDates 快速判斷是否有記錄
  if (isCurrentMonth) {
    if (recordedDates.value.includes(dateStr)) {
      // 有記錄，從 monthRecords 中查找完整記錄對象
      const existingRecord = monthRecords.value.find(
        record => {
          const recordDateStr = typeof record.recordDate === 'string' 
            ? record.recordDate 
            : formatDate(record.recordDate)
          return recordDateStr === dateStr
        }
      )
      selectedRecord.value = existingRecord
    } else {
      // 沒有記錄
      selectedRecord.value = null
    }
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

// 載入今日記錄
const loadTodayRecord = async () => {
  const today = formatDate(new Date())
  try {
    const record = await recordsStore.fetchRecordByDate(today)
    todayRecord.value = record
  } catch (err) {
    // 404 表示今天沒有記錄，這是正常情況
    if (err.response?.status === 404) {
      todayRecord.value = null
    } else {
      console.error('Failed to load today record:', err)
    }
  }
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
    
    // 如果當前顯示的月份是本月，更新今日記錄狀態
    const today = new Date()
    if (year === today.getFullYear() && month === today.getMonth()) {
      const todayStr = formatDate(today)
      const todayRecordInMonth = records.find(r => r.recordDate === todayStr)
      todayRecord.value = todayRecordInMonth || null
    }
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
}

onMounted(async () => {
  await Promise.all([
    loadTodayRecord(),
    loadMonthRecords()
  ])
})

// 當從其他頁面返回時重新載入記錄
onActivated(async () => {
  await Promise.all([
    loadTodayRecord(),
    loadMonthRecords()
  ])
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

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.stat-card.warning {
  border-left: 4px solid #ff9800;
  background: linear-gradient(135deg, #fff3e0 0%, #ffffff 100%);
}

.stat-icon {
  font-size: 2.5rem;
  line-height: 1;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.125rem;
  color: #333;
  font-weight: 500;
}

.stat-value strong {
  color: #4a90e2;
  font-size: 1.5rem;
}

.stat-subtext {
  font-size: 0.75rem;
  color: #999;
  margin-top: 0.25rem;
}

.calendar-section {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 響應式設計 */
@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }
  
  .stat-card {
    padding: 1rem;
  }
  
  .stat-icon {
    font-size: 2rem;
  }
}
</style>
