import { useEffect, useState } from 'react'
import './App.css'

import DashboardPage from './pages/DashboardPage.jsx'
import LoginPage from './pages/LoginPage.jsx'

const defaultRoute = 'login'

const routes = {
  dashboard: {
    path: '/dashboard',
    label: 'דשבורד',
    Component: DashboardPage,
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
  const ActivePage = routes[activeRoute].Component

  useEffect(() => {

    const syncRouteWithUrl = () => {
      setActiveRoute(getRouteFromPath())
    }

    window.addEventListener('popstate', syncRouteWithUrl)

    if (!Object.values(routes).some(r => r.path === window.location.pathname)) {
      window.history.replaceState(null, '', routes[defaultRoute].path)
    }

    return () => {
      window.removeEventListener('popstate', syncRouteWithUrl)
    }

  }, [])

  function navigateTo(routeKey) {
    window.history.pushState(null, '', routes[routeKey].path)
    setActiveRoute(routeKey)
  }

  return (
      <ActivePage
          activeRoute={activeRoute}
          routes={routes}
          onNavigate={navigateTo}
      />
  )
}

export default App