<template>
  <div class="calendar">
    <!-- 月份導航 -->
    <div class="calendar-header">
      <button @click="goToPreviousMonth" class="nav-btn">‹</button>
      <h2 class="month-title">{{ monthTitle }}</h2>
      <button @click="goToNextMonth" class="nav-btn">›</button>
    </div>

    <!-- 星期標題 -->
    <div class="weekdays">
      <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
    </div>

    <!-- 日期網格 -->
    <div class="days-grid">
      <div
        v-for="day in calendarDays"
        :key="day.date"
        :class="[
          'day-cell',
          {
            'other-month': !day.isCurrentMonth,
            'today': day.isToday,
            'has-record': day.hasRecord,
            'selected': day.isSelected
          }
        ]"
        @click="handleDayClick(day, $event)"
      >
        <span class="day-number">{{ day.day }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  currentMonth: {
    type: Date,
    required: true
  },
  recordedDates: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['date-click', 'month-change'])

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// 月份標題
const monthTitle = computed(() => {
  const year = props.currentMonth.getFullYear()
  const month = props.currentMonth.getMonth() + 1
  return `${year}年${month}月`
})

// 生成日曆天數
const calendarDays = computed(() => {
  const year = props.currentMonth.getFullYear()
  const month = props.currentMonth.getMonth()
  
  // 當月第一天
  const firstDay = new Date(year, month, 1)
  // 當月最後一天
  const lastDay = new Date(year, month + 1, 0)
  // 第一天是星期幾（0 = 星期日）
  const firstDayOfWeek = firstDay.getDay()
  
  const days = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  // 上個月的日期
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const date = new Date(year, month, -i)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      day: date.getDate(),
      isCurrentMonth: false,
      isToday: isSameDay(date, today),
      hasRecord: props.recordedDates.includes(dateStr)
    })
  }
  
  // 當月的日期
  for (let day = 1; day <= lastDay.getDate(); day++) {
    const date = new Date(year, month, day)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      day: day,
      isCurrentMonth: true,
      isToday: isSameDay(date, today),
      hasRecord: props.recordedDates.includes(dateStr)
    })
  }
  
  // 下個月的日期（填滿最後一行）
  const remainingDays = 42 - days.length // 6行 x 7天
  for (let day = 1; day <= remainingDays; day++) {
    const date = new Date(year, month + 1, day)
    const dateStr = formatDate(date)
    days.push({
      date: dateStr,
      day: day,
      isCurrentMonth: false,
      isToday: isSameDay(date, today),
      hasRecord: props.recordedDates.includes(dateStr)
    })
  }
  
  return days
})

// 格式化日期為 YYYY-MM-DD
const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 判斷是否為同一天
const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear() &&
         date1.getMonth() === date2.getMonth() &&
         date1.getDate() === date2.getDate()
}

// 處理日期點擊
const handleDayClick = (day, event) => {
  // 獲取點擊元素的位置
  const rect = event.currentTarget.getBoundingClientRect()
  emit('date-click', day.date, {
    top: rect.top,
    left: rect.left,
    width: rect.width,
    height: rect.height
  })
}

// 切換到上個月
const goToPreviousMonth = () => {
  const newMonth = new Date(props.currentMonth)
  newMonth.setMonth(newMonth.getMonth() - 1)
  emit('month-change', newMonth)
}

// 切換到下個月
const goToNextMonth = () => {
  const newMonth = new Date(props.currentMonth)
  newMonth.setMonth(newMonth.getMonth() + 1)
  emit('month-change', newMonth)
}
</script>

<style scoped>
.calendar {
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.nav-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #4a90e2;
  cursor: pointer;
  padding: 0.25rem 0.75rem;
  transition: all 0.2s;
  border-radius: 4px;
}

.nav-btn:hover {
  background-color: #f5f5f5;
}

.month-title {
  margin: 0;
  font-size: 1.25rem;
  color: #333;
  font-weight: 600;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}

.weekday {
  text-align: center;
  font-weight: 600;
  color: #666;
  padding: 0.5rem 0.25rem;
  font-size: 0.85rem;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.25rem;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  background: white;
  color: #333;
  min-height: 0;
}

.day-cell:hover {
  border-color: #4a90e2;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.2);
}

.day-cell.other-month {
  color: #ccc;
  background-color: #fafafa;
  border-color: #f0f0f0;
}

.day-cell.other-month.has-record {
  border-color: #4a90e2;
  background-color: #e3f2fd;
}

/* 沒有記錄的日期 */
.day-cell:not(.has-record):not(.other-month) {
  background-color: white;
  color: #333;
}

/* 有記錄的日期 - 藍色邊框和淺藍背景 */
.day-cell.has-record {
  background-color: #e3f2fd;
  border-color: #4a90e2;
  border-width: 2px;
  color: #1976d2;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(74, 144, 226, 0.15);
}

.day-cell.has-record .day-number {
  color: #1976d2;
  font-weight: 600;
}

/* 今天的日期 - 特殊標示 */
.day-cell.today {
  border-color: #4a90e2;
  border-width: 3px;
  font-weight: 700;
}

.day-cell.today.has-record {
  border-color: #4a90e2;
  background-color: #e3f2fd;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.3), 0 2px 8px rgba(74, 144, 226, 0.2);
}

.day-cell.today:not(.has-record) {
  background-color: #f0f7ff;
  border-color: #4a90e2;
  color: #4a90e2;
}

.day-cell.today:not(.has-record) .day-number {
  color: #4a90e2;
  font-weight: 700;
}

/* 選中的日期 */
.day-cell.selected {
  background-color: #4a90e2;
  border-color: #4a90e2;
  color: white;
  transform: scale(1.05);
  z-index: 2;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.4);
}

.day-cell.selected .day-number {
  color: white;
  font-weight: 700;
}

.day-number {
  font-size: 0.9rem;
  transition: all 0.2s;
}

/* 響應式設計 */
@media (min-width: 768px) {
  .calendar {
    max-width: 600px;
  }
  
  .days-grid {
    gap: 0.35rem;
  }
  
  .weekdays {
    gap: 0.35rem;
  }
}

@media (min-width: 1024px) {
  .calendar {
    max-width: 550px;
  }
}
</style>
