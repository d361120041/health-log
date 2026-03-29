/**
 * 與 DynamicRecordForm 的 ENUM 解析一致：由欄位 options 字串得到選項文字陣列（順序即欄位定義順序）。
 * @param {string|null|undefined} optionsString
 * @returns {string[]}
 */
export function parseEnumOptionLabels(optionsString) {
  if (Array.isArray(optionsString)) {
    return optionsString
      .map((opt) =>
        typeof opt === 'string' ? opt : String(opt?.value ?? opt?.label ?? opt ?? '')
      )
      .filter(Boolean)
  }
  if (!optionsString || typeof optionsString !== 'string') return []
  const s = optionsString.trim()
  if (!s) return []
  try {
    const parsed = JSON.parse(s)
    if (Array.isArray(parsed)) {
      return parsed
        .map((opt) => {
          if (typeof opt === 'string') return opt
          const v = opt?.value ?? opt?.label ?? opt
          return v != null ? String(v) : ''
        })
        .filter(Boolean)
    }
  } catch {
    // 非 JSON：逗號分隔
  }
  return s.split(',').map((opt) => opt.trim()).filter(Boolean)
}

/**
 * 依欄位定義順序排列名稱；未定義於 order 的名稱接在後面（原物件鍵序）。
 * @param {string[]} names
 * @param {string[]} order
 * @returns {string[]}
 */
export function orderNamesByFieldOptions(names, order) {
  const nameSet = new Set(names)
  if (!order?.length) return [...names]
  const out = []
  const seen = new Set()
  for (const o of order) {
    if (nameSet.has(o) && !seen.has(o)) {
      out.push(o)
      seen.add(o)
    }
  }
  for (const n of names) {
    if (!seen.has(n)) {
      out.push(n)
      seen.add(n)
    }
  }
  return out
}
