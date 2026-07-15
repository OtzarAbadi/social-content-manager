import { useEffect, useState } from 'react'
import axios from 'axios'
import NotificationsMenu from './NotificationsMenu.jsx'

const api = axios.create({ baseURL: 'http://localhost:8081', withCredentials: true })

const icons = {
  dashboard: '⌂', calendar: '▦', contents: '▤', clients: '♙', comments: '◌',
  analytics: '↗', notifications: '♢', logout: '↪',
}

const pageTitles = {
  dashboard: 'לוח בקרה', calendar: 'לוח תוכן', content: 'ניהול תוכן',
  clients: 'ניהול לקוחות', messages: 'הודעות ותגובות',
}

function initials(profile) {
  const name = profile?.fullName || profile?.username || 'SS'
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function PageShell({ activeRoute, routes, onNavigate, isAuthenticated, onLogout, children }) {
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    if (!isAuthenticated) return
    Promise.resolve().then(async () => {
      try { setProfile((await api.get('/users/me')).data) } catch { setProfile(null) }
    })
  }, [isAuthenticated])

  function navigate(event, routeKey) {
    event.preventDefault()
    onNavigate(routeKey)
  }

  if (!isAuthenticated) {
    return <main className="public-shell" dir="rtl">{children}</main>
  }

  const isAdmin = profile?.role ? profile.role !== 'CLIENT' : false

  return (
    <div className="authenticated-shell" dir="rtl">
      <aside className="app-sidebar" aria-label="ניווט ראשי">
        <div className="brand-lockup"><span className="brand-mark">S</span><div><strong>SSCM</strong><small>Social Studio</small></div></div>
        <nav className="sidebar-nav">
          <a className={`route-dashboard ${activeRoute === 'dashboard' ? 'active' : ''}`} href={routes.dashboard.path} onClick={(e) => navigate(e, 'dashboard')}><i>{icons.dashboard}</i><span>לוח בקרה</span></a>
          <a className={`route-calendar ${activeRoute === 'calendar' ? 'active' : ''}`} href={routes.calendar.path} onClick={(e) => navigate(e, 'calendar')}><i>{icons.calendar}</i><span>לוח שנה</span></a>
          <a className={`route-content ${activeRoute === 'content' ? 'active' : ''}`} href={routes.content.path} onClick={(e) => navigate(e, 'content')}><i>{icons.contents}</i><span>תוכן</span></a>
          {isAdmin && <a className={`route-clients ${activeRoute === 'clients' ? 'active' : ''}`} href={routes.clients.path} onClick={(e) => navigate(e, 'clients')}><i>{icons.clients}</i><span>לקוחות</span></a>}
          <a className={`route-messages ${activeRoute === 'messages' ? 'active' : ''}`} href={routes.messages.path} onClick={(e) => navigate(e, 'messages')}><i>{icons.comments}</i><span>הודעות</span></a>
          <button className="disabled-nav" type="button" disabled title="בקרוב"><i>{icons.analytics}</i><span>אנליטיקה</span><small>בקרוב</small></button>
          <button className="disabled-nav" type="button" disabled title="בקרוב"><i>{icons.notifications}</i><span>התראות</span><small>בקרוב</small></button>
        </nav>
        <button className="sidebar-logout" type="button" onClick={onLogout}><i>{icons.logout}</i><span>התנתקות</span></button>
      </aside>

      <div className="app-main">
        <header className="app-topbar">
          <div><p className="topbar-kicker">Smart Social Content Manager</p><h1>{pageTitles[activeRoute] || 'SSCM'}</h1></div>
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
        <button type="button" onClick={onLogout}><i>{icons.logout}</i><span>יציאה</span></button>
      </nav>
    </div>
  )
}

export default PageShell
