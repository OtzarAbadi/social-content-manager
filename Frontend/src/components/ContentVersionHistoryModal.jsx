import { useEffect, useState } from 'react'
import axios from 'axios'
import { getContentVersions } from '../api/contentVersions.js'
import StatusBadge from './StatusBadge.jsx'

const API_BASE_URL = 'http://localhost:8081'

const changeTypeLabels = {
  CREATED: 'יצירה',
  EDITED: 'עריכת תוכן',
  SCHEDULED: 'שינוי מועד פרסום',
  STATUS_CHANGED: 'שינוי סטטוס',
}

const contentTypeLabels = {
  IMAGE: 'תמונה',
  VIDEO: 'וידאו',
  TEXT: 'טקסט',
}

function formatDateTime(value) {
  if (!value) return 'לא צוין'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('he-IL')
}

function resolveFileUrl(fileUrl) {
  if (!fileUrl) return ''
  return fileUrl.startsWith('http') ? fileUrl : `${API_BASE_URL}${fileUrl}`
}

function getMediaKind(version) {
  const path = (version.fileUrl || '').split('?')[0].toLowerCase()
  if (/\.(jpg|jpeg|png|gif|webp|bmp)$/.test(path) || version.contentType === 'IMAGE') return 'image'
  if (/\.(mp4|webm|mov|avi|mkv)$/.test(path) || version.contentType === 'VIDEO') return 'video'
  return 'file'
}

function getErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש כדי לצפות בהיסטוריה.'
  if (status === 403) return 'אין לך הרשאה לצפות בהיסטוריה של תוכן זה.'
  if (status === 404) return 'התוכן המבוקש לא נמצא.'
  return 'לא הצלחנו לטעון את היסטוריית הגרסאות. אפשר לנסות שוב.'
}

function ContentVersionHistoryModal({ content, onClose }) {
  const contentId = content.content_id ?? content.contentId
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    setVersions([])
    setError('')
    setLoading(true)

    getContentVersions(contentId, controller.signal)
      .then((history) => setVersions(Array.isArray(history) ? history : []))
      .catch((requestError) => {
        if (!axios.isCancel(requestError)) setError(getErrorMessage(requestError))
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [contentId])

  useEffect(() => {
    function closeOnEscape(event) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal-card version-history-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="version-history-title"
        dir="rtl"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button type="button" className="modal-close" onClick={onClose} aria-label="סגירת היסטוריית גרסאות">×</button>
        <header className="version-history-header">
          <p>תוכן #{contentId}</p>
          <h2 id="version-history-title">היסטוריית גרסאות</h2>
          <span>{content.title}</span>
        </header>

        <div className="version-history-body" aria-live="polite">
          {loading && <div className="version-history-state"><span className="history-loader" />טוען היסטוריית גרסאות...</div>}
          {!loading && error && <div className="version-history-state version-history-error" role="alert">{error}</div>}
          {!loading && !error && versions.length === 0 && (
            <div className="version-history-state">עדיין אין גרסאות שמורות לתוכן זה.</div>
          )}
          {!loading && !error && versions.length > 0 && (
            <ol className="version-history-list">
              {versions.map((version) => {
                const mediaUrl = resolveFileUrl(version.fileUrl)
                const mediaKind = getMediaKind(version)
                return (
                  <li className="version-history-item" key={version.contentVersionId ?? version.versionNumber}>
                    <div className="version-history-summary">
                      <div>
                        <strong>גרסה {version.versionNumber}</strong>
                        <span>{changeTypeLabels[version.changeType] || version.changeType}</span>
                      </div>
                      <StatusBadge status={version.status} />
                    </div>
                    <div className="version-history-meta">
                      <span>עודכן: {formatDateTime(version.changedAt)}</span>
                      <span>משתמש #{version.changedByUserId ?? 'לא ידוע'}</span>
                    </div>
                    <dl className="version-history-fields">
                      <div><dt>כותרת</dt><dd>{version.title || 'ללא כותרת'}</dd></div>
                      <div><dt>תיאור</dt><dd>{version.description || 'אין תיאור'}</dd></div>
                      <div><dt>סוג תוכן</dt><dd>{contentTypeLabels[version.contentType] || version.contentType || 'לא צוין'}</dd></div>
                      <div><dt>מועד פרסום</dt><dd>{formatDateTime(version.plannedPublishDate)}</dd></div>
                    </dl>
                    {mediaUrl && mediaKind === 'image' && (
                      <a className="version-history-media" href={mediaUrl} target="_blank" rel="noreferrer">
                        <img src={mediaUrl} alt={`מדיה מגרסה ${version.versionNumber}`} />
                      </a>
                    )}
                    {mediaUrl && mediaKind === 'video' && (
                      <video className="version-history-media" src={mediaUrl} controls preload="metadata">
                        <a href={mediaUrl}>פתיחת הווידאו</a>
                      </video>
                    )}
                    {mediaUrl && mediaKind === 'file' && (
                      <a className="file-link" href={mediaUrl} target="_blank" rel="noreferrer">פתיחת קובץ המדיה</a>
                    )}
                  </li>
                )
              })}
            </ol>
          )}
        </div>
      </section>
    </div>
  )
}

export default ContentVersionHistoryModal
