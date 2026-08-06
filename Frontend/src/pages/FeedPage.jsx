import { useCallback, useEffect, useState } from 'react'
import { Film, Layers, RefreshCw } from 'lucide-react'
import PageShell from '../components/PageShell.jsx'
import CreationModal from '../components/CreationModal.jsx'
import ContentMediaCarousel from '../components/ContentMediaCarousel.jsx'
import api from '../services/api.js'
import { getImageUrl, getMediaType } from '../utils/imageUrl.js'

const contentId = (content) => content.content_id ?? content.contentId
const mediaItems = (content) => {
  const items = content.media?.length
    ? [...content.media]
    : content.file_url ? [{ mediaUrl: content.file_url, mediaType: content.content_type, displayOrder: 0 }] : []
  return items.sort((a, b) => (a.displayOrder ?? a.display_order ?? 0) - (b.displayOrder ?? b.display_order ?? 0))
}
const newestFirst = (first, second) => {
  const firstTime = Date.parse(first.createdAt || first.created_at || '') || 0
  const secondTime = Date.parse(second.createdAt || second.created_at || '') || 0
  return secondTime - firstTime || contentId(second) - contentId(first)
}

export function PendingFeed({ profile, reloadKey = 0 }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedId, setSelectedId] = useState(null)
  const [comments, setComments] = useState([])
  const [reason, setReason] = useState('')
  const [rejectOpen, setRejectOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const selected = items.find((content) => contentId(content) === selectedId) || null

  const load = useCallback(async () => {
    setSelectedId(null)
    setRejectOpen(false)
    setReason('')
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/contents/status/WAITING_APPROVAL', { suppressGlobalErrorToast: true })
      setItems(response.data.filter((content) => content.status === 'WAITING_APPROVAL' && contentId(content) != null).sort(newestFirst))
    } catch {
      setError('לא הצלחנו לטעון את התכנים שממתינים לאישור.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { Promise.resolve().then(load) }, [load, reloadKey])

  async function openDetails(clickedId) {
    const clicked = items.find((content) => contentId(content) === clickedId)
    if (!clicked || contentId(clicked) !== clickedId || mediaItems(clicked).length === 0) return
    setSelectedId(clickedId)
    setComments([])
    try {
      const response = await api.get('/comments/by-content', { params: { contentId: clickedId }, suppressGlobalErrorToast: true })
      setComments(response.data)
    } catch { setComments([]) }
  }

  function removeSelected() {
    const id = selectedId
    setItems((current) => current.filter((content) => contentId(content) !== id))
    setSelectedId(null)
    setRejectOpen(false)
    setReason('')
  }

  async function approve() {
    if (!selected) return
    setSaving(true)
    try {
      await api.put(`/contents/${contentId(selected)}/approve`)
      removeSelected()
    } finally { setSaving(false) }
  }

  async function reject(event) {
    event.preventDefault()
    if (!selected || !reason.trim()) return
    setSaving(true)
    try {
      await api.put(`/contents/${contentId(selected)}/reject`, { reason: reason.trim() })
      setRejectOpen(false)
      setReason('')
      removeSelected()
    } finally { setSaving(false) }
  }

  if (loading) return <div className="feed-grid feed-skeleton" aria-label="טוען פיד" role="status">{Array.from({ length: 6 }, (_, index) => <span key={index} />)}</div>
  if (error) return <div className="feed-state" role="alert"><p>{error}</p><button type="button" className="secondary-button" onClick={load}><RefreshCw size={17} /> ניסיון חוזר</button></div>
  if (!items.length) return <div className="feed-state"><h2>אין כרגע תכנים שממתינים לאישור</h2><p>תכנים חדשים שיועברו לאישור יופיעו כאן.</p></div>

  return <>
    <section className="feed-grid" aria-label="תכנים שממתינים לאישור">
      {items.map((content) => {
        const media = mediaItems(content)
        const first = media[0]
        const type = getMediaType(first?.mediaUrl || first?.media_url, first?.mediaType || first?.media_type)
        if (!first) return <div className="feed-tile feed-tile-empty" key={contentId(content)} role="img" aria-label={`${content.title}: ללא מדיה`}><span className="feed-no-media">ללא מדיה</span></div>
        return <button type="button" className="feed-tile" key={contentId(content)} onClick={() => openDetails(contentId(content))} aria-label={`פתיחת ${content.title}`}>
          {type === 'video'
            ? <video src={getImageUrl(first?.mediaUrl || first?.media_url)} muted playsInline preload="metadata" />
            : <img src={getImageUrl(first.mediaUrl || first.media_url)} alt={content.title} loading="lazy" />}
          <span className="feed-overlay-icons">
            {media.length > 1 && <Layers aria-label="מספר פריטי מדיה" />}
            {type === 'video' && <Film aria-label="וידאו" />}
          </span>
        </button>
      })}
    </section>

    <CreationModal key={selectedId ?? 'closed'} open={Boolean(selected)} titleId="feed-detail-title" closeLabel="סגירת פרטי התוכן" onClose={() => setSelectedId(null)}>
      {selected && <article className="feed-detail">
        <div className="feed-detail-media"><ContentMediaCarousel media={mediaItems(selected)} fallbackUrl={selected.file_url} fallbackType={selected.content_type} alt={selected.title} /></div>
        <div className="feed-detail-copy">
          <p className="eyebrow">ממתין לאישור</p>
          <h2 id="feed-detail-title">{selected.title}</h2>
          <p className="feed-caption">{selected.description || 'אין תיאור'}</p>
          {selected.plannedPublishDate && <p><strong>מועד פרסום מתוכנן:</strong> {new Date(selected.plannedPublishDate).toLocaleString('he-IL')}</p>}
          {comments.length > 0 && <section className="feed-comments" aria-label="תגובות קיימות"><h3>תגובות</h3>{comments.map((comment) => <p key={comment.commentId ?? comment.comment_id}>{comment.commentText}</p>)}</section>}
          {profile?.role === 'CLIENT' && <div className="feed-approval-actions">
            <button type="button" className="primary-button" onClick={approve} disabled={saving}>אישור</button>
            <button type="button" className="danger-button" onClick={() => setRejectOpen(true)} disabled={saving}>דחייה</button>
          </div>}
        </div>
      </article>}
    </CreationModal>

    <CreationModal open={rejectOpen} titleId="feed-reject-title" closeLabel="סגירת דחייה" onClose={() => setRejectOpen(false)}>
      <form className="feed-reject-form" onSubmit={reject}>
        <h2 id="feed-reject-title">דחיית תוכן</h2>
        <label>סיבת הדחייה<textarea value={reason} onChange={(event) => setReason(event.target.value)} required /></label>
        <button className="danger-button" disabled={saving || !reason.trim()}>דחיית התוכן</button>
      </form>
    </CreationModal>
  </>
}

function FeedPage(props) {
  const [profile, setProfile] = useState(null)
  useEffect(() => { api.get('/users/me').then((response) => setProfile(response.data)).catch(() => setProfile(null)) }, [])
  return <PageShell {...props}><div className="feed-page"><header className="feed-header"><p className="eyebrow">תכנים לאישור</p><h2>פיד</h2></header><PendingFeed profile={profile} /></div></PageShell>
}

export default FeedPage
