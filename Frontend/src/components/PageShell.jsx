import { useEffect, useState } from 'react'
import {
  Activity, BarChart3, Bell, CalendarDays, FileText, LayoutDashboard,
  LogOut, MessageCircle, Plug, Users,
} from 'lucide-react'
import NotificationsMenu from './NotificationsMenu.jsx'
import api from '../services/api.js'
import { APP_INITIAL, APP_NAME } from '../config/appConfig.js'

const icons = {
  dashboard: <LayoutDashboard />, calendar: <CalendarDays />, contents: <FileText />,
  clients: <Users />, comments: <MessageCircle />, analytics: <BarChart3 />,
  notifications: <Bell />, activity: <Activity />, integrations: <Plug />, logout: <LogOut />,
}

const pageTitles = {
  analytics: 'אנליטיקה',
  dashboard: 'לוח בקרה', calendar: 'לוח תוכן', content: 'ניהול תוכן',
  clients: 'ניהול לקוחות', messages: 'הודעות ותגובות',
  notifications: 'התראות',
  activity: 'מרכז הפעילות',
  integrations: 'אינטגרציות',
}

function initials(profile) {
  const name = profile?.fullName || profile?.username || 'SS'
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function PageShell({ activeRoute, routes, onNavigate, isAuthenticated, onLogout, children }) {
  const [profile, setProfile] = useState(null)
  const [profileError, setProfileError] = useState('')

  useEffect(() => {
    if (!isAuthenticated) return
    Promise.resolve().then(async () => {
      setProfileError('')
      try {
        setProfile((await api.get('/users/me')).data)
      } catch {
        setProfile(null)
        setProfileError('Unable to load your profile. Check the backend connection and try again.')
      }
    })
  }, [isAuthenticated])

  function navigate(event, routeKey) {
    event.preventDefault()
    onNavigate(routeKey)
  }

  if (!isAuthenticated) {
    return <main className="public-shell" dir="rtl">{children}</main>
  }

  const isAdmin = profile?.role === 'ADMIN'
  const canViewAnalytics = profile?.role === 'ADMIN' || profile?.role === 'CLIENT'
  const canViewNotifications = profile?.role === 'ADMIN' || profile?.role === 'CLIENT'
  const canViewActivity = profile?.role === 'ADMIN' || profile?.role === 'CLIENT'

  return (
    <div className="authenticated-shell" dir="rtl">
      <aside className="app-sidebar" aria-label="ניווט ראשי">
        <div className="brand-lockup"><span className="brand-mark">{APP_INITIAL}</span><div><strong>{APP_NAME}</strong></div></div>
        <nav className="sidebar-nav">
          <a className={`route-dashboard ${activeRoute === 'dashboard' ? 'active' : ''}`} href={routes.dashboard.path} onClick={(e) => navigate(e, 'dashboard')}><i>{icons.dashboard}</i><span>לוח בקרה</span></a>
          <a className={`route-calendar ${activeRoute === 'calendar' ? 'active' : ''}`} href={routes.calendar.path} onClick={(e) => navigate(e, 'calendar')}><i>{icons.calendar}</i><span>לוח שנה</span></a>
          <a className={`route-content ${activeRoute === 'content' ? 'active' : ''}`} href={routes.content.path} onClick={(e) => navigate(e, 'content')}><i>{icons.contents}</i><span>תוכן</span></a>
          {isAdmin && <a className={`route-clients ${activeRoute === 'clients' ? 'active' : ''}`} href={routes.clients.path} onClick={(e) => navigate(e, 'clients')}><i>{icons.clients}</i><span>לקוחות</span></a>}
          <a className={`route-messages ${activeRoute === 'messages' ? 'active' : ''}`} href={routes.messages.path} onClick={(e) => navigate(e, 'messages')}><i>{icons.comments}</i><span>הודעות</span></a>
          {canViewAnalytics && <a className={`route-analytics ${activeRoute === 'analytics' ? 'active' : ''}`} href={routes.analytics.path} onClick={(e) => navigate(e, 'analytics')}><i>{icons.analytics}</i><span>אנליטיקה</span></a>}
          {canViewNotifications && <a className={`route-notifications ${activeRoute === 'notifications' ? 'active' : ''}`} href={routes.notifications.path} onClick={(e) => navigate(e, 'notifications')}><i>{icons.notifications}</i><span>התראות</span></a>}
          {canViewActivity && <a className={`route-activity ${activeRoute === 'activity' ? 'active' : ''}`} href={routes.activity.path} onClick={(e) => navigate(e, 'activity')}><i>{icons.activity}</i><span>מרכז הפעילות</span></a>}
          {isAdmin && <a className={`route-integrations ${activeRoute === 'integrations' ? 'active' : ''}`} href={routes.integrations.path} onClick={(e) => navigate(e, 'integrations')}><i>{icons.integrations}</i><span>אינטגרציות</span></a>}
        </nav>
        <button className="sidebar-logout" type="button" onClick={onLogout}><i>{icons.logout}</i><span>התנתקות</span></button>
      </aside>

      <div className="app-main">
        {profileError && <p className="error-banner" role="alert">{profileError}</p>}
        <header className="app-topbar">
          <div><p className="topbar-kicker">{APP_NAME}</p><h1>{pageTitles[activeRoute] || APP_NAME}</h1></div>
          <div className="topbar-profile">
            <NotificationsMenu onNavigate={onNavigate} />
            <div className="profile-copy"><strong>{profile?.fullName || profile?.username || 'משתמש'}</strong><span>{profile ? (isAdmin ? 'מנהל' : 'לקוח') : 'טוען...'}</span></div>
            <span className="profile-avatar" aria-hidden="true">{initials(profile)}</span>
          </div>
        </header>
        <main className="app-content">{children}</main>
      </div>

      <nav className="mobile-nav" aria-label="ניווט לנייד">
        <a className={activeRoute === 'dashboard' ? 'active' : ''} href={routes.dashboard.path} onClick={(e) => navigate(e, 'dashboard')}><i>{icons.dashboard}</i><span>בית</span></a>
        <a className={activeRoute === 'calendar' ? 'active' : ''} href={routes.calendar.path} onClick={(e) => navigate(e, 'calendar')}><i>{icons.calendar}</i><span>לוח שנה</span></a>
        <a className={activeRoute === 'content' ? 'active' : ''} href={routes.content.path} onClick={(e) => navigate(e, 'content')}><i>{icons.contents}</i><span>תוכן</span></a>
        <a className={activeRoute === 'messages' ? 'active' : ''} href={routes.messages.path} onClick={(e) => navigate(e, 'messages')}><i>{icons.comments}</i><span>הודעות</span></a>
        {canViewAnalytics && <a className={activeRoute === 'analytics' ? 'active' : ''} href={routes.analytics.path} onClick={(e) => navigate(e, 'analytics')}><i>{icons.analytics}</i><span>אנליטיקה</span></a>}
        {canViewNotifications && <a className={activeRoute === 'notifications' ? 'active' : ''} href={routes.notifications.path} onClick={(e) => navigate(e, 'notifications')}><i>{icons.notifications}</i><span>התראות</span></a>}
        {canViewActivity && <a className={activeRoute === 'activity' ? 'active' : ''} href={routes.activity.path} onClick={(e) => navigate(e, 'activity')}><i>{icons.activity}</i><span>פעילות</span></a>}
        <button type="button" onClick={onLogout}><i>{icons.logout}</i><span>יציאה</span></button>
      </nav>
    </div>
  )
}

export default PageShell
