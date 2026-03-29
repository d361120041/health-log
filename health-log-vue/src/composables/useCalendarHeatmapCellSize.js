import {
  ref,
  computed,
  onMounted,
  onUnmounted,
  watch,
  nextTick,
} from 'vue'
import {
  CALENDAR_CELL_MIN,
  CALENDAR_CELL_MAX,
} from '@/components/charts/calendarHeatmapConfig.js'

/**
 * 依容器尺寸與查詢日期區間估算週列數（略放大，避免低估導致格子過大超出視窗）
 */
function approxWeekRows(rangeStart, rangeEnd) {
  if (!rangeStart || !rangeEnd) return 10
  const s = new Date(`${rangeStart}T12:00:00`)
  const e = new Date(`${rangeEnd}T12:00:00`)
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return 10
  const days = Math.max(1, Math.round((e - s) / 86400000) + 1)
  return Math.max(6, Math.ceil(days / 7) + 4)
}

/**
 * 與日曆 heatmap 的 dayLabel.firstDay: 1（週一）一致：區間實際跨幾個「週列」。
 * 僅用於 host 高度，避免用 inflated 週數撐高 canvas 內留白。
 */
function calendarWeekRowsForRange(rangeStart, rangeEnd) {
  if (!rangeStart || !rangeEnd) return 1
  const s = new Date(`${rangeStart}T12:00:00`)
  const e = new Date(`${rangeEnd}T12:00:00`)
  if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) return 1
  if (e < s) return 1

  const dowS = (s.getDay() + 6) % 7
  const weekStartS = new Date(s)
  weekStartS.setHours(0, 0, 0, 0)
  weekStartS.setDate(s.getDate() - dowS)

  const dowE = (e.getDay() + 6) % 7
  const weekStartE = new Date(e)
  weekStartE.setHours(0, 0, 0, 0)
  weekStartE.setDate(e.getDate() - dowE)

  const msPerWeek = 7 * 86400000
  const span = Math.floor((weekStartE - weekStartS) / msPerWeek) + 1
  return Math.max(1, span)
}

/**
 * 量測容器寬度，並依視窗高度上限計算正方形 cellSize 與貼合內容的 host 高度，避免日曆與 visualMap 之間大片留白。
 *
 * @param {import('vue').Ref<string>} rangeStartRef
 * @param {import('vue').Ref<string>} rangeEndRef
 * @param {{
 *   gutterX?: number,
 *   calendarTop?: number,
 *   visualMapReserve?: number,
 *   labelSlack?: number,
 *   minHostHeight?: number,
 *   maxHostHeightPx?: number,
 *   maxViewportFraction?: number,
 *   minCell?: number,
 *   maxCell?: number,
 * }} [layout]
 */
export function useCalendarHeatmapCellSize(
  rangeStartRef,
  rangeEndRef,
  layout = {}
) {
  const wrapRef = ref(null)
  const boxW = ref(0)
  const viewportH = ref(
    typeof window !== 'undefined' ? window.innerHeight : 800
  )

  const gutterX = layout.gutterX ?? 80
  const calendarTop = layout.calendarTop ?? 56
  const visualMapReserve = layout.visualMapReserve ?? 74
  const labelSlack = layout.labelSlack ?? 20
  const minHostHeight = layout.minHostHeight ?? 198
  const maxHostHeightPx = layout.maxHostHeightPx ?? 920
  const maxViewportFraction = layout.maxViewportFraction ?? 0.85
  const minCell = layout.minCell ?? CALENDAR_CELL_MIN
  const maxCell = layout.maxCell ?? CALENDAR_CELL_MAX

  function maxChartHeight() {
    return Math.min(
      maxHostHeightPx,
      Math.round(viewportH.value * maxViewportFraction)
    )
  }

  function measure() {
    const el = wrapRef.value
    if (!el) return
    const w = el.clientWidth
    if (boxW.value === w) return
    boxW.value = w
  }

  let ro = null
  let rafId = null
  function scheduleMeasure() {
    if (rafId != null) cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(() => {
      rafId = null
      measure()
    })
  }

  function onWindowResize() {
    viewportH.value =
      typeof window !== 'undefined' ? window.innerHeight : viewportH.value
    scheduleMeasure()
  }

  onMounted(() => {
    nextTick(() => {
      measure()
      const el = wrapRef.value
      if (el && typeof ResizeObserver !== 'undefined') {
        ro = new ResizeObserver(() => scheduleMeasure())
        ro.observe(el)
      }
    })
    window.addEventListener('resize', onWindowResize)
  })

  onUnmounted(() => {
    if (rafId != null) cancelAnimationFrame(rafId)
    ro?.disconnect()
    window.removeEventListener('resize', onWindowResize)
  })

  watch(
    [rangeStartRef, rangeEndRef],
    () => {
      nextTick(() => measure())
    },
    { flush: 'post' }
  )

  const cellSize = computed(() => {
    const rows = approxWeekRows(rangeStartRef.value, rangeEndRef.value)
    const w = boxW.value > 0 ? boxW.value : 720
    const maxByW = Math.floor((w - gutterX) / 7)

    const maxH = maxChartHeight()
    const bodyBudget = Math.max(
      minCell * rows,
      maxH - calendarTop - visualMapReserve - labelSlack
    )
    const maxByH = Math.floor(bodyBudget / Math.max(rows, 1))

    let s = Math.min(maxByW, maxByH)
    if (!Number.isFinite(s)) s = minCell
    s = Math.max(minCell, Math.min(maxCell, Math.round(s)))
    return s
  })

  /** 貼合日曆本體 + visualMap，避免容器過高 */
  const hostHeightPx = computed(() => {
    const rows = calendarWeekRowsForRange(
      rangeStartRef.value,
      rangeEndRef.value
    )
    const cs = cellSize.value
    const raw =
      calendarTop + rows * cs + labelSlack + visualMapReserve
    const maxH = maxChartHeight()
    return Math.round(
      Math.min(Math.max(raw, minHostHeight), maxH)
    )
  })

  const hostStyle = computed(() => ({
    width: '100%',
    height: `${hostHeightPx.value}px`,
    minHeight: `${minHostHeight}px`,
  }))

  return { wrapRef, cellSize, hostStyle }
}
