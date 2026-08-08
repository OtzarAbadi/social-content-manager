import { lazy, Suspense, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import './App.css'

import GlobalToast from './components/GlobalToast.jsx'
import PwaStatus from './components/PwaStatus.jsx'
import api from './services/api.js'

const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const LoginPage = lazy(() => import('./pages/LoginPage.jsx'))
const CalendarPage = lazy(() => import('./pages/CalendarPage.jsx'))
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage.jsx'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage.jsx'))
const ActivityPage = lazy(() => import('./pages/ActivityPage.jsx'))
const IntegrationsPage = lazy(() => import('./pages/IntegrationsPage.jsx'))
const FeedPage = lazy(() => import('./pages/FeedPage.jsx'))

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
  feed: {
    path: '/feed',
    label: 'פיד',
    Component: FeedPage,
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
  integrations: {
    path: '/integrations',
    label: 'אינטגרציות',
    Component: IntegrationsPage,
  },
  login: {
    path: '/login',
    label: 'לוגין',
    Component: LoginPage,
  },
}

function getRouteFromPath(pathname = window.location.pathname, search = window.location.search) {
  if (/^\/content\/\d+\/?$/.test(pathname)) {
    return new URLSearchParams(search).get('tab') === 'comments' ? 'messages' : 'content'
  }
  const matchingRoute = Object.entries(routes).find(
      ([, route]) => route.path === pathname
  )

  return matchingRoute?.[0] ?? defaultRoute
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [activeRoute, setActiveRoute] = useState(() => getRouteFromPath(location.pathname, location.search))
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [currentRole, setCurrentRole] = useState(null)
  const [isAuthResolved, setIsAuthResolved] = useState(false)
  const ActivePage = routes[activeRoute].Component

  useEffect(() => {
    let active = true
    api.get('/users/me', { suppressGlobalErrorToast: true })
      .then((response) => {
        if (active) {
          setCurrentRole(response.data?.role || null)
          setIsAuthenticated(true)
        }
      })
      .catch(() => {
        if (active) {
          setCurrentRole(null)
          setIsAuthenticated(false)
        }
      })
      .finally(() => {
        if (active) setIsAuthResolved(true)
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!isAuthResolved) return
    Promise.resolve().then(() => {
      const requestedRoute = getRouteFromPath(location.pathname, location.search)
      if (!isAuthenticated && requestedRoute !== 'login') {
        navigate(routes.login.path, { replace: true })
        setActiveRoute('login')
      } else if (isAuthenticated && requestedRoute === 'login') {
        navigate(routes.dashboard.path, { replace: true })
        setActiveRoute('dashboard')
      } else if (requestedRoute === 'integrations' && currentRole !== 'ADMIN') {
        navigate(routes.dashboard.path, { replace: true })
        setActiveRoute('dashboard')
      } else {
        setActiveRoute(requestedRoute)
      }
    })
  }, [isAuthResolved, isAuthenticated, currentRole, location.pathname, location.search, navigate])

  function navigateTo(routeKey) {
    const allowedRoute = routeKey === 'integrations' && currentRole !== 'ADMIN' ? 'dashboard' : routeKey
    navigate(routes[allowedRoute].path)
    setActiveRoute(allowedRoute)
  }

  function handleAuthenticated() {
    setIsAuthenticated(true)
    navigateTo('dashboard')
  }

  async function handleLogout() {
    try {
      await api.post('/users/logout', null, { suppressGlobalErrorToast: true })
    } finally {
      setIsAuthenticated(false)
      setCurrentRole(null)
      navigateTo('login')
    }
  }

  if (!isAuthResolved) {
    return <><div className="route-loader" role="status" aria-label="טוען יישום"><span /></div><PwaStatus /></>
  }

  return (
    <>
      <Suspense fallback={<div className="route-loader" role="status" aria-label="טוען עמוד"><span /></div>}>
      <ActivePage
          activeRoute={activeRoute}
          routes={routes}
          onNavigate={navigateTo}
          isAuthenticated={isAuthenticated}
          onAuthenticated={handleAuthenticated}
          onLogout={handleLogout}
      />
      </Suspense>
      <PwaStatus />
      <GlobalToast />
    </>
  )
}

export default App
