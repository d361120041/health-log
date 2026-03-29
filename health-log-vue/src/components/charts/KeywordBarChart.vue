<template>
  <div class="echart-panel">
    <h3 class="echart-title">關鍵字頻率圖（前 10 名）</h3>
    <VChart
      v-if="categories.length"
      class="chart chart-bar"
      :option="chartOption"
      autoresize
    />
    <p v-else class="echart-empty">無關鍵字資料</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps({
  /** 關鍵字 -> 次數（已排序或可任意順序，元件內會依次數重排並取前 10） */
  keywordFrequency: {
    type: Object,
    default: () => ({}),
  },
})

const sortedEntries = computed(() => {
  const obj = props.keywordFrequency || {}
  return Object.entries(obj)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
})

const categories = computed(() => sortedEntries.value.map(([k]) => k))
const counts = computed(() => sortedEntries.value.map(([, v]) => Number(v) || 0))

const chartOption = computed(() => ({
  textStyle: {
    fontFamily:
      'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
  },
  toolbox: {
    right: 12,
    top: 0,
    feature: {
      saveAsImage: { title: '儲存圖片', name: '關鍵字頻率' },
    },
  },
  grid: {
    left: 12,
    right: 24,
    top: 36,
    bottom: 8,
    containLabel: true,
  },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { type: 'dashed', opacity: 0.35 } },
  },
  yAxis: {
    type: 'category',
    data: categories.value,
    inverse: true,
    axisLabel: {
      width: 120,
      overflow: 'truncate',
      ellipsis: '…',
    },
  },
  series: [
    {
      name: '次數',
      type: 'bar',
      data: counts.value,
      barMaxWidth: 28,
      itemStyle: {
        color: '#4a90e2',
        borderRadius: [0, 4, 4, 0],
      },
    },
  ],
}))
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

.chart-bar {
  width: 100%;
  height: 360px;
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
