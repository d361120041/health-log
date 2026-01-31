<template>
  <div class="trend-chart-container">
    <h1>數據報表</h1>

    <div class="chart-controls">
      <div class="control-group">
        <label for="field-select">選擇欄位</label>
        <select
          id="field-select"
          v-model="selectedField"
          class="form-select"
          @change="onFieldChange"
        >
          <option value="">請選擇欄位</option>
          <option
            v-for="field in availableFields"
            :key="field.settingId"
            :value="field.fieldName"
          >
            {{ getFieldDisplayText(field) }}
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
    <div v-else-if="!hasData && !isLoading" class="empty-message">
      請選擇欄位和日期範圍後點擊查詢
    </div>
    
    <!-- NUMBER 類型報表 -->
    <div v-else-if="selectedFieldType === 'NUMBER' && numberReport" class="chart-wrapper">
      <div class="report-section">
        <h2>統計摘要</h2>
        <div class="statistics-grid" v-if="numberReport.statistics">
          <div class="stat-item">
            <span class="stat-label">平均值</span>
            <span class="stat-value">{{ formatNumber(numberReport.statistics.average) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最大值</span>
            <span class="stat-value">{{ formatNumber(numberReport.statistics.max) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最小值</span>
            <span class="stat-value">{{ formatNumber(numberReport.statistics.min) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">總和</span>
            <span class="stat-value">{{ formatNumber(numberReport.statistics.sum) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">記錄數</span>
            <span class="stat-value">{{ numberReport.statistics.count }}</span>
          </div>
        </div>
      </div>
      <div class="data-preview">
        <h3>趨勢數據：</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>數值</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="point in numberReport.trendData" :key="point.date">
              <td>{{ formatDate(point.date) }}</td>
              <td>{{ point.value || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ENUM 類型報表 - 分佈統計 -->
    <div v-else-if="selectedFieldType === 'ENUM' && enumDistribution" class="chart-wrapper">
      <div class="report-section">
        <h2>分佈統計</h2>
        <div class="distribution-list">
          <div 
            v-for="(count, option) in enumDistribution.distribution" 
            :key="option"
            class="distribution-item"
          >
            <div class="distribution-label">{{ option }}</div>
            <div class="distribution-bar">
              <div 
                class="distribution-fill" 
                :style="{ width: `${enumDistribution.percentages[option] || 0}%` }"
              >
              </div>
            </div>
            <div class="distribution-count">
              <span>{{ count }} 次</span>
              <span class="percentage-text">
                ({{ enumDistribution.percentages[option] || 0 }}%)
              </span>
            </div>
          </div>
        </div>
        <div class="total-count">總記錄數：{{ enumDistribution.totalCount }}</div>
      </div>
    </div>

    <!-- TEXT 類型報表 -->
    <div v-else-if="selectedFieldType === 'TEXT' && textAnalysis" class="chart-wrapper">
      <div class="report-section">
        <h2>文字分析</h2>
        <div class="text-stats">
          <div class="stat-item">
            <span class="stat-label">總記錄數</span>
            <span class="stat-value">{{ textAnalysis.totalCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">平均長度</span>
            <span class="stat-value">{{ Math.round(textAnalysis.averageLength) }} 字</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最長</span>
            <span class="stat-value">{{ textAnalysis.maxLength }} 字</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">最短</span>
            <span class="stat-value">{{ textAnalysis.minLength }} 字</span>
          </div>
        </div>
        <div class="keyword-section">
          <h3>關鍵字頻率（前 10 名）</h3>
          <div class="keyword-list">
            <div 
              v-for="(count, keyword) in topKeywords" 
              :key="keyword"
              class="keyword-item"
            >
              <span class="keyword-text">{{ keyword }}</span>
              <span class="keyword-count">{{ count }} 次</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useReportStore } from '@/stores/reportStore'
import { useSettingsStore } from '@/stores/settingsStore'

const reportStore = useReportStore()
const settingsStore = useSettingsStore()

const selectedField = ref('')
const startDate = ref('')
const endDate = ref('')

const isLoading = computed(() => reportStore.isLoading)
const error = computed(() => reportStore.error)
const numberReport = computed(() => reportStore.numberReport)
const enumDistribution = computed(() => reportStore.enumDistribution)
const textAnalysis = computed(() => reportStore.textAnalysis)

// 顯示所有啟用的欄位
const availableFields = computed(() => {
  return settingsStore.fieldSettings.filter(
    (field) => field.isActive
  )
})

// 獲取選中欄位的類型
const selectedFieldType = computed(() => {
  if (!selectedField.value) return null
  const field = settingsStore.fieldSettings.find(
    (f) => f.fieldName === selectedField.value
  )
  return field?.dataType || null
})

// 檢查是否有數據
const hasData = computed(() => {
  if (selectedFieldType.value === 'NUMBER') {
    return numberReport.value !== null
  } else if (selectedFieldType.value === 'ENUM') {
    return enumDistribution.value !== null
  } else if (selectedFieldType.value === 'TEXT') {
    return textAnalysis.value !== null
  }
  return false
})

// TEXT 類型的前 10 名關鍵字
const topKeywords = computed(() => {
  if (!textAnalysis.value?.keywordFrequency) return {}
  const sorted = Object.entries(textAnalysis.value.keywordFrequency)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
  return Object.fromEntries(sorted)
})

const canFetch = computed(() => {
  return selectedField.value && startDate.value && endDate.value
})

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-TW')
}

const formatNumber = (value) => {
  if (value === null || value === undefined) return '-'
  return Number(value).toLocaleString('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const getFieldTypeLabel = (dataType) => {
  const labels = {
    NUMBER: '數值',
    ENUM: '選項',
    TEXT: '文字'
  }
  return labels[dataType] || dataType
}

const getFieldTypeClass = (dataType) => {
  return `type-${dataType.toLowerCase()}`
}

const getFieldDisplayText = (field) => {
  let text = field.fieldName
  if (field.unit) {
    text += ` (${field.unit})`
  }
  return text
}

const onFieldChange = () => {
  // 欄位改變時清除舊數據
  reportStore.clearAllData()
}

const fetchData = async () => {
  if (!canFetch.value) return

  const params = {
    fieldName: selectedField.value,
    startDate: startDate.value,
    endDate: endDate.value,
  }

  try {
    if (selectedFieldType.value === 'NUMBER') {
      await reportStore.fetchNumberReport(params)
    } else if (selectedFieldType.value === 'ENUM') {
      await reportStore.fetchEnumDistribution(params)
    } else if (selectedFieldType.value === 'TEXT') {
      await reportStore.fetchTextAnalysis(params)
    }
  } catch (err) {
    console.error('Fetch report data error:', err)
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

.field-type-badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  margin-left: 0.5rem;
  font-weight: normal;
}

.type-number {
  background-color: #e3f2fd;
  color: #1976d2;
}

.type-enum {
  background-color: #f3e5f5;
  color: #7b1fa2;
}

.type-text {
  background-color: #e8f5e9;
  color: #388e3c;
}

.report-section {
  margin-bottom: 2rem;
}

.report-section h2 {
  margin-bottom: 1rem;
  color: #333;
  font-size: 1.5rem;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  padding: 1rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.stat-label {
  font-size: 0.875rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
}

.distribution-list {
  margin-bottom: 1rem;
}

.distribution-item {
  display: flex;
  flex-direction: column; /* 手機版：垂直佈局 */
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.distribution-label {
  width: 100%; /* 手機版：全寬 */
  font-weight: 500;
  color: #333;
  font-size: 0.9rem;
}

.distribution-bar {
  width: 100%; /* 手機版：全寬 */
  height: 36px; /* 增加高度，讓效果更明顯 */
  background-color: #e0e0e0;
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.distribution-fill {
  height: 100%;
  min-width: 4%; /* 降低最小寬度，但確保可見 */
  background: linear-gradient(90deg, #4a90e2, #357abd);
  transition: width 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.distribution-count {
  width: 100%; /* 手機版：全寬 */
  text-align: left; /* 手機版：左對齊 */
  font-size: 0.875rem;
  color: #666;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 桌面版：橫向佈局 */
@media (min-width: 768px) {
  .distribution-item {
    flex-direction: row; /* 桌面版：橫向佈局 */
    align-items: center;
    gap: 1rem;
  }

  .distribution-label {
    min-width: 100px;
    width: auto;
    font-size: 1rem;
  }

  .distribution-bar {
    flex: 1;
    width: auto;
    height: 28px; /* 桌面版：適中的高度 */
    border-radius: 14px;
  }

  .distribution-count {
    min-width: 120px;
    width: auto;
    text-align: right;
    justify-content: flex-end;
  }
}

.total-count {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #e3f2fd;
  border-radius: 8px;
  font-weight: 500;
  color: #1976d2;
}

.percentage-text {
  font-weight: 600;
  color: #4a90e2;
}

.text-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.keyword-section {
  margin-top: 2rem;
}

.keyword-section h3 {
  margin-bottom: 1rem;
  color: #333;
}

.keyword-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.keyword-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.keyword-text {
  font-weight: 500;
  color: #333;
}

.keyword-count {
  font-size: 0.875rem;
  color: #666;
  background-color: #e0e0e0;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
}
</style>

