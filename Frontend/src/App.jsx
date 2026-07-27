import { lazy, Suspense, useEffect, useState } from 'react'
import Cookies from 'js-cookie'
import './App.css'

import GlobalToast from './components/GlobalToast.jsx'

const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const LoginPage = lazy(() => import('./pages/LoginPage.jsx'))
const CalendarPage = lazy(() => import('./pages/CalendarPage.jsx'))
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage.jsx'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage.jsx'))
const ActivityPage = lazy(() => import('./pages/ActivityPage.jsx'))
const IntegrationsPage = lazy(() => import('./pages/IntegrationsPage.jsx'))

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
      <GlobalToast />
    </>
  )
}

export default App
