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
          @change="clearQuickRangeHighlight"
        />
      </div>

      <div class="control-group">
        <label for="end-date">結束日期</label>
        <input
          id="end-date"
          v-model="endDate"
          type="date"
          class="form-input"
          @change="clearQuickRangeHighlight"
        />
      </div>

      <button
        @click="fetchData"
        :disabled="!canFetch || isLoading"
        class="btn btn-primary"
      >
        {{ isLoading ? '載入中...' : '查詢' }}
      </button>

      <div class="quick-range-bar">
        <span class="quick-range-label">快速區間</span>
        <div class="quick-range-tags">
          <button
            v-for="p in quickRangePresets"
            :key="p.id"
            type="button"
            class="quick-range-tag"
            :class="{ 'quick-range-tag--active': selectedQuickRangeId === p.id }"
            :disabled="isLoading"
            @click="onQuickRange(p)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="!hasData && !isLoading" class="empty-message">
      請選擇欄位和日期範圍後點擊查詢
    </div>
    
    <!-- NUMBER 類型報表 -->
    <div v-else-if="selectedFieldType === 'NUMBER' && numberReport" class="chart-wrapper">
      <h2 class="report-type-title">數值報表</h2>
      <div class="report-tabs" role="tablist" aria-label="數值報表圖表">
        <button
          v-for="tab in numberChartTabs"
          :key="tab.id"
          type="button"
          role="tab"
          class="report-tab"
          :class="{ 'report-tab--active': numberChartTab === tab.id }"
          :aria-selected="numberChartTab === tab.id"
          @click="numberChartTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="report-tab-panel">
        <div
          v-show="numberChartTab === 'summary'"
          class="report-section"
        >
          <h3 class="report-panel-title">統計摘要</h3>
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
        <div v-show="numberChartTab === 'line'" class="report-tab-chart-host">
          <NumberTrendChart
            :trend-data="numberReport.trendData"
            :y-axis-name="selectedFieldUnit"
          />
        </div>
        <div v-show="numberChartTab === 'calendar'" class="report-tab-chart-host">
          <NumberCalendarHeatmap
            :trend-data="numberReport.trendData"
            :range-start="startDate"
            :range-end="endDate"
            :value-label="numberHeatmapValueLabel"
          />
        </div>
        <div v-show="numberChartTab === 'table'" class="data-preview">
          <h3 class="report-panel-title">趨勢數據</h3>
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
    </div>

    <!-- ENUM 類型報表 - 分佈統計 -->
    <div v-else-if="selectedFieldType === 'ENUM' && enumDistribution" class="chart-wrapper">
      <h2 class="report-type-title">選項報表</h2>
      <div class="report-tabs" role="tablist" aria-label="選項報表圖表">
        <button
          v-for="tab in enumChartTabs"
          :key="tab.id"
          type="button"
          role="tab"
          class="report-tab"
          :class="{ 'report-tab--active': enumChartTab === tab.id }"
          :aria-selected="enumChartTab === tab.id"
          @click="enumChartTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="report-tab-panel">
        <div v-show="enumChartTab === 'distribution'" class="report-tab-chart-host">
          <EnumDistributionChart
            :distribution="enumDistribution.distribution"
            :percentages="enumDistribution.percentages"
            :option-order="selectedEnumOptionOrder"
          />
        </div>
        <div v-show="enumChartTab === 'calendar'" class="report-tab-chart-host">
          <EnumOptionCalendarHeatmap
            :enum-trend="enumTrend"
            :range-start="startDate"
            :range-end="endDate"
            :option-order="selectedEnumOptionOrder"
          />
        </div>
        <div v-show="enumChartTab === 'list'" class="report-section">
          <h3 class="report-panel-title">選項明細</h3>
          <div class="distribution-list">
            <div
              v-for="row in enumDistributionRows"
              :key="row.option"
              class="distribution-item"
            >
              <div class="distribution-label">{{ row.option }}</div>
              <div class="distribution-bar">
                <div
                  class="distribution-fill"
                  :style="{
                    width: `${roundPercent(row.percent)}%`,
                    background: `linear-gradient(90deg, ${row.barColorDark}, ${row.barColor})`,
                  }"
                >
                </div>
              </div>
              <div class="distribution-count">
                <span>{{ row.count }} 次</span>
                <span class="percentage-text">
                  ({{ roundPercent(row.percent) }}%)
                </span>
              </div>
            </div>
          </div>
          <div class="total-count">總記錄數：{{ enumDistribution.totalCount }}</div>
        </div>
      </div>
    </div>

    <!-- TEXT 類型報表 -->
    <div v-else-if="selectedFieldType === 'TEXT' && textAnalysis" class="chart-wrapper">
      <h2 class="report-type-title">文字報表</h2>
      <div class="report-tabs" role="tablist" aria-label="文字報表圖表">
        <button
          v-for="tab in textChartTabs"
          :key="tab.id"
          type="button"
          role="tab"
          class="report-tab"
          :class="{ 'report-tab--active': textChartTab === tab.id }"
          :aria-selected="textChartTab === tab.id"
          @click="textChartTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="report-tab-panel">
        <div v-show="textChartTab === 'summary'" class="report-section">
          <h3 class="report-panel-title">摘要</h3>
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
        </div>
        <div v-show="textChartTab === 'calendar'" class="report-tab-chart-host">
          <TextCalendarHeatmap
            :timeline-data="textAnalysis.timelineData || {}"
            :range-start="startDate"
            :range-end="endDate"
          />
        </div>
        <div v-show="textChartTab === 'keywords'" class="report-tab-chart-host">
          <KeywordBarChart :keyword-frequency="textAnalysis.keywordFrequency || {}" />
        </div>
        <div v-show="textChartTab === 'keywordList'" class="keyword-section">
          <h3 class="report-panel-title">關鍵字頻率（前 10 名）</h3>
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
import '@/echarts/registerEcharts.js'
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useReportStore } from '@/stores/reportStore'
import { useSettingsStore } from '@/stores/settingsStore'
import NumberTrendChart from '@/components/charts/NumberTrendChart.vue'
import NumberCalendarHeatmap from '@/components/charts/NumberCalendarHeatmap.vue'
import EnumDistributionChart from '@/components/charts/EnumDistributionChart.vue'
import EnumOptionCalendarHeatmap from '@/components/charts/EnumOptionCalendarHeatmap.vue'
import TextCalendarHeatmap from '@/components/charts/TextCalendarHeatmap.vue'
import KeywordBarChart from '@/components/charts/KeywordBarChart.vue'
import {
  parseEnumOptionLabels,
  orderNamesByFieldOptions,
} from '@/utils/enumOptionOrder.js'
import {
  colorForEnumOption,
  mixHexWithBlack,
} from '@/utils/enumChartColors.js'

