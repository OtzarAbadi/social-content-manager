function EmptyState({ icon: Icon, title, description, actionLabel, onAction }) {
  return (
    <div className="empty-state">
      {Icon && <span className="empty-state-icon"><Icon size={28} aria-hidden="true" /></span>}
      <strong>{title}</strong>
      {description && <p>{description}</p>}
      {actionLabel && onAction && <button type="button" className="primary-button" onClick={onAction}>{actionLabel}</button>}
    </div>
  )
}

export default EmptyState
