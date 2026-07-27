function Toolbar({ children, label = 'פעולות', className = '' }) {
  return <div className={`action-toolbar ${className}`} role="toolbar" aria-label={label}>{children}</div>
}

export default Toolbar
