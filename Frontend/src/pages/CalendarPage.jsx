import { useCallback, useEffect, useMemo, useState } from 'react'
import { Eye, MoveHorizontal, X } from 'lucide-react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import heLocale from '@fullcalendar/core/locales/he'
import PageShell from '../components/PageShell.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import statusDesign from '../components/statusDesign.js'
import api from '../services/api.js'
import MediaPreview from '../components/MediaPreview.jsx'

function idOf(content) { return content.content_id ?? content.contentId }
function clientIdOf(content) { return content.clientId ?? content.client_id }
function sortContentsNewest(items = []) {
  return [...items].sort((first, second) => {
    const firstTime = Date.parse(first.createdAt || first.created_at || '')
    const secondTime = Date.parse(second.createdAt || second.created_at || '')
    if (!Number.isFinite(firstTime)) return Number.isFinite(secondTime) ? 1 : Number(idOf(second) || 0) - Number(idOf(first) || 0)
    if (!Number.isFinite(secondTime)) return -1
    return secondTime - firstTime
  })
}
function toLocalDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function CalendarPage({ activeRoute, routes, onNavigate, isAuthenticated, onLogout }) {
  const [profile, setProfile] = useState(null)
  const [contents, setContents] = useState([])
  const [clients, setClients] = useState([])
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadCalendar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const meResponse = await api.get('/users/me')
      const isAdmin = meResponse.data.role === 'ADMIN'
      const [contentResponse, clientResponse] = await Promise.all([
        api.get('/contents'),
        isAdmin ? api.get('/clients') : Promise.resolve({ data: [] }),
      ])
      setProfile(meResponse.data)
      setContents(sortContentsNewest(contentResponse.data))
      setClients(clientResponse.data)
    } catch {
      setError('לא הצלחנו לטעון את לוח התוכן. נסו שוב.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    Promise.resolve().then(loadCalendar)
  }, [loadCalendar])

  const isAdmin = profile?.role === 'ADMIN'
  const clientNames = useMemo(() => new Map(clients.map((client) => [
    Number(client.client_id ?? client.clientId), client.business_name ?? client.businessName,
  ])), [clients])
  const scheduled = useMemo(() => contents.filter((content) => content.plannedPublishDate), [contents])
  const events = useMemo(() => scheduled.map((content) => {
    const meta = statusDesign[content.status] ?? statusDesign.DRAFT
    const businessName = clientNames.get(Number(clientIdOf(content)))
    return {
      id: String(idOf(content)),
      title: `${content.title} · ${meta.label}${isAdmin && businessName ? ` · ${businessName}` : ''}`,
      start: content.plannedPublishDate,
      backgroundColor: meta.color,
      borderColor: meta.color,
      extendedProps: { content },
    }
  }), [scheduled, clientNames, isAdmin])

  async function handleDrop(info) {
    const contentId = Number(info.event.id)
    try {
      const response = await api.put(`/contents/${contentId}/schedule`, {
        plannedPublishDate: toLocalDateTime(info.event.start),
      })
      setContents((current) => current.map((item) => idOf(item) === contentId ? response.data : item))
      setNotice('מועד הפרסום עודכן')
    } catch {
      info.revert()
      setError('עדכון מועד הפרסום נכשל. השינוי בוטל.')
    }
  }

  const plannedDate = selected?.plannedPublishDate ? new Date(selected.plannedPublishDate) : null
  const selectedClient = selected ? clientNames.get(Number(clientIdOf(selected))) : ''

  useEffect(() => {
    if (!selected) return
    const closeOnEscape = (event) => { if (event.key === 'Escape') setSelected(null) }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [selected])

  return (
    <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate} isAuthenticated={isAuthenticated} onLogout={onLogout}>
      <section className="calendar-card" aria-labelledby="calendar-title">
        <div className="calendar-heading">
          <div><p className="eyebrow">תכנון ופרסום</p><h2 id="calendar-title">לוח תוכן</h2></div>
          {profile && <span className="calendar-permission">{isAdmin ? <><MoveHorizontal size={18} /> גרירה פעילה למנהלים</> : <><Eye size={18} /> תצוגה לקריאה בלבד</>}</span>}
        </div>
        {notice && <p className="calendar-notice" role="status">{notice}</p>}
        {error && <div className="calendar-error" role="alert">{error} <button type="button" onClick={loadCalendar}>טעינה מחדש</button></div>}
        {loading ? <p className="entity-state">טוען את לוח התוכן...</p> : (
          <>
            {scheduled.length === 0 && <p className="calendar-empty">אין כרגע תכנים עם מועד פרסום מתוכנן.</p>}
            <FullCalendar
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              locales={[heLocale]}
              locale="he"
              direction="rtl"
              headerToolbar={{ start: 'prev,next today', center: 'title', end: 'dayGridMonth,timeGridWeek' }}
              buttonText={{ today: 'היום', month: 'חודש', week: 'שבוע' }}
              events={events}
              editable={isAdmin}
              eventStartEditable={isAdmin}
              eventDurationEditable={false}
              eventDrop={handleDrop}
              eventClick={(info) => setSelected(info.event.extendedProps.content)}
              height="auto"
              nowIndicator
            />
          </>
        )}
      </section>

      {selected && <div className="modal-backdrop" role="presentation" onMouseDown={() => setSelected(null)}>
        <section className="calendar-modal" role="dialog" aria-modal="true" aria-labelledby="event-title" onMouseDown={(event) => event.stopPropagation()}>
          <button className="modal-close" type="button" aria-label="סגירה" onClick={() => setSelected(null)}><X size={20} /></button>
          <StatusBadge status={selected.status} />
          <h2 id="event-title">{selected.title}</h2>
          <p className="calendar-description">{selected.description || 'לא נוסף תיאור.'}</p>
          <dl className="event-details">
            <div><dt>תאריך פרסום</dt><dd>{plannedDate?.toLocaleDateString('he-IL')}</dd></div>
            <div><dt>שעת פרסום</dt><dd>{plannedDate?.toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' })}</dd></div>
            {isAdmin && <div><dt>לקוח</dt><dd>{selectedClient || 'לקוח לא זמין'}</dd></div>}
          </dl>
          {selected.file_url
            ? <MediaPreview path={selected.file_url} type={selected.content_type} alt={`מדיה עבור ${selected.title}`} className="calendar-media-preview" />
            : <p className="media-missing">אין מדיה מצורפת לתוכן זה.</p>}
          <button className="primary-button" type="button" onClick={() => onNavigate('dashboard')}>פתיחת עמוד התוכן</button>
        </section>
      </div>}
    </PageShell>
  )
}

export default CalendarPage
