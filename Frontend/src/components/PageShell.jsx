function PageShell({ activeRoute, routes, onNavigate, children }) {
  return (
    <main className="app-shell" dir="rtl">
      <header className="topbar">
        <div>
          <p className="eyebrow">ניהול תוכן חברתי</p>
          <h1>סטודיו פלואו</h1>
        </div>

        <nav className="page-tabs" aria-label="עמודים">
          {Object.entries(routes).map(([routeKey, route]) => (
            <a
              className={activeRoute === routeKey ? 'active' : ''}
              href={route.path}
              key={routeKey}
              onClick={(event) => onNavigate(routeKey, event)}
            >
              {route.label}
            </a>
          ))}
        </nav>
      </header>

      {children}
    </main>
  )
}

export default PageShell
