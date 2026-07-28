import statusDesign from './statusDesign.js'

function StatusBadge({ status, className = '' }) {
  const meta = statusDesign[status] || statusDesign.DRAFT
  return <span className={`status-badge-ui status-${status || 'DRAFT'} ${className}`.trim()} aria-label={`סטטוס: ${meta.label}`}>{meta.label}</span>
}

export default StatusBadge
