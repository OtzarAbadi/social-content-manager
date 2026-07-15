function PageShell({ activeRoute, routes, onNavigate, isAuthenticated, onLogout, children }) {
  return (
    <main className="app-shell" dir="rtl">
      <header className="topbar">
        <div>
          <p className="eyebrow">ניהול תוכן חברתי</p>
          <h1>סטודיו פלואו</h1>
        </div>

        {isAuthenticated && (
        <nav className="page-tabs" aria-label="עמודים">
          {Object.entries(routes).filter(([routeKey]) => routeKey !== 'login').map(([routeKey, route]) => (
            <a
              className={activeRoute === routeKey ? 'active' : ''}
              href={route.path}
              key={routeKey}
              onClick={(event) => onNavigate(routeKey, event)}
            >
              {route.label}
            </a>
          ))}
          <button type="button" onClick={onLogout}>התנתקות</button>
        </nav>
        )}
      </header>

      {children}
    </main>
  )
}

export default PageShell
