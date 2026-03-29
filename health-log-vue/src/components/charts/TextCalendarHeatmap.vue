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
      <p v-else class="echart-empty">此區間無文字紀錄可繪製熱力圖</p>
    </div>
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import VChart from 'vue-echarts'
import { useCalendarHeatmapCellSize } from '@/composables/useCalendarHeatmapCellSize.js'

const props = defineProps({
  /** 後端 timelineData：日期字串 -> 文字內容 */
  timelineData: { type: Object, default: () => ({}) },
  rangeStart: { type: String, required: true },
  rangeEnd: { type: String, required: true },
})

const { wrapRef, cellSize, hostStyle } = useCalendarHeatmapCellSize(
  toRef(props, 'rangeStart'),
  toRef(props, 'rangeEnd')
)

const heatmapPayload = computed(() => {
  const tl = props.timelineData || {}
  const rows = []
  let minV = Infinity
  let maxV = -Infinity
  for (const [dayRaw, text] of Object.entries(tl)) {
    const day = String(dayRaw).slice(0, 10)
    const len = text != null ? String(text).length : 0
    rows.push([day, len])
    minV = Math.min(minV, len)
    maxV = Math.max(maxV, len)
  }
  return { rows, minV, maxV }
})

const hasData = computed(() => heatmapPayload.value.rows.length > 0)

const chartOption = computed(() => {
  const { rows, minV, maxV } = heatmapPayload.value
  let vmin = minV
  let vmax = maxV
  if (vmin === vmax) {
    vmax = vmin === 0 ? 1 : vmin + 1
  }

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      formatter(p) {
        const len = p.data?.[1]
        const day = p.data?.[0]
        return `${day}<br/>字數：${len != null ? len : '—'}`
      },
    },
    toolbox: {
      right: 12,
      top: 0,
      feature: {
        saveAsImage: { title: '儲存圖片', name: '字數熱力圖' },
      },
    },
    visualMap: {
      min: vmin,
      max: vmax,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 28,
      text: ['多', '少'],
    },
    calendar: {
      orient: 'vertical',
      top: 56,
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
  margin: 0 0 0.75rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: #333;
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
