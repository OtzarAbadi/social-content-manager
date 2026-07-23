import { useEffect, useState } from 'react'
import Cookies from 'js-cookie'
import './App.css'

import DashboardPage from './pages/DashboardPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import CalendarPage from './pages/CalendarPage.jsx'
import AnalyticsPage from './pages/AnalyticsPage.jsx'
import NotificationsPage from './pages/NotificationsPage.jsx'
import ActivityPage from './pages/ActivityPage.jsx'

const defaultRoute = 'login'

const routes = {
  dashboard: {
    path: '/dashboard',
    label: 'דשבורד',
    Component: DashboardPage,
  },
  content: {
    path: '/content',
    label: 'תוכן',
    Component: DashboardPage,
  },
  clients: {
    path: '/clients',
    label: 'לקוחות',
    Component: DashboardPage,
  },
  messages: {
    path: '/messages',
    label: 'הודעות',
    Component: DashboardPage,
  },
  calendar: {
    path: '/calendar',
    label: 'לוח שנה',
    Component: CalendarPage,
  },
  analytics: {
    path: '/analytics',
    label: 'אנליטיקה',
    Component: AnalyticsPage,
  },
  notifications: {
    path: '/notifications',
    label: 'התראות',
    Component: NotificationsPage,
  },
  activity: {
    path: '/activity',
    label: 'מרכז הפעילות',
    Component: ActivityPage,
  },
  login: {
    path: '/login',
    label: 'לוגין',
    Component: LoginPage,
  },
}

function getRouteFromPath() {
  const matchingRoute = Object.entries(routes).find(
      ([, route]) => route.path === window.location.pathname
  )

  return matchingRoute?.[0] ?? defaultRoute
}

function App() {
  const [activeRoute, setActiveRoute] = useState(getRouteFromPath)
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(Cookies.get('token')))
  const ActivePage = routes[activeRoute].Component

  useEffect(() => {

    const syncRouteWithUrl = () => {
      const requestedRoute = getRouteFromPath()
      if (!isAuthenticated && requestedRoute !== 'login') {
        window.history.replaceState(null, '', routes.login.path)
        setActiveRoute('login')
        return
      }
      setActiveRoute(requestedRoute)
    }

    window.addEventListener('popstate', syncRouteWithUrl)

    Promise.resolve().then(() => {
      if (!isAuthenticated && getRouteFromPath() !== 'login') {
        window.history.replaceState(null, '', routes.login.path)
        setActiveRoute('login')
      } else if (isAuthenticated && getRouteFromPath() === 'login') {
        window.history.replaceState(null, '', routes.dashboard.path)
        setActiveRoute('dashboard')
      } else if (!Object.values(routes).some(r => r.path === window.location.pathname)) {
        window.history.replaceState(null, '', routes[defaultRoute].path)
      }
    })

    return () => {
      window.removeEventListener('popstate', syncRouteWithUrl)
    }

  }, [isAuthenticated])

  function navigateTo(routeKey) {
    window.history.pushState(null, '', routes[routeKey].path)
    setActiveRoute(routeKey)
  }

  function handleAuthenticated() {
    setIsAuthenticated(true)
    navigateTo('dashboard')
  }

  function handleLogout() {
    Cookies.remove('token', {
      secure: window.location.protocol === 'https:',
      sameSite: 'strict',
    })
    setIsAuthenticated(false)
    navigateTo('login')
  }

  return (
      <ActivePage
          activeRoute={activeRoute}
          routes={routes}
          onNavigate={navigateTo}
          isAuthenticated={isAuthenticated}
          onAuthenticated={handleAuthenticated}
          onLogout={handleLogout}
      />
  )
}

export default App
