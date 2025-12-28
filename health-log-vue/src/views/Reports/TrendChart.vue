<template>
  <div class="trend-chart-container">
    <h1>趨勢圖表</h1>

    <div class="chart-controls">
      <div class="control-group">
        <label for="field-select">選擇欄位</label>
        <select
          id="field-select"
          v-model="selectedField"
          class="form-select"
        >
          <option value="">請選擇欄位</option>
          <option
            v-for="field in numberFields"
            :key="field.settingId"
            :value="field.fieldName"
          >
            {{ field.fieldName }} {{ field.unit ? `(${field.unit})` : '' }}
          </option>
        </select>
      </div>

      <div class="control-group">
        <label for="start-date">開始日期</label>
        <input
          id="start-date"
          v-model="startDate"
          type="date"
          class="form-input"
        />
      </div>

      <div class="control-group">
        <label for="end-date">結束日期</label>
        <input
          id="end-date"
          v-model="endDate"
          type="date"
          class="form-input"
        />
      </div>

      <button
        @click="fetchData"
        :disabled="!canFetch || isLoading"
        class="btn btn-primary"
      >
        {{ isLoading ? '載入中...' : '查詢' }}
      </button>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="trendData.length === 0 && !isLoading" class="empty-message">
      請選擇欄位和日期範圍後點擊查詢
    </div>
    <div v-else-if="trendData.length > 0" class="chart-wrapper">
      <div class="chart-placeholder">
        <p>圖表區域（可整合 Chart.js 或其他圖表庫）</p>
        <div class="data-preview">
          <h3>數據預覽：</h3>
          <table class="data-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>數值</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="point in trendData" :key="point.date">
                <td>{{ formatDate(point.date) }}</td>
                <td>{{ point.value || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useReportStore } from '@/stores/reportStore'
import { useSettingsStore } from '@/stores/settingsStore'

const reportStore = useReportStore()
const settingsStore = useSettingsStore()

const selectedField = ref('')
const startDate = ref('')
const endDate = ref('')

const trendData = computed(() => reportStore.trendData)
const isLoading = computed(() => reportStore.isLoading)
const error = computed(() => reportStore.error)

// 只顯示 NUMBER 類型的欄位
const numberFields = computed(() => {
  return settingsStore.fieldSettings.filter(
    (field) => field.isActive && field.dataType === 'NUMBER'
  )
})

const canFetch = computed(() => {
  return selectedField.value && startDate.value && endDate.value
})

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-TW')
}

const fetchData = async () => {
  if (!canFetch.value) return

  try {
    await reportStore.fetchTrendData({
      fieldName: selectedField.value,
      startDate: startDate.value,
      endDate: endDate.value,
      includeNulls: false,
    })
  } catch (err) {
    console.error('Fetch trend data error:', err)
  }
}

// 初始化日期範圍（預設為最近 30 天）
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)

  endDate.value = end.toISOString().split('T')[0]
  startDate.value = start.toISOString().split('T')[0]
}

onMounted(async () => {
  // 載入欄位設定
  if (settingsStore.fieldSettings.length === 0) {
    await settingsStore.fetchFieldSettings()
  }
  initDateRange()
})
</script>

<style scoped>
.trend-chart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  margin-bottom: 2rem;
  color: #333;
}

.chart-controls {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-width: 150px;
}

.control-group label {
  font-weight: 500;
  color: #333;
}

.form-select,
.form-input {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-select:focus,
.form-input:focus {
  outline: none;
  border-color: #4a90e2;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  align-self: flex-end;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #357abd;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message,
.empty-message {
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

.chart-wrapper {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-placeholder {
  text-align: center;
  padding: 2rem;
}

.data-preview {
  margin-top: 2rem;
  text-align: left;
}

.data-preview h3 {
  margin-bottom: 1rem;
  color: #333;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  background-color: #f5f5f5;
  font-weight: 500;
  color: #333;
}
</style>

