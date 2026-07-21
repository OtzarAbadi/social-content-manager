import { useCallback, useEffect, useMemo, useState } from 'react'
import PageShell from '../components/PageShell.jsx'
import { getAnalyticsDashboard, getAnalyticsProfile } from '../api/analytics.js'

const statusLabels = { DRAFT: 'טיוטה', WAITING_APPROVAL: 'ממתין לאישור', APPROVED: 'מאושר', REJECTED: 'נדחה', PUBLISHED: 'פורסם' }
const typeLabels = { IMAGE: 'תמונה', VIDEO: 'וידאו', TEXT: 'טקסט' }

function errorMessage(error) {
  if (error?.response?.status === 401) return 'ההתחברות פגה. יש להתחבר מחדש כדי לצפות באנליטיקה.'
  if (error?.response?.status === 403) return 'אין לך הרשאה לצפות בנתוני האנליטיקה.'
  return 'לא הצלחנו לטעון את נתוני האנליטיקה. אפשר לנסות שוב.'
}

function formatGeneratedAt(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('he-IL')
}

function DistributionBars({ values, labels, className = '' }) {
  const entries = Object.entries(values || {})
  const maximum = Math.max(0, ...entries.map(([, count]) => Number(count) || 0))
  return <div className={`analytics-bars ${className}`}>
    {entries.map(([key, count]) => {
      const numericCount = Number(count) || 0
      const width = maximum === 0 ? 0 : (numericCount / maximum) * 100
      return <div className="analytics-bar-row" key={key}>
        <div className="analytics-bar-label"><span>{labels[key] || key}</span><strong>{numericCount}</strong></div>
        <div className="analytics-bar-track" aria-label={`${labels[key] || key}: ${numericCount}`}><span style={{ width: `${width}%` }} /></div>
      </div>
    })}
  </div>
}

function AnalyticsPage({ activeRoute, routes, onNavigate, isAuthenticated, onLogout }) {
  const [analytics, setAnalytics] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadAnalytics = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [dashboard, currentProfile] = await Promise.all([getAnalyticsDashboard(), getAnalyticsProfile()])
      setAnalytics(dashboard)
      setProfile(currentProfile)
    } catch (requestError) {
      setAnalytics(null)
      setError(errorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadAnalytics() }, [loadAnalytics])

  const monthlyMaximum = useMemo(() => Math.max(0, ...(analytics?.scheduledByMonth || []).map((item) => Number(item.count) || 0)), [analytics])
  const isAdmin = profile?.role === 'ADMIN'
  const isClient = profile?.role === 'CLIENT'

  return <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate} isAuthenticated={isAuthenticated} onLogout={onLogout}>
    <section className="analytics-page" dir="rtl">
      <header className="analytics-heading">
        <div><p className="eyebrow">תמונת מצב פנימית</p><h2>{isClient ? 'הפעילות של העסק שלך' : 'אנליטיקת תוכן'}</h2><p>נתוני SSCM שוטפים על תוכן, תכנון ותקשורת.</p></div>
        {analytics?.generatedAt && <time>עודכן: {formatGeneratedAt(analytics.generatedAt)}</time>}
      </header>

      {loading && <div className="analytics-state"><span className="analytics-loader" />טוען נתוני אנליטיקה...</div>}
      {!loading && error && <div className="analytics-state analytics-error" role="alert"><p>{error}</p><button type="button" className="secondary-button" onClick={loadAnalytics}>ניסיון נוסף</button></div>}

      {!loading && !error && analytics && (isAdmin || isClient) && <>
        {analytics.totalContents === 0 && <div className="analytics-empty">עדיין אין תוכן להצגה. המדדים יתעדכנו לאחר יצירת תוכן.</div>}
        <section className="analytics-summary" aria-label="מדדי סיכום">
          <article><span>סך הכול תוכן</span><strong>{analytics.totalContents}</strong></article>
          <article><span>מתוזמנים</span><strong>{analytics.scheduledContents}</strong></article>
          <article><span>ממתינים לאישור</span><strong>{analytics.waitingApprovalContents}</strong></article>
          <article><span>פורסמו</span><strong>{analytics.publishedContents}</strong></article>
          <article><span>תגובות</span><strong>{analytics.totalComments}</strong><small>ממוצע {Number(analytics.averageCommentsPerContent || 0).toFixed(1)} לתוכן</small></article>
        </section>

        <div className="analytics-grid">
          <section className="analytics-panel">
            <div className="analytics-panel-title"><div><p className="eyebrow">סטטוסים</p><h3>התפלגות מצב התוכן</h3></div><span>{analytics.totalContents} פריטים</span></div>
            <DistributionBars values={analytics.contentsByStatus} labels={statusLabels} className="status-bars" />
          </section>
          <section className="analytics-panel">
            <div className="analytics-panel-title"><div><p className="eyebrow">פורמטים</p><h3>סוגי תוכן</h3></div></div>
            <DistributionBars values={analytics.contentsByType} labels={typeLabels} className="type-bars" />
          </section>
          <section className="analytics-panel analytics-month-panel">
            <div className="analytics-panel-title"><div><p className="eyebrow">תכנון</p><h3>תוכן מתוזמן לפי חודש</h3></div></div>
            {analytics.scheduledByMonth.length === 0 ? <p className="analytics-panel-empty">אין עדיין תוכן עם מועד פרסום מתוכנן.</p> :
              <div className="analytics-month-chart" aria-label="תוכן מתוזמן לפי חודש">
                {analytics.scheduledByMonth.map((item) => {
                  const height = monthlyMaximum === 0 ? 0 : (Number(item.count) / monthlyMaximum) * 100
                  return <div className="analytics-month-column" key={item.month}><strong>{item.count}</strong><div><span style={{ height: `${height}%` }} /></div><time>{item.month}</time></div>
                })}
              </div>}
          </section>

          {isAdmin && <section className="analytics-panel analytics-client-panel">
            <div className="analytics-panel-title"><div><p className="eyebrow">לקוחות</p><h3>סיכום פעילות לפי לקוח</h3></div></div>
            {analytics.clientSummaries.length === 0 ? <p className="analytics-panel-empty">אין לקוחות להצגה.</p> :
              <div className="analytics-table-wrap"><table className="analytics-table">
                <thead><tr><th>לקוח</th><th>תוכן</th><th>מתוזמן</th><th>ממתין</th><th>פורסם</th><th>תגובות</th></tr></thead>
                <tbody>{analytics.clientSummaries.map((client) => <tr key={client.clientId}>
                  <th scope="row"><span>{client.businessName}</span><small>#{client.clientId}</small></th><td>{client.totalContents}</td><td>{client.scheduledContents}</td><td>{client.waitingApprovalContents}</td><td>{client.publishedContents}</td><td>{client.commentCount}</td>
                </tr>)}</tbody>
              </table></div>}
          </section>}
        </div>
      </>}
    </section>
  </PageShell>
}

export default AnalyticsPage
