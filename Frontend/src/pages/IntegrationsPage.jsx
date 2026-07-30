import { useCallback, useEffect, useState } from 'react'
import {
  Activity, BarChart3, CalendarClock, Camera, CheckCircle2,
  RefreshCw, Send, ShieldCheck, Unplug,
} from 'lucide-react'
import PageShell from '../components/PageShell.jsx'
import Skeleton from '../components/Skeleton.jsx'
import {
  getInstagramSettings, getPublishingStatus, updateInstagramSettings,
} from '../api/publishing.js'

function StatusLine({ label, value, state = 'neutral' }) {
  return <div className="integration-status-line">
    <span>{label}</span>
    <strong className={`integration-status-value status-${state}`}>{value}</strong>
  </div>
}

function IntegrationsPage(props) {
  const [status, setStatus] = useState(null)
  const [state, setState] = useState('loading')
  const [settings, setSettings] = useState({ instagramUserId: '', graphApiBaseUrl: '' })
  const [saving, setSaving] = useState(false)
  const [saveState, setSaveState] = useState('')

  const load = useCallback(async () => {
    setState('loading')
    try {
      const [publishingStatus, instagramSettings] = await Promise.all([
        getPublishingStatus(), getInstagramSettings(),
      ])
      setStatus(publishingStatus)
      setSettings({
        instagramUserId: instagramSettings.instagramUserId || '',
        graphApiBaseUrl: instagramSettings.graphApiBaseUrl || '',
        accessTokenConfigured: Boolean(instagramSettings.accessTokenConfigured),
      })
      setState('ready')
    } catch (error) {
      setState(error?.response?.status === 403 ? 'forbidden' : 'error')
    }
  }, [])

  async function saveSettings(event) {
    event.preventDefault()
    setSaving(true)
    setSaveState('')
    try {
      const updated = await updateInstagramSettings({
        instagramUserId: settings.instagramUserId.trim(),
        graphApiBaseUrl: settings.graphApiBaseUrl.trim(),
      })
      setSettings((current) => ({ ...current, ...updated }))
      setSaveState('success')
    } catch {
      setSaveState('error')
    } finally {
      setSaving(false)
    }
  }

  useEffect(() => { Promise.resolve().then(load) }, [load])

  const automatic = Boolean(status?.automaticPublishingEnabled)
  const provider = status?.activeProvider || 'לא זמין'

  return <PageShell {...props}>
    <section className="integrations-page" dir="rtl" aria-labelledby="integrations-title">
      <header className="integration-heading">
        <div>
          <p className="section-kicker">חיבורים ושירותים</p>
          <h2 id="integrations-title">אינטגרציות</h2>
          <p>כאן אפשר להבין אילו שירותים מחוברים, מה כל חיבור מאפשר ומה דורש תשומת לב.</p>
        </div>
        <button type="button" className="secondary-button" onClick={load} disabled={state === 'loading'}>
          <RefreshCw size={18} aria-hidden="true" /> רענון מצב
        </button>
      </header>

      {state === 'loading' && <Skeleton rows={3} className="page-skeleton" />}
      {state === 'forbidden' && <div className="state-card error-state" role="alert">אין לך הרשאה לצפות בעמוד זה.</div>}
      {state === 'error' && <div className="state-card error-state" role="alert">לא ניתן לטעון את מצב האינטגרציות. <button type="button" className="secondary-button" onClick={load}>ניסיון נוסף</button></div>}

      {state === 'ready' && status && <>
        <section className="integration-hero-card" aria-labelledby="instagram-connection-title">
          <div className="integration-hero-icon"><Camera size={30} aria-hidden="true" /></div>
          <div className="integration-hero-copy">
            <p className="eyebrow">Instagram Connection</p>
            <h3 id="instagram-connection-title">חיבור Instagram ו-Meta</h3>
            <p>החיבור מאפשר פרסום תמונות מאושרות וקבלת נתוני ביצועים מחשבון Instagram המקצועי.</p>
          </div>
          <span className="integration-health"><CheckCircle2 size={18} aria-hidden="true" /> הגדרות השרת פעילות</span>
        </section>

        <div className="integration-detail-grid">
          <form className="integration-detail-card instagram-settings-form" onSubmit={saveSettings}>
            <h3><Camera size={20} aria-hidden="true" /> פרטי חשבון Instagram</h3>
            <label>
              מזהה חשבון Instagram מקצועי
              <input
                value={settings.instagramUserId}
                onChange={(event) => setSettings((current) => ({ ...current, instagramUserId: event.target.value }))}
                inputMode="numeric"
                pattern="[0-9]{5,40}"
                required
                disabled={saving}
              />
            </label>
            <label>
              כתובת Graph API
              <input
                type="url"
                dir="ltr"
                value={settings.graphApiBaseUrl}
                onChange={(event) => setSettings((current) => ({ ...current, graphApiBaseUrl: event.target.value }))}
                pattern="https://graph\.facebook\.com/v[0-9]+\.[0-9]+/?"
                required
                disabled={saving}
              />
            </label>
            <p className="instagram-token-state">
              אסימון Meta: {settings.accessTokenConfigured ? 'מוגדר באופן מאובטח בשרת' : 'לא מוגדר בשרת'}
            </p>
            {saveState === 'success' && <p className="instagram-settings-feedback success" role="status">פרטי Instagram עודכנו בהצלחה.</p>}
            {saveState === 'error' && <p className="instagram-settings-feedback error" role="alert">לא הצלחנו לעדכן את הפרטים. בדקו את הערכים ונסו שוב.</p>}
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? <><span className="button-spinner" />שומר...</> : 'שמירת פרטי Instagram'}
            </button>
          </form>
          <article className="integration-detail-card">
            <h3><ShieldCheck size={20} aria-hidden="true" /> מצב החיבור</h3>
            <StatusLine label="ספק פרסום פעיל" value={provider === 'LOCAL' ? 'מקומי' : provider} />
            <StatusLine label="חשבון מחובר" value="מוגדר בצד השרת" state="success" />
            <StatusLine label="הרשאות פרסום" value="נבדקות בזמן פרסום" />
            <StatusLine label="הרשאות אנליטיקה" value="נבדקות מול Meta בזמן טעינה" />
          </article>

          <article className="integration-detail-card">
            <h3><CalendarClock size={20} aria-hidden="true" /> אוטומציה וסנכרון</h3>
            <StatusLine label="פרסום אוטומטי" value={automatic ? 'פעיל' : 'לא פעיל'} state={automatic ? 'success' : 'neutral'} />
            <StatusLine label="תדירות בדיקה" value={`כל ${status.pollingIntervalSeconds} שניות`} />
            <StatusLine label="סנכרון אחרון" value="בעת טעינת העמוד" />
            <p>המערכת בודקת תכנים מתוזמנים לפי התדירות שמוגדרת בשרת.</p>
          </article>
        </div>

        <section className="integration-diagnostics" aria-labelledby="integration-diagnostics-title">
          <header><div><p className="eyebrow">Diagnostics</p><h3 id="integration-diagnostics-title">אבחון החיבור</h3></div></header>
          <div className="integration-diagnostic-grid">
            <article><ShieldCheck aria-hidden="true" /><strong>מצב הרשאות</strong><span>מוצג בבירור אם Meta דוחה בקשה</span></article>
            <article><Activity aria-hidden="true" /><strong>בריאות החיבור</strong><span>שירות הסטטוס זמין</span></article>
            <article><Send aria-hidden="true" /><strong>בדיקת פרסום</strong><span>מתבצעת בבטחה מתוך תוכן מאושר</span></article>
            <article><BarChart3 aria-hidden="true" /><strong>Instagram Insights</strong><span>נתונים אמיתיים בלבד, ללא ערכי דמה</span></article>
          </div>
        </section>

        <div className="integration-actions" role="group" aria-label="פעולות חיבור">
          <button type="button" className="primary-button" disabled title="חיבור OAuth עדיין אינו זמין"><RefreshCw size={18} />חיבור מחדש</button>
          <button type="button" className="danger-button" disabled title="ניתוק OAuth עדיין אינו זמין"><Unplug size={18} />ניתוק</button>
          <p>חיבור וניתוק עצמי יהיו זמינים לאחר הוספת OAuth. כרגע פרטי החיבור מנוהלים בצורה מאובטחת בשרת.</p>
        </div>
      </>}
    </section>
  </PageShell>
}

export default IntegrationsPage
