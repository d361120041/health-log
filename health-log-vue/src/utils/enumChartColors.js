/**
 * 無法依選項個數產生色票時的後備（例如 n≤0、或名稱對不到欄位順序時的輪詢）。
 */
export const DEFAULT_ENUM_CHART_COLORS = [
  '#4a90e2',
  '#7b68ee',
  '#50c878',
  '#e6a23c',
  '#e74c3c',
  '#17a2b8',
  '#9b59b6',
  '#3498db',
  '#1abc9c',
  '#f39c12',
]

/**
 * 單色漸層的色相／飽和度（僅調亮度做深→淺）。
 * 青綠 teal，與常見報表藍區隔；若要中性灰階可改 MONO_S = 0（MONO_H 可忽略）。
 */
const MONO_H = 168
const MONO_S = 50

/**
 * HSL(0–360, 0–100, 0–100) → [r,g,b] 0–255
 */
function hslToRgb(h, s, l) {
  h = ((h % 360) + 360) % 360
  s = Math.max(0, Math.min(100, s)) / 100
  l = Math.max(0, Math.min(100, l)) / 100

  if (s === 0) {
    const v = Math.round(l * 255)
    return [v, v, v]
  }

  const hue2rgb = (p, q, t) => {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1 / 6) return p + (q - p) * 6 * t
    if (t < 1 / 2) return q
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
    return p
  }

  const hn = h / 360
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s
  const p = 2 * l - q
  const r = hue2rgb(p, q, hn + 1 / 3)
  const g = hue2rgb(p, q, hn)
  const b = hue2rgb(p, q, hn - 1 / 3)
  return [
    Math.round(r * 255),
    Math.round(g * 255),
    Math.round(b * 255),
  ]
}

function rgbToHex(r, g, b) {
  return (
    '#' +
    [r, g, b]
      .map((x) =>
        Math.max(0, Math.min(255, x)).toString(16).padStart(2, '0')
      )
      .join('')
  )
}

/**
 * 欄位順序第 1 個選項最深、最後一個最淺（同一色相、亮度遞增）。
 * @param {number} n
 * @returns {string[]}
 */
function monoDeepToLightPalette(n) {
  if (n <= 0) return [...DEFAULT_ENUM_CHART_COLORS]
  /** 最深 / 最淺亮度（%），避免極端全黑或難以辨識 */
  const lMin = 28
  const lMax = 80
  const out = []
  for (let i = 0; i < n; i++) {
    const t = n === 1 ? 0.5 : i / (n - 1)
    const lightness = lMin + t * (lMax - lMin)
    const [r, g, b] = hslToRgb(MONO_H, MONO_S, lightness)
    out.push(rgbToHex(r, g, b))
  }
  return out
}

/**
 * 依欄位定義的選項個數產生色票：同一色相、由深到淺。
 * @param {number} fieldOptionCount
 * @returns {string[]}
 */
export function getEnumChartPalette(fieldOptionCount) {
  const n = Math.floor(Number(fieldOptionCount)) || 0
  if (n <= 0) return [...DEFAULT_ENUM_CHART_COLORS]
  return monoDeepToLightPalette(n)
}

/**
 * 依選項在欄位順序中的索引取色；無欄位順序時依 fallbackIndex 用預設色票輪詢。
 * @param {string} optionName
 * @param {string[]} fieldOrder
 * @param {number} [fallbackIndex]
 */
export function colorForEnumOption(optionName, fieldOrder, fallbackIndex = 0) {
  if (fieldOrder?.length) {
    const palette = getEnumChartPalette(fieldOrder.length)
    const idx = fieldOrder.indexOf(optionName)
    if (idx >= 0 && idx < palette.length) return palette[idx]
  }
  const def = DEFAULT_ENUM_CHART_COLORS
  return def[fallbackIndex % def.length]
}

/**
 * 與黑色混合（長條漸層右側較亮、左側略深）。
 * @param {string} hex #rrggbb
 * @param {number} ratio 0–1，越大越暗
 */
export function mixHexWithBlack(hex, ratio = 0.22) {
  const h = hex.replace('#', '')
  if (h.length !== 6) return hex
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  const t = Math.max(0, Math.min(1, ratio))
  const lerp = (a) => Math.round(a * (1 - t))
  return rgbToHex(lerp(r), lerp(g), lerp(b))
}
