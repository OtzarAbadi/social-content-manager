import { useEffect, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, X } from 'lucide-react'

function GlobalToast() {
  const [toast, setToast] = useState({ message: '', type: 'error' })
  const timeoutRef = useRef(null)

  useEffect(() => {
    const showToast = (event) => {
      window.clearTimeout(timeoutRef.current)
      setToast({
        message: event.detail?.message || 'הפעולה נכשלה. אפשר לנסות שוב.',
        type: event.detail?.type || 'error',
      })
      timeoutRef.current = window.setTimeout(() => setToast({ message: '', type: 'error' }), 5000)
    }
    window.addEventListener('sscm:api-error', showToast)
    window.addEventListener('sscm:toast', showToast)
    return () => {
      window.removeEventListener('sscm:api-error', showToast)
      window.removeEventListener('sscm:toast', showToast)
      window.clearTimeout(timeoutRef.current)
    }
  }, [])

  if (!toast.message) return null

  return (
    <aside className={`global-toast toast-${toast.type}`} role="alert" aria-live="assertive">
      {toast.type === 'success' ? <CheckCircle2 size={20} aria-hidden="true" /> : <AlertCircle size={20} aria-hidden="true" />}
      <span>{toast.message}</span>
      <button type="button" onClick={() => setToast({ message: '', type: 'error' })} aria-label="סגירת ההודעה"><X size={18} /></button>
    </aside>
  )
}

export default GlobalToast
