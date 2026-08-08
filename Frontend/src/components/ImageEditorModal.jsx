import { useEffect, useRef, useState } from 'react'
import { DEFAULT_IMAGE_EDITS, exportEditedImage, renderImageToCanvas } from '../utils/imageEditor.js'

const ratios = [['original', 'מקורי'], ['1:1', '1:1'], ['4:5', '4:5'], ['9:16', '9:16']]
const tools = [
  { id: 'crop', label: 'חיתוך', icon: '⌗' },
  { id: 'ratio', label: 'יחס', icon: '▣' },
  { id: 'rotate', label: 'סיבוב', icon: '↻' },
  { id: 'brightness', label: 'בהירות', icon: '☀' },
  { id: 'contrast', label: 'ניגודיות', icon: '◐' },
  { id: 'saturation', label: 'רוויה', icon: '◉' },
  { id: 'warmth', label: 'חום', icon: '♨' },
  { id: 'tint', label: 'גוון', icon: '◒' },
  { id: 'sharpness', label: 'חדות', icon: '✦' },
  { id: 'vignette', label: 'וינייטה', icon: '◍' },
]
const adjustmentConfig = {
  brightness: { label: 'בהירות', min: 50, max: 150, neutral: 100 },
  contrast: { label: 'ניגודיות', min: 50, max: 150, neutral: 100 },
  saturation: { label: 'רוויה', min: 0, max: 200, neutral: 100 },
  warmth: { label: 'חום', min: -100, max: 100, neutral: 0 },
  tint: { label: 'גוון', min: -100, max: 100, neutral: 0 },
  sharpness: { label: 'חדות', min: 0, max: 100, neutral: 0 },
  vignette: { label: 'וינייטה', min: 0, max: 100, neutral: 0 },
}

function ValueSlider({ label, value, min, max, neutral, onChange }) {
  const displayValue = neutral === 100 ? value - 100 : value
  return <div className="image-editor-active-adjustment">
    <div className="image-editor-adjustment-heading"><strong>{label}</strong><output>{displayValue > 0 ? `+${displayValue}` : displayValue}</output>
      <button type="button" className="image-editor-tool-reset" onClick={() => onChange(neutral)}>איפוס</button>
    </div>
    <label className="image-editor-value-slider">
      <span className="sr-only">{label}</span>
      <input type="range" aria-label={label} min={min} max={max} value={value} onChange={(event) => onChange(Number(event.target.value))} />
      <i aria-hidden="true" style={{ insetInlineStart: `${((neutral - min) / (max - min)) * 100}%` }} />
    </label>
  </div>
}

function ImageEditorModal({ file, onCancel, onSave, exportImage = exportEditedImage }) {
  const [edits, setEdits] = useState({ ...DEFAULT_IMAGE_EDITS })
  const [activeTool, setActiveTool] = useState('crop')
  const [showOriginal, setShowOriginal] = useState(false)
  const [image, setImage] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const canvasRef = useRef(null)

  useEffect(() => {
    const url = URL.createObjectURL(file)
    const source = new Image()
    source.onload = () => setImage(source)
    source.onerror = () => setError('לא ניתן לטעון את התמונה לעריכה')
    source.src = url
    return () => URL.revokeObjectURL(url)
  }, [file])

  useEffect(() => {
    if (!image || !canvasRef.current) return
    renderImageToCanvas(canvasRef.current, image, showOriginal ? DEFAULT_IMAGE_EDITS : edits, 900)
  }, [edits, image, showOriginal])

  const update = (field, value) => setEdits((current) => ({ ...current, [field]: value }))
  const selectTool = (tool) => {
    setActiveTool(tool.id)
    if (tool.id === 'rotate') update('rotation', edits.rotation + 90)
    if (tool.id === 'crop' && !edits.cropMode) update('cropMode', true)
  }

  async function save() {
    if (!image) return
    setSaving(true)
    setError('')
    try { onSave(await exportImage(file, image, edits)) } catch (saveError) {
      setError(saveError.message === 'UNSUPPORTED_IMAGE_FORMAT' ? 'פורמט התמונה אינו נתמך לעריכה' : 'לא הצלחנו לשמור את עריכת התמונה')
    } finally { setSaving(false) }
  }

  const activeAdjustment = adjustmentConfig[activeTool]
  return <div className="image-editor-overlay" role="presentation">
    <section className="image-editor-dialog" role="dialog" aria-modal="true" aria-labelledby="image-editor-title" dir="rtl">
      <header className="image-editor-header">
        <button type="button" className="image-editor-header-action" onClick={onCancel}>ביטול</button>
        <h2 id="image-editor-title">עריכת תמונה</h2>
        <button type="button" className="image-editor-header-action image-editor-save" onClick={save} disabled={saving || !image}>{saving ? 'שומר...' : 'סיום'}</button>
      </header>

      <div className="image-editor-preview" aria-label="תצוגה מלאה"><canvas ref={canvasRef} />
        <button type="button" className={`image-editor-original ${showOriginal ? 'active' : ''}`}
          aria-pressed={showOriginal} onClick={() => setShowOriginal((current) => !current)}>
          צפייה במקור
        </button>
      </div>
      {error && <p className="entity-state entity-state-error" role="alert">{error}</p>}

      <div className="image-editor-controls">
        <div className="image-editor-secondary-panel" data-testid="image-editor-secondary-panel">
          {activeTool === 'crop' && <div className="image-editor-crop-panel">
            <ValueSlider label="זום" min={100} max={300} neutral={100} value={Math.round(edits.zoom * 100)} onChange={(value) => update('zoom', value / 100)} />
            <div className="image-editor-position-grid">
              <ValueSlider label="מיקום אופקי" min={-100} max={100} neutral={0} value={Math.round(edits.offsetX * 100)} onChange={(value) => update('offsetX', value / 100)} />
              <ValueSlider label="מיקום אנכי" min={-100} max={100} neutral={0} value={Math.round(edits.offsetY * 100)} onChange={(value) => update('offsetY', value / 100)} />
            </div>
          </div>}
          {activeTool === 'ratio' && <div className="image-editor-ratios" aria-label="יחס תמונה">
            {ratios.map(([value, label]) => <button type="button" aria-pressed={edits.aspectRatio === value} className={edits.aspectRatio === value ? 'active' : ''} key={value} onClick={() => update('aspectRatio', value)}>{label}</button>)}
          </div>}
          {activeTool === 'rotate' && <div className="image-editor-rotate-panel"><span>סיבוב: {((edits.rotation % 360) + 360) % 360}°</span><button type="button" onClick={() => update('rotation', edits.rotation - 90)}>סיבוב שמאלה</button><button type="button" onClick={() => update('rotation', edits.rotation + 90)}>סיבוב ימינה</button></div>}
          {activeAdjustment && <ValueSlider {...activeAdjustment} value={edits[activeTool]} onChange={(value) => update(activeTool, value)} />}
        </div>
        <div className="image-editor-tool-strip" role="toolbar" aria-label="כלי עריכת תמונה">
          {tools.map((tool) => <button type="button" key={tool.id} aria-pressed={activeTool === tool.id}
            className={`image-editor-tool ${activeTool === tool.id ? 'active' : ''}`} onClick={() => selectTool(tool)}>
            <span aria-hidden="true">{tool.icon}</span><small>{tool.label}</small>
          </button>)}
        </div>
      </div>

      <footer className="image-editor-footer"><button type="button" className="ghost-button" onClick={() => setEdits({ ...DEFAULT_IMAGE_EDITS })}>איפוס כל העריכות</button></footer>
    </section>
  </div>
}

export default ImageEditorModal