const reportStore = useReportStore()
const settingsStore = useSettingsStore()

const numberChartTabs = [
  { id: 'summary', label: '統計摘要' },
  { id: 'line', label: '趨勢圖' },
  { id: 'calendar', label: '日曆熱力圖' },
  { id: 'table', label: '趨勢數據表' },
]
const numberChartTab = ref('summary')

const enumChartTabs = [
  { id: 'distribution', label: '分布圖' },
  { id: 'calendar', label: '日曆熱力圖' },
  { id: 'list', label: '選項計數表' },
]
const enumChartTab = ref('distribution')

const textChartTabs = [
  { id: 'summary', label: '摘要' },
  { id: 'calendar', label: '日曆熱力圖' },
  { id: 'keywords', label: '關鍵字長條' },
  { id: 'keywordList', label: '關鍵字列表' },
]
const textChartTab = ref('summary')

function scheduleChartResize() {
  nextTick(() => {
    window.dispatchEvent(new Event('resize'))
  })
}

watch(numberChartTab, scheduleChartResize)
watch(enumChartTab, scheduleChartResize)
watch(textChartTab, scheduleChartResize)

const selectedField = ref('')
const startDate = ref('')
const endDate = ref('')

const isLoading = computed(() => reportStore.isLoading)
const error = computed(() => reportStore.error)
const numberReport = computed(() => reportStore.numberReport)
const enumDistribution = computed(() => reportStore.enumDistribution)
const enumTrend = computed(() => reportStore.enumTrend)
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

const selectedFieldUnit = computed(() => {
  if (!selectedField.value) return ''
  const field = settingsStore.fieldSettings.find(
    (f) => f.fieldName === selectedField.value
  )
  return field?.unit?.trim() || ''
})

const numberHeatmapValueLabel = computed(() => {
  const u = selectedFieldUnit.value
  return u ? `數值（${u}）` : '數值'
})

/** 目前 ENUM 欄位在設定中的選項順序（與表單下拉一致） */
const selectedEnumOptionOrder = computed(() => {
  if (selectedFieldType.value !== 'ENUM' || !selectedField.value) return []
  const field = settingsStore.fieldSettings.find(
    (f) => f.fieldName === selectedField.value
  )
  if (!field || field.dataType !== 'ENUM') return []
  return parseEnumOptionLabels(field.options)
})

/** 分佈列表列順序：依欄位設定，其餘鍵接在後 */
const enumDistributionRows = computed(() => {
  const ed = enumDistribution.value
  if (!ed?.distribution) return []
  const dist = ed.distribution
  const pct = ed.percentages || {}
  const keys = Object.keys(dist)
  const order = selectedEnumOptionOrder.value
  const ordered = orderNamesByFieldOptions(keys, order)
  return ordered.map((option, i) => {
    const barColor = colorForEnumOption(option, order, i)
    return {
      option,
      count: dist[option],
      percent: pct[option],
      barColor,
      barColorDark: mixHexWithBlack(barColor, 0.22),
    }
  })
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

/** 百分比四捨五入至整數（顯示與長條寬度） */
const roundPercent = (value) => Math.round(Number(value ?? 0))

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
  reportStore.clearAllData()
  numberChartTab.value = 'summary'
  enumChartTab.value = 'distribution'
  textChartTab.value = 'summary'
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
      await reportStore.fetchEnumDistributionAndTrend(params)
    } else if (selectedFieldType.value === 'TEXT') {
      await reportStore.fetchTextAnalysis(params)
    }
  } catch (err) {
    console.error('Fetch report data error:', err)
  }
}

