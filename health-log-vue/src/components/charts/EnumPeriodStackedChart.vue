<template>
  <div class="echart-panel">
    <h3 class="echart-title">{{ title }}</h3>
    <VChart
      v-if="hasData"
      class="chart chart-period-stack"
      :option="chartOption"
      autoresize
    />
    <p v-else class="echart-empty">無週／月彙總資料可繪製</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { colorForEnumOption } from '@/utils/enumChartColors.js'

const props = defineProps({
  title: { type: String, default: '選項次數（堆疊）' },
  /** 每列含 periodLabel、counts、columns */
  rows: {
    type: Array,
    default: () => [],
  },
  /** 與表格欄位順序一致，用於色票 */
  optionOrder: {
    type: Array,
    default: () => [],
  },
})

const hasData = computed(() => {
  const r = props.rows
  return Array.isArray(r) && r.length > 0 && r[0]?.columns?.length > 0
})

const chartOption = computed(() => {
  const rows = props.rows || []
  const order = props.optionOrder || []
  if (!rows.length) return {}

  const columns = rows[0].columns || []
  const categories = rows.map((x) => x.periodLabel)

  const series = columns.map((name, i) => ({
    name,
    type: 'bar',
    stack: 'total',
    emphasis: { focus: 'series' },
    data: rows.map((row) => Number(row.counts?.[name] ?? 0)),
    itemStyle: {
      color: colorForEnumOption(name, order, i),
    },
  }))

  return {
    textStyle: {
      fontFamily:
        'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans TC", sans-serif',
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      left: 'center',
      bottom: 8,
      itemWidth: 10,
      itemHeight: 10,
    },
    grid: {
      left: 48,
      right: 24,
      top: 24,
      bottom: columns.length > 6 ? 120 : 80,
      containLabel: false,
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        interval: 0,
        rotate: categories.length > 8 ? 32 : 0,
        fontSize: 11,
      },
    },
    yAxis: {
      type: 'value',
      name: '次數',
      minInterval: 1,
    },
    series,
  }
})
</script>

<style scoped>
.chart-period-stack {
  width: 100%;
  min-height: 320px;
  height: 360px;
}
.echart-panel {
  margin-top: 1rem;
}
.echart-title {
  font-size: 1rem;
  margin: 0 0 0.5rem;
  font-weight: 600;
}
.echart-empty {
  color: var(--color-text-muted, #666);
  padding: 1rem 0;
}
</style>
