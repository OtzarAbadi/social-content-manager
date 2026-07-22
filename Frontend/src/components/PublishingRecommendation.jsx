import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { getPublishingRecommendation } from '../api/publishingRecommendations.js'

function recommendationError(error) {
  const status = error?.response?.status
  if (status === 400) return 'יש לבדוק את פרטי התוכן ומילות המפתח ולנסות שוב.'
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש.'
  if (status === 403) return 'אין לך הרשאה לקבל המלצת פרסום.'
  return 'לא הצלחנו ליצור המלצה. אפשר לנסות שוב.'
}

function formatRecommendedDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value || '')
  if (!match) return 'מועד לא זמין'
  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])))
  const weekday = new Intl.DateTimeFormat('he-IL', { weekday: 'long', timeZone: 'UTC' }).format(date)
  return `${weekday}, ${match[3]}.${match[2]}.${match[1]} בשעה ${match[4]}:${match[5]}`
}

function PublishingRecommendation({ contentType, title, clientId, plannedPublishDate, onApply }) {
  const [keywords, setKeywords] = useState('')
  const [recommendation, setRecommendation] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const requestRef = useRef(null)

  useEffect(() => () => requestRef.current?.abort(), [])

  const validClientId = Number(clientId) > 0
  const canGenerate = Boolean(title?.trim() && contentType && validClientId)

  async function generate() {
    requestRef.current?.abort()
    const controller = new AbortController()
    requestRef.current = controller
    setLoading(true)
    setError('')

    try {
      const response = await getPublishingRecommendation({
        contentType,
        title: title.trim(),
        keywords: keywords.split(',').map((keyword) => keyword.trim()).filter(Boolean),
        clientId: Number(clientId),
        existingPlannedPublishDate: plannedPublishDate || null,
      }, controller.signal)
      if (requestRef.current === controller) setRecommendation(response)
    } catch (requestError) {
      if (!axios.isCancel(requestError) && requestRef.current === controller) {
        setError(recommendationError(requestError))
      }
    } finally {
      if (requestRef.current === controller) {
        requestRef.current = null
        setLoading(false)
      }
    }
  }

  function handleKeywordsChange(event) {
    requestRef.current?.abort()
    requestRef.current = null
    setKeywords(event.target.value)
    setRecommendation(null)
    setError('')
    setLoading(false)
  }

  function applyRecommendation() {
    const recommended = recommendation?.recommendedPlannedPublishDate?.slice(0, 16)
    if (!recommended) return
    if (plannedPublishDate && plannedPublishDate !== recommended
        && !window.confirm('להחליף את מועד הפרסום המתוכנן בהמלצה החדשה?')) return
    onApply(recommended)
    setRecommendation(null)
  }

  function cancel() {
    requestRef.current?.abort()
    requestRef.current = null
    setRecommendation(null)
    setError('')
    setLoading(false)
  }

  return (
    <section className="publishing-recommendation" aria-label="המלצת מועד פרסום לפי כללים מקומיים" dir="rtl">
      <div className="publishing-recommendation-heading">
        <div>
          <strong>המלצת מועד פרסום</strong>
          <small>המלצה מבוססת כללים מקומיים · ללא נתוני קהל</small>
        </div>
        <button type="button" className="ghost-button small-button" onClick={generate} disabled={loading || !canGenerate}>
          {loading ? 'יוצר המלצה...' : recommendation ? 'צור המלצה מחדש' : 'המלץ על מועד פרסום'}
        </button>
      </div>
      <label className="publishing-recommendation-keywords">
        מילות מפתח, מופרדות בפסיקים
        <input value={keywords} onChange={handleKeywordsChange} disabled={loading} placeholder="מבצע, מדריך, אירוע" />
      </label>
      {!canGenerate && <p className="publishing-recommendation-hint">יש לבחור לקוח ולהזין כותרת לפני יצירת המלצה.</p>}
      {error && <p className="publishing-recommendation-error" role="alert">{error}</p>}
      {recommendation && (
        <div className="publishing-recommendation-preview" role="status">
          <div>
            <span>המועד המומלץ</span>
            <strong>{formatRecommendedDate(recommendation.recommendedPlannedPublishDate)}</strong>
            <small>אזור זמן: {recommendation.timezone}</small>
          </div>
          <p>{recommendation.rationale}</p>
          <div className="publishing-recommendation-actions">
            <button type="button" className="secondary-button small-button" onClick={applyRecommendation}>החל על תאריך הפרסום</button>
            <button type="button" className="ghost-button small-button" onClick={generate} disabled={loading}>צור המלצה מחדש</button>
            <button type="button" className="ghost-button small-button" onClick={cancel}>בטל</button>
          </div>
        </div>
      )}
    </section>
  )
}

export default PublishingRecommendation
