import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageShell from '../components/PageShell.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import Skeleton from '../components/Skeleton.jsx'
import { getActivity } from '../api/activity.js'
import api from '../services/api.js'
import { ActivityIcon, getActivityDesign } from '../components/activityDesign.js'

const groupOrder = ['today', 'yesterday', 'previous']
const groupLabels = {
  today: 'היום',
  yesterday: 'אתמול',
  previous: 'תאריכים קודמים',
}

function errorMessage(error) {
  if (error?.response?.status === 401) return 'ההתחברות פגה. יש להתחבר מחדש כדי לצפות בפעילות.'
  if (error?.response?.status === 403) return 'אין לך הרשאה לצפות במרכז הפעילות.'
  return 'לא הצלחנו לטעון את הפעילות. אפשר לנסות שוב.'
}

function dateGroup(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'previous'
  const today = new Date()
  const startToday = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const startDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayDifference = Math.round((startToday - startDate) / 86400000)
  if (dayDifference === 0) return 'today'
  if (dayDifference === 1) return 'yesterday'
  return 'previous'
}

function formatActivityDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return { date: 'מועד לא זמין', time: '' }
  return {
    date: date.toLocaleDateString('he-IL'),
    time: date.toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' }),
  }
}

function ActivityPage({ activeRoute, routes, onNavigate, isAuthenticated, onLogout }) {
  const navigate = useNavigate()
  const [activities, setActivities] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [profile, setProfile] = useState(null)
  const [clients, setClients] = useState([])
  const [selectedClientId, setSelectedClientId] = useState('')

  const loadActivity = useCallback(async (signal) => {
    setLoading(true)
    setError('')
    try {
      setActivities(await getActivity({ signal }))
    } catch (requestError) {
      if (requestError?.code !== 'ERR_CANCELED') {
        setActivities([])
        setError(errorMessage(requestError))
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    Promise.resolve().then(() => loadActivity(controller.signal))
    return () => controller.abort()
  }, [loadActivity])

  useEffect(() => {
    let active = true
    api.get('/users/me').then(async ({ data }) => {
      if (!active) return
      setProfile(data)
      if (data?.role === 'ADMIN') {
        const response = await api.get('/clients')
        if (active) setClients(response.data)
      }
    }).catch(() => {})
    return () => { active = false }
  }, [])

  const visibleActivities = useMemo(() => selectedClientId
    ? activities.filter((activity) => Number(activity.clientId) === Number(selectedClientId))
    : activities, [activities, selectedClientId])

  const groups = useMemo(() => visibleActivities.reduce((result, activity) => {
    const group = dateGroup(activity.occurredAt)
    result[group].push(activity)
    return result
  }, { today: [], yesterday: [], previous: [] }), [visibleActivities])

  function openRelatedContent(activity) {
    if (!activity.contentId) return
    navigate(`/content/${activity.contentId}?highlightId=${activity.contentId}`)
  }

  function handleActivityKeyDown(event, activity) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openRelatedContent(activity)
    }
  }

  return <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate} isAuthenticated={isAuthenticated} onLogout={onLogout}>
    <section className="activity-page" dir="rtl" aria-labelledby="activity-page-title">
      <div className="activity-page-toolbar">
        <div>
          <p className="eyebrow">עדכונים אחרונים</p>
          <h2 id="activity-page-title">מרכז הפעילות</h2>
        </div>
        <button type="button" className="secondary-button" onClick={() => loadActivity()} disabled={loading}>
          {loading ? 'טוען...' : 'רענן'}
        </button>
      </div>
      <p className="activity-page-intro">כל השינויים האחרונים בתוכן, באישורים ובתכנון הפרסום.</p>
      {profile?.role === 'ADMIN' && <div className="activity-filters">
        <label>לקוח
          <select aria-label="סינון פעילות לפי לקוח" value={selectedClientId} onChange={(event) => setSelectedClientId(event.target.value)}>
            <option value="">כל הלקוחות</option>
            {clients.map((client) => <option key={client.client_id ?? client.clientId} value={client.client_id ?? client.clientId}>{client.business_name}</option>)}
          </select>
        </label>
      </div>}

      {loading && <Skeleton rows={4} className="page-skeleton" />}
      {!loading && error && <div className="activity-page-state activity-page-error" role="alert">
        <p>{error}</p>
        <button type="button" className="secondary-button" onClick={() => loadActivity()}>נסו שוב</button>
      </div>}
      {!loading && !error && visibleActivities.length === 0 && <div className="activity-page-state">
        <strong>{selectedClientId ? 'אין פעילות להצגה עבור הלקוח שנבחר' : 'עדיין אין פעילות להצגה'}</strong>
        {!selectedClientId && <p>שינויים בתוכן יופיעו כאן.</p>}
      </div>}

      {!loading && !error && visibleActivities.length > 0 && <div className="activity-groups">
        {groupOrder.map((groupKey) => groups[groupKey].length > 0 && <section className="activity-group" key={groupKey} aria-labelledby={`activity-group-${groupKey}`}>
          <h3 id={`activity-group-${groupKey}`}>{groupLabels[groupKey]}</h3>
          <div className="activity-timeline">
            {groups[groupKey].map((activity) => {
              const design = getActivityDesign(activity.type)
              const formatted = formatActivityDate(activity.occurredAt)
              const clickable = Boolean(activity.contentId)
              return <article className={`activity-item ${clickable ? 'activity-item-clickable' : ''}`} key={activity.activityId}
                role={clickable ? 'link' : undefined} tabIndex={clickable ? 0 : undefined}
                onClick={clickable ? () => openRelatedContent(activity) : undefined}
                onKeyDown={clickable ? (event) => handleActivityKeyDown(event, activity) : undefined}>
                <span className={`activity-icon activity-icon-${activity.type}`}><ActivityIcon type={activity.type} /></span>
                <div className="activity-item-body">
                  <div className="activity-item-heading">
                    <div>
                      <h4>{design.title}</h4>
                      <p className="activity-description">{design.description}</p>
                    </div>
                    {activity.status && <StatusBadge status={activity.status} />}
                  </div>
                  <strong className="activity-content-title">{activity.contentTitle || `תוכן #${activity.contentId}`}</strong>
                  <div className="activity-item-meta">
                    {activity.actorName && <span>{activity.actorName}</span>}
                    {activity.clientName && <span>{activity.clientName}</span>}
                    <span>גרסה {activity.versionNumber}</span>
                    <time dateTime={activity.occurredAt || undefined}>
                      <span>{formatted.date}</span>
                      {formatted.time && <span>{formatted.time}</span>}
                    </time>
                    {clickable && <button type="button" className="activity-open-content" onClick={(event) => { event.stopPropagation(); openRelatedContent(activity) }}>
                      פתח את התוכן <span aria-hidden="true">←</span>
                    </button>}
                  </div>
                </div>
              </article>
            })}
          </div>
        </section>)}
      </div>}
    </section>
  </PageShell>
}

export default ActivityPage