/** 本地日曆日轉 YYYY-MM-DD（避免 toISOString 時區偏移） */
function toLocalYMD(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 結束日為今天、含首尾共 inclusiveDays 天 */
function applyInclusiveDayRange(inclusiveDays) {
  const end = new Date()
  end.setHours(0, 0, 0, 0)
  const start = new Date(end)
  start.setDate(start.getDate() - (inclusiveDays - 1))
  endDate.value = toLocalYMD(end)
  startDate.value = toLocalYMD(start)
}

/** 本週起始：週一 00:00（本地） */
function startOfThisWeekMonday(ref) {
  const d = new Date(ref)
  d.setHours(0, 0, 0, 0)
  const day = d.getDay()
  const offset = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + offset)
  return d
}

function applyFixedEndRange(start, end) {
  const e = new Date(end)
  e.setHours(0, 0, 0, 0)
  const s = new Date(start)
  s.setHours(0, 0, 0, 0)
  endDate.value = toLocalYMD(e)
  startDate.value = toLocalYMD(s)
}

const quickRangePresets = [
  { id: 'cw', label: '本周', mode: 'thisWeek' },
  { id: 'cm', label: '本月', mode: 'thisMonth' },
  { id: 'cq', label: '本季', mode: 'thisQuarter' },
  { id: 'cy', label: '本年', mode: 'thisYear' },
  { id: '7', label: '近1週', mode: 'rolling', days: 7 },
  { id: '14', label: '近2週', mode: 'rolling', days: 14 },
  { id: '30', label: '近1個月', mode: 'rolling', days: 30 },
  { id: '90', label: '近3個月', mode: 'rolling', days: 90 },
]

const selectedQuickRangeId = ref(null)

function clearQuickRangeHighlight() {
  selectedQuickRangeId.value = null
}

async function onQuickRange(preset) {
  selectedQuickRangeId.value = preset.id
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  switch (preset.mode) {
    case 'rolling':
      applyInclusiveDayRange(preset.days ?? 7)
      break
    case 'thisWeek':
      applyFixedEndRange(startOfThisWeekMonday(today), today)
      break
    case 'thisMonth':
      applyFixedEndRange(
        new Date(today.getFullYear(), today.getMonth(), 1),
        today
      )
      break
    case 'thisQuarter': {
      const q0 = Math.floor(today.getMonth() / 3) * 3
      applyFixedEndRange(new Date(today.getFullYear(), q0, 1), today)
      break
    }
    case 'thisYear':
      applyFixedEndRange(new Date(today.getFullYear(), 0, 1), today)
      break
    default:
      applyInclusiveDayRange(7)
  }

  await nextTick()
  if (canFetch.value) {
    await fetchData()
  }
}

// 初始化日期範圍（預設為最近 30 天，本地日；與「近1個月」一致）
const initDateRange = () => {
  applyInclusiveDayRange(30)
  selectedQuickRangeId.value = '30'
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

.quick-range-bar {
  flex-basis: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem 1rem;
  padding-top: 1rem;
  margin-top: 0.25rem;
  border-top: 1px solid #eee;
}

.quick-range-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #555;
  white-space: nowrap;
}

.quick-range-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.quick-range-tag {
  padding: 0.35rem 0.75rem;
  border: 1px solid #d0d0d0;
  border-radius: 999px;
  background: #fafafa;
  color: #444;
  font-size: 0.8125rem;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.quick-range-tag:hover:not(:disabled) {
  background: #eef4fc;
  border-color: #4a90e2;
  color: #333;
}

.quick-range-tag--active {
  background: #e8f0fe;
  border-color: #4a90e2;
  color: #1a4a8c;
  font-weight: 600;
}

.quick-range-tag:disabled {
  opacity: 0.55;
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

.report-type-title {
  margin: 0 0 1rem;
  font-size: 1.5rem;
  color: #333;
}

.report-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e8e8e8;
}

.report-tab {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #f5f5f5;
  color: #555;
  font-size: 0.9375rem;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.report-tab:hover {
  background: #eee;
  color: #333;
}

.report-tab--active {
  background: #fff;
  border-color: #4a90e2;
  color: #333;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(74, 144, 226, 0.2);
}

.report-tab-panel {
  min-height: 8rem;
}

.report-panel-title {
  margin: 0 0 1rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: #333;
}

.report-tab-chart-host {
  min-width: 0;
}

.report-tab-panel .data-preview {
  margin-top: 0;
}

.report-tab-panel > .report-section:last-child {
  margin-bottom: 0;
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
  overflow-wrap: anywhere;
  word-break: break-word;
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
  background-color: #e0f2f1;
  border-radius: 8px;
  font-weight: 500;
  color: #00695c;
}

.percentage-text {
  font-weight: 600;
  color: #0f766e;
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

.report-tab-panel .keyword-section {
  margin-top: 0;
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

