<template>
  <div class="echart-panel">
    <h3 class="echart-title">趨勢圖</h3>
    <VChart
      v-if="hasPlottableData"
      class="chart"
      :option="chartOption"
      autoresize
    />
    <p v-else class="echart-empty">此區間無有效數值可繪製趨勢</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps({
  /** @type {{ date: string, value?: string | null }[]} */
  trendData: {
    type: Array,
    default: () => [],
  },
  /** Y 軸名稱（例如單位） */
  yAxisName: {
    type: String,
    default: '',
  },
})

function formatAxisDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return String(dateStr)
  return d.toLocaleDateString('zh-TW', {
    month: 'numeric',
    day: 'numeric',
  })
}

const hasPlottableData = computed(() => {
  const list = props.trendData || []
  return list.some((p) => {
    if (p?.value == null || p.value === '') return false
    const n = Number(p.value)
    return Number.isFinite(n)
  })
})

const chartOption = computed(() => {
  const list = props.trendData || []
  const dates = list.map((p) => formatAxisDate(p?.date))
  const values = list.map((p) => {
    if (p?.value == null || p.value === '') return null
    const n = Number(p.value)
    return Number.isFinite(n) ? n : null
  })

  const rotate = dates.length > 16 ? 40 : dates.length > 10 ? 28 : 0

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        const item = Array.isArray(params) ? params[0] : params
        if (!item) return ''
        const idx = item.dataIndex
        const raw = list[idx]
        const day = raw?.date
          ? new Date(raw.date).toLocaleDateString('zh-TW')
          : item.axisValue
        const val = item.data
        const text =
          val != null && val !== ''
            ? Number(val).toLocaleString('zh-TW', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 4,
              })
            : '—'
        return `${day}<br/>數值：${text}`
      },
    },
    toolbox: {
      right: 12,
      top: 0,
      feature: {
        saveAsImage: { title: '儲存圖片', name: '趨勢圖' },
        dataZoom: {
          title: { zoom: '框選縮放', back: '還原縮放' },
        },
      },
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      {
        type: 'slider',
        bottom: 8,
        height: 22,
        start: 0,
        end: 100,
      },
    ],
    grid: {
      left: 52,
      right: 20,
      top: 40,
      bottom: 56,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLabel: { rotate },
    },
    yAxis: {
      type: 'value',
      name: props.yAxisName || undefined,
      nameGap: 12,
      scale: true,
      splitLine: {
        lineStyle: { type: 'dashed', opacity: 0.35 },
      },
    },
    series: [
      {
        name: '數值',
        type: 'line',
        smooth: true,
        showSymbol: dates.length <= 31,
        symbolSize: 6,
        data: values,
        connectNulls: false,
        lineStyle: { color: '#4a90e2', width: 2 },
        itemStyle: { color: '#4a90e2' },
        areaStyle: {
          color: 'rgba(74, 144, 226, 0.15)',
        },
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

.chart {
  width: 100%;
  height: 380px;
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
