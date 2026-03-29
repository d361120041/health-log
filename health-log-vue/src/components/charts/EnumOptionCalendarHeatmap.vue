<template>
  <div class="echart-panel">
    <h3 class="echart-title">日曆熱力圖</h3>
    <div ref="wrapRef" class="chart-calendar-host" :style="hostStyle">
      <VChart
        v-if="hasData"
        class="chart chart-calendar"
        :option="chartOption"
        autoresize
      />
      <p v-else class="echart-empty">此區間無選項紀錄可繪製日曆</p>
    </div>
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import VChart from 'vue-echarts'
import { useCalendarHeatmapCellSize } from '@/composables/useCalendarHeatmapCellSize.js'
import { orderNamesByFieldOptions } from '@/utils/enumOptionOrder.js'
import {
  getEnumChartPalette,
  DEFAULT_ENUM_CHART_COLORS,
} from '@/utils/enumChartColors.js'

const props = defineProps({
  enumTrend: { type: Object, default: null },
  rangeStart: { type: String, required: true },
  rangeEnd: { type: String, required: true },
  /** 與欄位設定 options 相同順序；有則 visualMap／色碼與分布圖一致 */
  optionOrder: {
    type: Array,
    default: () => [],
  },
})

const { wrapRef, cellSize, hostStyle } = useCalendarHeatmapCellSize(
  toRef(props, 'rangeStart'),
  toRef(props, 'rangeEnd'),
  {
    calendarTop: 72,
    visualMapReserve: 102,
    labelSlack: 22,
    minHostHeight: 212,
  }
)

function buildOptionList(raw, fieldOrder) {
  const trendData =
    raw?.trendData && typeof raw.trendData === 'object' ? raw.trendData : {}
  const apiOptions = Array.isArray(raw?.options) ? raw.options : []
  const optionSet = new Set(apiOptions)
  for (const dk of Object.keys(trendData)) {
    const row = trendData[dk]
    if (row && typeof row === 'object') {
      Object.keys(row).forEach((k) => optionSet.add(k))
    }
  }
  const names = [...optionSet]
  if (fieldOrder?.length > 0) {
    return orderNamesByFieldOptions(names, fieldOrder)
  }
  if (apiOptions.length > 0) {
    return apiOptions.filter((o) => optionSet.has(o))
  }
  return names
}

/** 該日次數最多者；平手取 optionList 中較前項 */
function dominantOptionForDay(row, optionList) {
  if (!row || typeof row !== 'object') return null
  let best = null
  let bestCount = -1
  for (const opt of optionList) {
    const c = Number(row[opt] ?? 0)
    if (c > bestCount) {
      bestCount = c
      best = opt
    }
  }
  if (bestCount <= 0 || best == null) return null
  return best
}

const payload = computed(() => {
  const raw = props.enumTrend
  const trendData =
    raw?.trendData && typeof raw.trendData === 'object' ? raw.trendData : {}
  const optionList = buildOptionList(raw, props.optionOrder)
  const optionToIndex = new Map(
    optionList.map((opt, i) => [opt, i + 1])
  )

  const rows = []
  for (const dk of Object.keys(trendData).sort()) {
    const day = String(dk).slice(0, 10)
    const dominant = dominantOptionForDay(trendData[dk], optionList)
    if (dominant == null) continue
    const idx = optionToIndex.get(dominant)
    if (idx == null) continue
    rows.push([day, idx])
  }

  const fieldOrder =
    props.optionOrder?.length > 0 ? props.optionOrder : optionList
  const palette = getEnumChartPalette(fieldOrder.length)
  const pieces = optionList.map((opt, i) => {
    const idx = fieldOrder.indexOf(opt)
    const color =
      idx >= 0 && idx < palette.length
        ? palette[idx]
        : DEFAULT_ENUM_CHART_COLORS[i % DEFAULT_ENUM_CHART_COLORS.length]
    return {
      value: i + 1,
      label: opt,
      color,
    }
  })

  return { rows, optionList, pieces }
})

const hasData = computed(
  () => payload.value.rows.length > 0 && payload.value.optionList.length > 0
)

const chartOption = computed(() => {
  const { rows, optionList, pieces } = payload.value
  const indexToLabel = new Map(
    optionList.map((opt, i) => [i + 1, opt])
  )

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      formatter(p) {
        const idx = p.data?.[1]
        const day = p.data?.[0]
        const label = indexToLabel.get(idx) ?? '—'
        return `${day}<br/>選項：<strong>${label}</strong>`
      },
    },
    toolbox: {
      right: 12,
      top: 0,
      feature: {
        saveAsImage: { title: '儲存圖片', name: '選項日曆' },
      },
    },
    visualMap: {
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 24,
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 8,
      pieces,
    },
    calendar: {
      orient: 'vertical',
      top: 72,
      left: 'center',
      range: [props.rangeStart, props.rangeEnd],
      cellSize: cellSize.value,
      itemStyle: { borderWidth: 0.5 },
      splitLine: { lineStyle: { color: '#e0e0e0' } },
      yearLabel: { show: true },
      dayLabel: { firstDay: 1, nameMap: 'zh-cn' },
      monthLabel: { nameMap: 'zh-cn' },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: rows,
      },
    ],
  }
})
</script>

<style scoped>
.echart-panel {
  margin-bottom: 1.5rem;
}

.echart-title {
  margin: 0 0 0.35rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: #333;
}

.echart-hint {
  margin: 0 0 0.75rem;
  font-size: 0.8125rem;
  color: #666;
  line-height: 1.45;
}

.chart-calendar-host {
  box-sizing: border-box;
}

.chart-calendar {
  width: 100%;
  height: 100%;
}

.chart-calendar-host .echart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 12rem;
  margin: 0;
}

.echart-empty {
  margin: 0;
  padding: 2rem;
  text-align: center;
  color: #888;
  background: #f9f9f9;
  border-radius: 8px;
}
</style>
