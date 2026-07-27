function Skeleton({ rows = 3, className = '' }) {
  return <div className={`skeleton-stack ${className}`} role="status" aria-label="טוען נתונים">
    {Array.from({ length: rows }, (_, index) => <span className="skeleton-row" key={index} />)}
  </div>
}

export default Skeleton
