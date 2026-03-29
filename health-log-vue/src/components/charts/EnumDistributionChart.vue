<template>
  <div class="echart-panel">
    <h3 class="echart-title">分布圖</h3>
    <VChart
      v-if="pieSeriesData.length"
      class="chart chart-donut"
      :option="chartOption"
      autoresize
    />
    <p v-else class="echart-empty">無分佈資料</p>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { orderNamesByFieldOptions } from '@/utils/enumOptionOrder.js'
import {
  colorForEnumOption,
  getEnumChartPalette,
} from '@/utils/enumChartColors.js'

/** 窄螢幕：外側標籤／引線易超出寬度，改環內僅顯示占比 */
const viewportW = ref(
  typeof window !== 'undefined' ? window.innerWidth : 1024
)
function syncViewportW() {
  if (typeof window !== 'undefined') viewportW.value = window.innerWidth
}
onMounted(() => {
  syncViewportW()
  window.addEventListener('resize', syncViewportW)
})
onUnmounted(() => {
  window.removeEventListener('resize', syncViewportW)
})

const props = defineProps({
  /** 選項 -> 次數 */
  distribution: {
    type: Object,
    default: () => ({}),
  },
  /** 選項 -> 百分比 */
  percentages: {
    type: Object,
    default: () => ({}),
  },
  /** 與欄位設定 options 相同順序；有則圓餅／圖例依此排列，無則依次數 */
  optionOrder: {
    type: Array,
    default: () => [],
  },
})

const pieSeriesData = computed(() => {
  const dist = props.distribution || {}
  const withData = Object.entries(dist).filter(
    ([, count]) => Number(count) > 0
  )
  const names = withData.map(([name]) => name)
  const ordered =
    props.optionOrder?.length > 0
      ? orderNamesByFieldOptions(names, props.optionOrder)
      : [...names].sort((a, b) => Number(dist[b]) - Number(dist[a]))
  return ordered.map((name) => ({
    name,
    value: Number(dist[name]),
  }))
})

function displayPercentForSlice(p, pctMap) {
  const api = pctMap[p.name]
  if (api != null) return Math.round(Number(api))
  return Math.round(Number(p.percent) || 0)
}

const chartOption = computed(() => {
  const raw = pieSeriesData.value
  const order = props.optionOrder || []
  const paletteNoOrder = getEnumChartPalette(raw.length)
  const data = raw.map((d, i) => ({
    ...d,
    itemStyle: {
      color:
        order.length > 0
          ? colorForEnumOption(d.name, order, i)
          : paletteNoOrder[i % paletteNoOrder.length],
    },
  }))
  const pct = props.percentages || {}
  const narrow = viewportW.value < 640

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const name = p.name
        const c = p.value
        const displayPct = displayPercentForSlice(p, pct)
        const lines = [
          `<strong>${name}</strong>`,
          `次數：${c}`,
          `占比：${displayPct}%`,
        ]
        return lines.join('<br/>')
      },
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      left: 'center',
      bottom: 8,
      itemWidth: 10,
      itemHeight: 10,
      ...(narrow ? { textStyle: { fontSize: 11 } } : {}),
    },
    toolbox: {
      right: 12,
      top: 0,
      feature: {
        saveAsImage: { title: '儲存圖片', name: '分布圖' },
      },
    },
    series: [
      {
        name: '選項',
        type: 'pie',
        radius: narrow ? ['40%', '58%'] : ['42%', '62%'],
        center: ['50%', narrow ? '44%' : '46%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: narrow
          ? {
              position: 'inside',
              fontSize: 10,
              formatter: (p) =>
                `${displayPercentForSlice(p, pct)}%`,
            }
          : {
              formatter: (p) =>
                `${p.name}\n${displayPercentForSlice(p, pct)}%`,
            },
        labelLine: {
          show: !narrow,
        },
        emphasis: {
          label: narrow
            ? {
                show: true,
                fontSize: 11,
                fontWeight: 'bold',
                position: 'inside',
                formatter: (p) =>
                  `${displayPercentForSlice(p, pct)}%`,
              }
            : {
                show: true,
                fontSize: 14,
                fontWeight: 'bold',
                formatter: (p) =>
                  `${p.name}\n${displayPercentForSlice(p, pct)}%`,
              },
          itemStyle: {
            shadowBlur: 12,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)',
          },
        },
        data,
      },
    ],
  }
})
</script>

<style scoped>
.echart-panel {
  margin-bottom: 1.5rem;
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.echart-title {
  margin: 0 0 0.75rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: #333;
}

.chart-donut {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 400px;
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
