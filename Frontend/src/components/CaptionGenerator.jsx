import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { generateCaptionSuggestion } from '../api/captionSuggestions.js'

const toneOptions = [
  { value: 'PROFESSIONAL', label: 'מקצועי' },
  { value: 'FRIENDLY', label: 'ידידותי' },
  { value: 'PROMOTIONAL', label: 'שיווקי' },
]

function errorMessage(error) {
  const status = error?.response?.status
  if (status === 400) return 'יש לבדוק את הכותרת ומילות המפתח ולנסות שוב.'
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש.'
  if (status === 403) return 'אין לך הרשאה ליצור הצעת כיתוב.'
  return 'לא הצלחנו ליצור הצעת כיתוב. אפשר לנסות שוב.'
}

function CaptionGenerator({ title, contentType, description, onApply }) {
  const [tone, setTone] = useState('FRIENDLY')
  const [keywords, setKeywords] = useState('')
  const [suggestion, setSuggestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const requestRef = useRef(null)

  useEffect(() => () => requestRef.current?.abort(), [])

  async function generate() {
    requestRef.current?.abort()
    const controller = new AbortController()
    requestRef.current = controller
    setLoading(true)
    setError('')

    try {
      const response = await generateCaptionSuggestion({
        title: title.trim(),
        contentType: contentType || null,
        tone,
        keywords: keywords.split(',').map((item) => item.trim()).filter(Boolean),
        language: 'HE',
      }, controller.signal)
      if (requestRef.current === controller) setSuggestion(response.caption || '')
    } catch (requestError) {
      if (!axios.isCancel(requestError) && requestRef.current === controller) {
        setError(errorMessage(requestError))
      }
    } finally {
      if (requestRef.current === controller) {
        requestRef.current = null
        setLoading(false)
      }
    }
  }

  function applySuggestion() {
    if (description?.trim() && !window.confirm('להחליף את התיאור הקיים בהצעת הכיתוב?')) return
    onApply(suggestion)
  }

  function cancel() {
    requestRef.current?.abort()
    requestRef.current = null
    setSuggestion('')
    setError('')
    setLoading(false)
  }

  return (
    <section className="caption-generator" aria-label="מחולל הצעת כיתוב מקומי" dir="rtl">
      <div className="caption-generator-heading">
        <div><strong>הצעת כיתוב</strong><small>סימולטור מקומי — ללא שירות AI חיצוני</small></div>
        <button type="button" className="ghost-button small-button" onClick={generate} disabled={loading || !title?.trim()}>
          {loading ? 'יוצר הצעה...' : suggestion ? 'צור מחדש' : 'יצירת הצעת כיתוב'}
        </button>
      </div>
      <div className="caption-generator-options">
        <label>סגנון
          <select value={tone} onChange={(event) => setTone(event.target.value)} disabled={loading}>
            {toneOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
        <label>מילות מפתח, מופרדות בפסיקים
          <input value={keywords} onChange={(event) => setKeywords(event.target.value)} disabled={loading} placeholder="קיץ, טיפוח, מבצע" />
        </label>
      </div>
      {!title?.trim() && <p className="caption-generator-hint">יש להזין כותרת לפני יצירת הצעה.</p>}
      {error && <p className="caption-generator-error" role="alert">{error}</p>}
      {suggestion && (
        <div className="caption-suggestion-preview">
          <p>{suggestion}</p>
          <div>
            <button type="button" className="secondary-button small-button" onClick={applySuggestion}>השתמש בכיתוב</button>
            <button type="button" className="ghost-button small-button" onClick={cancel}>בטל</button>
          </div>
        </div>
      )}
    </section>
  )
}

export default CaptionGenerator
