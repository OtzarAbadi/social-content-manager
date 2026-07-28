import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'

function CreationModal({ open, titleId, closeLabel, onClose, children }) {
  const dialogRef = useRef(null)
  const returnFocusRef = useRef(null)
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!open) return undefined

    returnFocusRef.current = document.activeElement
    const previousBodyOverflow = document.body.style.overflow
    const previousHtmlOverflow = document.documentElement.style.overflow
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'

    const focusFrame = window.requestAnimationFrame(() => dialogRef.current?.focus())
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onCloseRef.current()
      }
    }
    window.addEventListener('keydown', closeOnEscape)

    return () => {
      window.cancelAnimationFrame(focusFrame)
      window.removeEventListener('keydown', closeOnEscape)
      document.body.style.overflow = previousBodyOverflow
      document.documentElement.style.overflow = previousHtmlOverflow
      window.requestAnimationFrame(() => returnFocusRef.current?.focus())
    }
  }, [open])

  if (!open) return null

  return (
    <div className="creation-modal-overlay" role="presentation" onMouseDown={onClose}>
      <section
        className="creation-modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        dir="rtl"
        ref={dialogRef}
        tabIndex="-1"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button type="button" className="modal-close creation-modal-close" onClick={onClose} aria-label={closeLabel}>
          <X size={20} aria-hidden="true" />
        </button>
        {children}
      </section>
    </div>
  )
}

export default CreationModal
