/**
 * 將逐日報表資料依「週一為週界」或「曆月」分桶彙總（本地日曆）。
 */

import { orderNamesByFieldOptions } from '@/utils/enumOptionOrder.js'

/** @param {string} ymd */
export function parseLocalDate(ymd) {
  if (ymd == null) return null
  const s = String(ymd).slice(0, 10)
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s)
  if (!m) return null
  const y = Number(m[1])
  const mo = Number(m[2])
  const d = Number(m[3])
  if (!y || mo < 1 || mo > 12 || d < 1 || d > 31) return null
  const dt = new Date(y, mo - 1, d)
  if (
    dt.getFullYear() !== y ||
    dt.getMonth() !== mo - 1 ||
    dt.getDate() !== d
  ) {
    return null
  }
  return dt
}

function toYMD(d) {
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${mo}-${day}`
}

/** 該日所屬週的週一（本地） */
export function mondayOfWeekContaining(date) {
  const d = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate()
  )
  const day = d.getDay()
  const offset = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + offset)
  return d
}

/** 週一 YYYY-MM-DD */
export function weekBucketKeyFromDate(date) {
  return toYMD(mondayOfWeekContaining(date))
}

/** yyyy-MM */
export function monthBucketKeyFromDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

/** 例：3/3～3/9 */
export function formatWeekRangeLabel(mondayYmd) {
  const d0 = parseLocalDate(mondayYmd)
  if (!d0) return mondayYmd
  const d1 = new Date(d0)
  d1.setDate(d1.getDate() + 6)
  const f = (x) => `${x.getMonth() + 1}/${x.getDate()}`
  return `${f(d0)}～${f(d1)}`
}

/** 例：2025年3月 */
export function formatMonthLabel(monthKey) {
  const m = /^(\d{4})-(\d{2})$/.exec(monthKey)
  if (!m) return monthKey
  return `${m[1]}年${Number(m[2])}月`
}

/**
 * @param {'week'|'month'} period
 * @param {Date} date
 * @returns {{ sortKey: string, periodLabel: string }}
 */
export function bucketForDate(period, date) {
  if (period === 'week') {
    const sortKey = weekBucketKeyFromDate(date)
    return {
      sortKey,
      periodLabel: formatWeekRangeLabel(sortKey),
    }
  }
  const sortKey = monthBucketKeyFromDate(date)
  return {
    sortKey,
    periodLabel: formatMonthLabel(sortKey),
  }
}

/**
 * NUMBER 逐日趨勢 → 週／月列
 * @param {Array<{ date: string, value: string|number }>} trendData
 * @param {'week'|'month'} period
 */
export function aggregateNumberTrendByPeriod(trendData, period) {
  const list = Array.isArray(trendData) ? trendData : []
  /** @type {Map<string, { values: number[], periodLabel: string }>} */
  const map = new Map()

  for (const pt of list) {
    const d = parseLocalDate(pt?.date)
    if (!d) continue
    const n = Number(pt?.value)
    if (!Number.isFinite(n)) continue
    const { sortKey, periodLabel } = bucketForDate(period, d)
    let cell = map.get(sortKey)
    if (!cell) {
      cell = { values: [], periodLabel }
      map.set(sortKey, cell)
    }
    cell.values.push(n)
  }

  return [...map.entries()]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([sortKey, cell]) => {
      const { values, periodLabel } = cell
      const count = values.length
      const sum = values.reduce((s, v) => s + v, 0)
      const avg = count ? sum / count : 0
      const min = Math.min(...values)
      const max = Math.max(...values)
      return {
        sortKey,
        periodLabel,
        count,
        sum,
        avg,
        min,
        max,
      }
    })
}

/**
 * 與 EnumOptionCalendarHeatmap 一致：欄位順序 → API options → 其餘鍵序
 * @param {Record<string, Record<string, number>>} trendData
 * @param {string[]} apiOptions
 * @param {string[]} fieldOrder
 * @returns {string[]}
 */
export function getEnumColumnOrder(trendData, apiOptions, fieldOrder) {
  const raw = trendData && typeof trendData === 'object' ? trendData : {}
  const apiOpts = Array.isArray(apiOptions) ? apiOptions : []
  const optionSet = new Set(apiOpts)
  for (const dk of Object.keys(raw)) {
    const row = raw[dk]
    if (row && typeof row === 'object') {
      Object.keys(row).forEach((k) => optionSet.add(k))
    }
  }
  const names = [...optionSet]
  const fo = Array.isArray(fieldOrder) ? fieldOrder : []
  if (fo.length > 0) {
    return orderNamesByFieldOptions(names, fo)
  }
  if (apiOpts.length > 0) {
    return apiOpts.filter((o) => optionSet.has(o))
  }
  return names
}

/**
 * ENUM 逐日趨勢 → 週／月列（各選項次數）
 * @param {Record<string, Record<string, number>>} trendData 日字串 → { 選項: 次數 }
 * @param {string[]} columnOrder 橫向欄位順序（見 getEnumColumnOrder）
 * @param {'week'|'month'} period
 */
export function aggregateEnumTrendByPeriod(trendData, columnOrder, period) {
  const raw = trendData && typeof trendData === 'object' ? trendData : {}
  const columns = Array.isArray(columnOrder) ? columnOrder : []
  /** @type {Map<string, Map<string, number>>} */
  const byBucket = new Map()
  /** @type {Map<string, string>} */
  const labels = new Map()

  for (const dayKey of Object.keys(raw)) {
    const d = parseLocalDate(dayKey)
    if (!d) continue
    const { sortKey, periodLabel } = bucketForDate(period, d)
    labels.set(sortKey, periodLabel)
    let optMap = byBucket.get(sortKey)
    if (!optMap) {
      optMap = new Map()
      byBucket.set(sortKey, optMap)
    }
    const dayRow = raw[dayKey]
    if (!dayRow || typeof dayRow !== 'object') continue
    for (const [opt, c] of Object.entries(dayRow)) {
      const add = Number(c) || 0
      if (add <= 0) continue
      optMap.set(opt, (optMap.get(opt) ?? 0) + add)
    }
  }

  let cols = columns
  if (!cols.length) {
    const optionSet = new Set()
    for (const m of byBucket.values()) {
      for (const k of m.keys()) optionSet.add(k)
    }
    cols = [...optionSet]
  }

  return [...byBucket.entries()]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([sortKey, optMap]) => {
      /** @type {Record<string, number>} */
      const counts = {}
      let total = 0
      for (const col of cols) {
        const v = optMap.get(col) ?? 0
        counts[col] = v
        total += v
      }
      return {
        sortKey,
        periodLabel: labels.get(sortKey) ?? sortKey,
        columns: cols,
        counts,
        total,
      }
    })
}

/**
 * TEXT timeline → 週／月列（僅有文字的日）
 * @param {Record<string, string>} timelineData
 * @param {'week'|'month'} period
 */
export function aggregateTextTimelineByPeriod(timelineData, period) {
  const raw = timelineData && typeof timelineData === 'object' ? timelineData : {}
  /** @type {Map<string, { lengths: number[], periodLabel: string }>} */
  const map = new Map()

  for (const dayKey of Object.keys(raw)) {
    const d = parseLocalDate(dayKey)
    if (!d) continue
    const text = raw[dayKey]
    if (text == null || String(text).trim() === '') continue
    const len = String(text).length
    const { sortKey, periodLabel } = bucketForDate(period, d)
    let cell = map.get(sortKey)
    if (!cell) {
      cell = { lengths: [], periodLabel }
      map.set(sortKey, cell)
    }
    cell.lengths.push(len)
  }

  return [...map.entries()]
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([sortKey, cell]) => {
      const { lengths, periodLabel } = cell
      const count = lengths.length
      const totalChars = lengths.reduce((s, v) => s + v, 0)
      const avgLength = count ? totalChars / count : 0
      const minLength = Math.min(...lengths)
      const maxLength = Math.max(...lengths)
      return {
        sortKey,
        periodLabel,
        count,
        totalChars,
        avgLength,
        minLength,
        maxLength,
      }
    })
}
