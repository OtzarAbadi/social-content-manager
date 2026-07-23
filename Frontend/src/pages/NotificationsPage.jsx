import { useCallback, useEffect, useState } from 'react'
import PageShell from '../components/PageShell.jsx'
import {
  announceNotificationsUpdated,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  NOTIFICATIONS_UPDATED_EVENT,
} from '../api/notifications.js'

function errorMessage(error) {
  if (error?.response?.status === 401) return 'ההתחברות פגה. יש להתחבר מחדש כדי לצפות בהתראות.'
  if (error?.response?.status === 403) return 'אין לך הרשאה לצפות בהתראות.'
  return 'לא הצלחנו לטעון את ההתראות. אפשר לנסות שוב.'
}

function formatDate(value) {
  if (!value) return 'מועד לא זמין'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'מועד לא זמין'
    : date.toLocaleString('he-IL', { dateStyle: 'short', timeStyle: 'short' })
}

function NotificationsPage({ activeRoute, routes, onNavigate, isAuthenticated, onLogout }) {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updatingId, setUpdatingId] = useState(null)
  const [markingAll, setMarkingAll] = useState(false)

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setNotifications(await getNotifications())
    } catch (requestError) {
      setNotifications([])
      setError(errorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    Promise.resolve().then(loadNotifications)
    const refresh = () => loadNotifications()
    window.addEventListener(NOTIFICATIONS_UPDATED_EVENT, refresh)
    return () => window.removeEventListener(NOTIFICATIONS_UPDATED_EVENT, refresh)
  }, [loadNotifications])

  const hasUnread = notifications.some((item) => !(item.read ?? item.isRead))

  async function markOneRead(notification) {
    if (notification.read ?? notification.isRead) return
    setUpdatingId(notification.notificationId)
    setError('')
    try {
      await markNotificationRead(notification.notificationId)
      setNotifications((items) => items.map((item) => (
        item.notificationId === notification.notificationId ? { ...item, read: true } : item
      )))
      announceNotificationsUpdated()
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setUpdatingId(null)
    }
  }

  async function markAllRead() {
    setMarkingAll(true)
    setError('')
    try {
      await markAllNotificationsRead()
      setNotifications((items) => items.map((item) => ({ ...item, read: true })))
      announceNotificationsUpdated()
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setMarkingAll(false)
    }
  }

  return <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate} isAuthenticated={isAuthenticated} onLogout={onLogout}>
    <section className="notifications-page" dir="rtl" aria-labelledby="notifications-page-title">
      <header className="notifications-page-heading">
        <div>
          <p className="eyebrow">עדכונים</p>
          <h2 id="notifications-page-title">כל ההתראות</h2>
          <p>העדכונים האחרונים הזמינים עבורך במקום אחד.</p>
        </div>
        {hasUnread && <button type="button" className="secondary-button" onClick={markAllRead} disabled={markingAll}>
          {markingAll ? 'מסמן...' : 'סימון הכל כנקרא'}
        </button>}
      </header>

      {loading && <div className="notifications-page-state" role="status">טוען התראות...</div>}
      {!loading && error && <div className="notifications-page-state notifications-page-error" role="alert">
        <p>{error}</p>
        <button type="button" className="secondary-button" onClick={loadNotifications}>נסו שוב</button>
      </div>}
      {!loading && !error && notifications.length === 0 && <div className="notifications-page-state">
        <strong>אין התראות להצגה</strong>
        <p>התראות חדשות יופיעו כאן כשיהיו עדכונים.</p>
      </div>}

      {!loading && notifications.length > 0 && <div className="notifications-page-list">
        {notifications.map((notification) => {
          const unread = !(notification.read ?? notification.isRead)
          return <article className={`notifications-page-item ${unread ? 'unread' : ''}`} key={notification.notificationId}>
            <span className="notification-dot" aria-hidden="true" />
            <div className="notifications-page-copy">
              <div className="notifications-page-item-heading">
                <div>
                  {notification.type && <span className="notifications-page-type">{notification.type}</span>}
                  <h3>{notification.title || 'התראה'}</h3>
                </div>
                <span className="notifications-page-read-state">{unread ? 'לא נקראה' : 'נקראה'}</span>
              </div>
              {notification.message && <p>{notification.message}</p>}
              <time dateTime={notification.createdAt || undefined}>{formatDate(notification.createdAt)}</time>
            </div>
            {unread && <button type="button" className="secondary-button small-button" onClick={() => markOneRead(notification)} disabled={updatingId === notification.notificationId}>
              {updatingId === notification.notificationId ? 'מסמן...' : 'סימון כנקרא'}
            </button>}
          </article>
        })}
      </div>}
    </section>
  </PageShell>
}

export default NotificationsPage
