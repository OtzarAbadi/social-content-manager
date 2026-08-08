export const unavailableAnalyticsValue = 'לא זמין'

export function showAnalyticsValue(value, percent = false) {
  if (value === null || value === undefined) return unavailableAnalyticsValue
  return percent ? `${Number(value).toLocaleString('he-IL', { maximumFractionDigits: 2 })}%`
    : Number(value).toLocaleString('he-IL')
}

export function formatAnalyticsChartDate(value, long = false) {
  if (!value) return unavailableAnalyticsValue
  const date = new Date(`${String(value).slice(0, 10)}T12:00:00`)
  if (Number.isNaN(date.getTime())) return String(value)
  return long
    ? date.toLocaleDateString('he-IL', { day: '2-digit', month: 'long' })
    : date.toLocaleDateString('he-IL', { day: '2-digit', month: '2-digit' })
}

export function mergeAnalyticsTrends(accountTrend = [], mediaTrend = []) {
  const byDate = new Map()
  const merge = (row, fallbackOnly) => {
    if (!row?.date) return
    const date = String(row.date).slice(0, 10)
    const current = byDate.get(date) || { date }
    for (const [key, value] of Object.entries(row)) {
      if (key === 'date' || value === null || value === undefined) continue
      if (!fallbackOnly || current[key] === null || current[key] === undefined) current[key] = value
    }
    byDate.set(date, current)
  }
  accountTrend.forEach((row) => merge(row, false))
  mediaTrend.forEach((row) => merge(row, true))
  return [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date))
}
