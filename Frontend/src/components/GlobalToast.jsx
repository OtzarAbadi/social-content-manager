import { useEffect, useRef, useState } from 'react'
import { AlertCircle, X } from 'lucide-react'

function GlobalToast() {
  const [message, setMessage] = useState('')
  const timeoutRef = useRef(null)

  useEffect(() => {
    const showToast = (event) => {
      window.clearTimeout(timeoutRef.current)
      setMessage(event.detail?.message || 'הפעולה נכשלה. אפשר לנסות שוב.')
      timeoutRef.current = window.setTimeout(() => setMessage(''), 5000)
    }
    window.addEventListener('sscm:api-error', showToast)
    return () => {
      window.removeEventListener('sscm:api-error', showToast)
      window.clearTimeout(timeoutRef.current)
    }
  }, [])

  if (!message) return null

  return (
    <aside className="global-toast" role="alert" aria-live="assertive">
      <AlertCircle size={20} aria-hidden="true" />
      <span>{message}</span>
      <button type="button" onClick={() => setMessage('')} aria-label="סגירת הודעת השגיאה"><X size={18} /></button>
    </aside>
  )
}

export default GlobalToast
