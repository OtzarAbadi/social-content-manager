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
