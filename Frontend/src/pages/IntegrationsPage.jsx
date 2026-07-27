import { useEffect, useState } from 'react'
import { CalendarClock, CircleOff, Server } from 'lucide-react'
import PageShell from '../components/PageShell.jsx'
import Skeleton from '../components/Skeleton.jsx'
import { getPublishingStatus } from '../api/publishing.js'

function IntegrationsPage(props) {
  const [status, setStatus] = useState(null)
  const [state, setState] = useState('loading')

  useEffect(() => {
    const controller = new AbortController()
    getPublishingStatus({ signal: controller.signal })
      .then((data) => { setStatus(data); setState('ready') })
      .catch((error) => {
        if (error?.code !== 'ERR_CANCELED') setState(error?.response?.status === 403 ? 'forbidden' : 'error')
      })
    return () => controller.abort()
  }, [])

  return (
    <PageShell {...props}>
      <section className="integrations-page" dir="rtl" aria-labelledby="integrations-title">
        <div className="page-toolbar">
          <div>
            <p className="section-kicker">הגדרות פרסום</p>
            <h2 id="integrations-title">אינטגרציות</h2>
          </div>
        </div>

        {state === 'loading' && <Skeleton rows={3} className="page-skeleton" />}
        {state === 'forbidden' && <div className="state-card error-state" role="alert">אין לך הרשאה לצפות בעמוד זה.</div>}
        {state === 'error' && <div className="state-card error-state" role="alert">לא ניתן לטעון את מצב האינטגרציות.</div>}

        {state === 'ready' && status && (
          <>
            <div className="integration-grid">
              <article className="integration-card">
                <span className="integration-card-icon" aria-hidden="true"><Server size={24} /></span>
                <h3>מצב ספק הפרסום</h3>
                <strong>{status.activeProvider === 'LOCAL' ? 'ספק מקומי' : status.activeProvider}</strong>
              </article>
              <article className="integration-card">
                <span className="integration-card-icon" aria-hidden="true"><CircleOff size={24} /></span>
                <h3>חיבור חיצוני ל-Meta</h3>
                <strong>לא הוגדר</strong>
              </article>
              <article className="integration-card">
                <span className="integration-card-icon" aria-hidden="true"><CalendarClock size={24} /></span>
                <h3>פרסום אוטומטי</h3>
                <strong>{status.automaticPublishingEnabled ? 'פעיל' : 'לא פעיל'}</strong>
                <small>תדירות בדיקה: כל {status.pollingIntervalSeconds} שניות</small>
              </article>
            </div>
            <p className="integration-notice">
              הספק המקומי מעדכן את מצב התוכן במערכת אך אינו מפרסם לרשת חברתית חיצונית.
            </p>
          </>
        )}
      </section>
    </PageShell>
  )
}

export default IntegrationsPage
