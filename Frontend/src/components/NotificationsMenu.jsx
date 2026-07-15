import { useCallback, useEffect, useRef, useState } from 'react'
import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8081', withCredentials: true })

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('he-IL', { dateStyle: 'short', timeStyle: 'short' })
}

function NotificationsMenu({ onNavigate }) {
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const rootRef = useRef(null)

  const loadCount = useCallback(async () => {
    try { setUnreadCount((await api.get('/notifications/unread-count')).data.count || 0) } catch { /* retry later */ }
  }, [])

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/notifications')
      setNotifications(response.data)
      setUnreadCount(response.data.filter((item) => !(item.read ?? item.isRead)).length)
    } catch {
      setError('לא הצלחנו לטעון את ההתראות')
    } finally { setLoading(false) }
  }, [])

  useEffect(() => {
    Promise.resolve().then(loadCount)
    const interval = window.setInterval(loadCount, 45000)
    return () => window.clearInterval(interval)
  }, [loadCount])

  useEffect(() => {
    if (!open) return
    Promise.resolve().then(loadNotifications)
    const close = (event) => {
      if (event.key === 'Escape' || (event.type === 'mousedown' && !rootRef.current?.contains(event.target))) setOpen(false)
    }
    document.addEventListener('keydown', close)
    document.addEventListener('mousedown', close)
    return () => { document.removeEventListener('keydown', close); document.removeEventListener('mousedown', close) }
  }, [open, loadNotifications])

  async function openNotification(notification) {
    if (!(notification.read ?? notification.isRead)) {
      await api.put(`/notifications/${notification.notificationId}/read`)
      setNotifications((items) => items.map((item) => item.notificationId === notification.notificationId ? { ...item, read: true } : item))
      setUnreadCount((count) => Math.max(0, count - 1))
    }
    if (notification.relatedContentId) { setOpen(false); onNavigate('content') }
  }

  async function markAllRead() {
    await api.put('/notifications/read-all')
    setNotifications((items) => items.map((item) => ({ ...item, read: true })))
    setUnreadCount(0)
  }

  return <div className="notifications-menu" ref={rootRef}>
    <button className="notification-button" type="button" aria-label="התראות" aria-haspopup="dialog" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <svg className="notification-bell-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
      </svg>
      {unreadCount > 0 && <span className="notification-count">{unreadCount > 99 ? '99+' : unreadCount}</span>}
    </button>
    {open && <section className="notifications-panel" aria-label="התראות">
      <header><div><p className="eyebrow">עדכונים</p><h2>התראות</h2></div>{unreadCount > 0 && <button type="button" onClick={markAllRead}>סימון הכל כנקרא</button>}</header>
      <div className="notifications-list">
        {loading && <p className="notification-state">טוען התראות...</p>}
        {error && <p className="notification-state notification-state-error">{error}<button type="button" onClick={loadNotifications}>נסו שוב</button></p>}
        {!loading && !error && notifications.length === 0 && <p className="notification-state">אין התראות חדשות</p>}
        {!loading && !error && notifications.map((notification) => {
          const unread = !(notification.read ?? notification.isRead)
          return <button className={`notification-item ${unread ? 'unread' : ''}`} type="button" key={notification.notificationId} onClick={() => openNotification(notification)}>
            <span className="notification-dot" /><span><strong>{notification.title}</strong><small>{notification.message}</small><time>{formatDate(notification.createdAt)}</time></span>
          </button>
        })}
      </div>
    </section>}
  </div>
}

export default NotificationsMenu
