import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { getContentVersions, restoreContentVersion } from '../api/contentVersions.js'
import StatusBadge from './StatusBadge.jsx'

const API_BASE_URL = 'http://localhost:8081'
const RESTORABLE_STATUSES = new Set(['DRAFT', 'REJECTED'])

const changeTypeLabels = {
  CREATED: 'יצירה',
  EDITED: 'עריכת תוכן',
  SCHEDULED: 'שינוי מועד פרסום',
  STATUS_CHANGED: 'שינוי סטטוס',
}

const contentTypeLabels = { IMAGE: 'תמונה', VIDEO: 'וידאו', TEXT: 'טקסט' }

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

function getHistoryErrorMessage(error) {
  const status = error?.response?.status
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש כדי לצפות בהיסטוריה.'
  if (status === 403) return 'אין לך הרשאה לצפות בהיסטוריה של תוכן זה.'
  if (status === 404) return 'התוכן המבוקש לא נמצא.'
  return 'לא הצלחנו לטעון את היסטוריית הגרסאות. אפשר לנסות שוב.'
}

function getRestoreErrorMessage(error) {
  const status = error?.response?.status
  const backendMessage = error?.response?.data?.message
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש לפני השחזור.'
  if (status === 403) return 'רק מנהל מערכת רשאי לשחזר גרסה.'
  if (status === 404) return 'התוכן או הגרסה המבוקשת אינם קיימים.'
  if (status === 400 && backendMessage === 'Historical media file is unavailable') {
    return 'לא ניתן לשחזר: קובץ המדיה של גרסה זו אינו זמין.'
  }
  if (status === 400) return 'לא ניתן לשחזר גרסה במצב הנוכחי של התוכן.'
  return 'השחזור נכשל. לא בוצעו שינויים ואפשר לנסות שוב.'
}

function ContentVersionHistoryModal({ content, role, onClose, onRestored }) {
  const contentId = content.content_id ?? content.contentId
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [restoringVersion, setRestoringVersion] = useState(null)
  const [restoreError, setRestoreError] = useState('')
  const [restoreSuccess, setRestoreSuccess] = useState('')
  const isAdmin = role === 'ADMIN'
  const canRestore = isAdmin && RESTORABLE_STATUSES.has(content.status)

  const loadHistory = useCallback(async (signal) => {
    setError('')
    setLoading(true)
    try {
      const history = await getContentVersions(contentId, signal)
      setVersions(Array.isArray(history) ? history : [])
    } catch (requestError) {
      if (!axios.isCancel(requestError)) setError(getHistoryErrorMessage(requestError))
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [contentId])

  useEffect(() => {
    const controller = new AbortController()
    setVersions([])
    setRestoreError('')
    setRestoreSuccess('')
    loadHistory(controller.signal)
    return () => controller.abort()
  }, [loadHistory])

  useEffect(() => {
    function closeOnEscape(event) {
      if (event.key === 'Escape' && restoringVersion === null) onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose, restoringVersion])

  async function handleRestore(versionNumber) {
    const confirmed = window.confirm(`לשחזר את גרסה ${versionNumber}?\nהסטטוס ומועד הפרסום הנוכחיים יישמרו.`)
    if (!confirmed) return

    setRestoringVersion(versionNumber)
    setRestoreError('')
    setRestoreSuccess('')
    try {
      const result = await restoreContentVersion(contentId, versionNumber)
      if (onRestored) await onRestored(result)
      await loadHistory()
      setRestoreSuccess(result.changed
        ? `גרסה ${versionNumber} שוחזרה ונשמרה כגרסה ${result.newVersionNumber}.`
        : `התוכן כבר תואם לגרסה ${versionNumber}; לא נוצרה גרסה חדשה.`)
    } catch (requestError) {
      setRestoreError(getRestoreErrorMessage(requestError))
    } finally {
      setRestoringVersion(null)
    }
  }

  const closeIfIdle = () => {
    if (restoringVersion === null) onClose()
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={closeIfIdle}>
      <section className="modal-card version-history-modal" role="dialog" aria-modal="true" aria-labelledby="version-history-title" dir="rtl" onMouseDown={(event) => event.stopPropagation()}>
        <button type="button" className="modal-close" onClick={closeIfIdle} disabled={restoringVersion !== null} aria-label="סגירת היסטוריית גרסאות">×</button>
        <header className="version-history-header">
          <p>תוכן #{contentId}</p>
          <h2 id="version-history-title">היסטוריית גרסאות</h2>
          <span>{content.title}</span>
        </header>

        <div className="version-history-body" aria-live="polite">
          {isAdmin && !canRestore && (
            <p className="version-restore-note">שחזור זמין רק כאשר התוכן במצב טיוטה או נדחה, כדי לא לעקוף תהליכי אישור ופרסום.</p>
          )}
          {restoreError && <p className="version-restore-feedback version-restore-error" role="alert">{restoreError}</p>}
          {restoreSuccess && <p className="version-restore-feedback version-restore-success">{restoreSuccess}</p>}
          {loading && <div className="version-history-state"><span className="history-loader" />טוען היסטוריית גרסאות...</div>}
          {!loading && error && <div className="version-history-state version-history-error" role="alert">{error}</div>}
          {!loading && !error && versions.length === 0 && <div className="version-history-state">עדיין אין גרסאות שמורות לתוכן זה.</div>}
          {!loading && !error && versions.length > 0 && (
            <ol className="version-history-list">
              {versions.map((version) => {
                const mediaUrl = resolveFileUrl(version.fileUrl)
                const mediaKind = getMediaKind(version)
                return (
                  <li className="version-history-item" key={version.contentVersionId ?? version.versionNumber}>
                    <div className="version-history-summary">
                      <div><strong>גרסה {version.versionNumber}</strong><span>{changeTypeLabels[version.changeType] || version.changeType}</span></div>
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
                    {mediaUrl && mediaKind === 'image' && <a className="version-history-media" href={mediaUrl} target="_blank" rel="noreferrer"><img src={mediaUrl} alt={`מדיה מגרסה ${version.versionNumber}`} /></a>}
                    {mediaUrl && mediaKind === 'video' && <video className="version-history-media" src={mediaUrl} controls preload="metadata"><a href={mediaUrl}>פתיחת הווידאו</a></video>}
                    {mediaUrl && mediaKind === 'file' && <a className="file-link" href={mediaUrl} target="_blank" rel="noreferrer">פתיחת קובץ המדיה</a>}
                    {canRestore && (
                      <div className="version-restore-actions">
                        <button type="button" className="ghost-button small-button version-restore-button" disabled={restoringVersion !== null} onClick={() => handleRestore(version.versionNumber)}>
                          {restoringVersion === version.versionNumber ? 'משחזר...' : 'שחזור גרסה זו'}
                        </button>
                      </div>
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
