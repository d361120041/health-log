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
      <p v-else class="echart-empty">此區間無有效數值可繪製熱力圖</p>
    </div>
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import VChart from 'vue-echarts'
import { useCalendarHeatmapCellSize } from '@/composables/useCalendarHeatmapCellSize.js'

const props = defineProps({
  trendData: { type: Array, default: () => [] },
  /** YYYY-MM-DD */
  rangeStart: { type: String, required: true },
  /** YYYY-MM-DD */
  rangeEnd: { type: String, required: true },
  /** visualMap 標題／tooltip 用 */
  valueLabel: { type: String, default: '數值' },
})

const { wrapRef, cellSize, hostStyle } = useCalendarHeatmapCellSize(
  toRef(props, 'rangeStart'),
  toRef(props, 'rangeEnd')
)

function toYMD(dateVal) {
  if (dateVal == null) return null
  if (typeof dateVal === 'string') return dateVal.slice(0, 10)
  return null
}

const heatmapPayload = computed(() => {
  const rows = []
  let minV = Infinity
  let maxV = -Infinity
  for (const p of props.trendData || []) {
    const day = toYMD(p?.date)
    if (!day) continue
    const n = Number(p?.value)
    if (!Number.isFinite(n)) continue
    rows.push([day, n])
    minV = Math.min(minV, n)
    maxV = Math.max(maxV, n)
  }
  return { rows, minV, maxV }
})

const hasData = computed(() => heatmapPayload.value.rows.length > 0)

const chartOption = computed(() => {
  const { rows, minV, maxV } = heatmapPayload.value
  let vmin = minV
  let vmax = maxV
  if (vmin === vmax) {
    vmax = vmin === 0 ? 1 : vmin + Math.abs(vmin) * 0.05 + 0.01
  }

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      formatter(p) {
        const v = p.data?.[1]
        const day = p.data?.[0]
        const formatted =
          v != null && Number.isFinite(v)
            ? Number(v).toLocaleString('zh-TW', {
                maximumFractionDigits: 4,
              })
            : '—'
        return `${day}<br/>${props.valueLabel}：${formatted}`
      },
    },
    toolbox: {
      right: 12,
      top: 0,
      feature: {
        saveAsImage: { title: '儲存圖片', name: '數值熱力圖' },
      },
    },
    visualMap: {
      min: vmin,
      max: vmax,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 28,
      text: ['高', '低'],
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
