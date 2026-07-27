import { useRef, useState } from 'react'
import { CheckCircle2, Send, X } from 'lucide-react'
import { getMediaType } from '../utils/imageUrl.js'
import {
  getInstagramPublishErrorMessage,
  publishContentToInstagram,
} from '../api/publishing.js'
import { showToast } from '../utils/toast.js'

function InstagramPublishAction({ content, role, publishedMediaId, onPublished }) {
  const [confirming, setConfirming] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const requestLock = useRef(false)
  const contentId = content.content_id ?? content.contentId
  const isEligible = role === 'ADMIN'
    && content.status === 'APPROVED'
    && Boolean(content.file_url)
    && getMediaType(content.file_url, content.content_type) === 'image'

  if (!isEligible) return null

  async function publish() {
    if (requestLock.current || publishedMediaId) return
    requestLock.current = true
    setPublishing(true)
    try {
      const result = await publishContentToInstagram(contentId)
      onPublished(contentId, result.instagramMediaId)
      setConfirming(false)
      showToast('התוכן פורסם בהצלחה באינסטגרם.', 'success')
    } catch (error) {
      showToast(getInstagramPublishErrorMessage(error), 'error')
    } finally {
      requestLock.current = false
      setPublishing(false)
    }
  }

  return (
    <div className="instagram-publish-action">
      <button
        type="button"
        className={`secondary-button small-button instagram-button ${publishedMediaId ? 'published' : ''}`}
        disabled={publishing || Boolean(publishedMediaId)}
        onClick={() => setConfirming(true)}
      >
        {publishedMediaId
          ? <><CheckCircle2 size={20} />פורסם באינסטגרם</>
          : <><Send size={20} />פרסום באינסטגרם</>}
      </button>

      {publishedMediaId && (
        <details className="instagram-technical-details">
          <summary>פרטים טכניים</summary>
          <span dir="ltr">Instagram media ID: {publishedMediaId}</span>
        </details>
      )}

      {confirming && !publishedMediaId && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal-card instagram-confirmation" role="dialog" aria-modal="true" aria-labelledby={`instagram-confirm-title-${contentId}`}>
            <button className="modal-close" type="button" onClick={() => setConfirming(false)} disabled={publishing} aria-label="סגירת חלון האישור"><X size={20} /></button>
            <h2 id={`instagram-confirm-title-${contentId}`}>פרסום אמיתי באינסטגרם</h2>
            <p>הפעולה תפרסם פוסט אמיתי בחשבון האינסטגרם המחובר. לא ניתן לבטל את הפרסום מתוך המערכת.</p>
            <div className="modal-actions">
              <button type="button" className="ghost-button" onClick={() => setConfirming(false)} disabled={publishing}>ביטול</button>
              <button type="button" className="primary-button" onClick={publish} disabled={publishing}>
                {publishing ? <><span className="button-spinner" />מפרסם...</> : <><Send size={20} />אישור ופרסום</>}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export default InstagramPublishAction
