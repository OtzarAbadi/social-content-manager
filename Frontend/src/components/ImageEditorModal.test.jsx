import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import ImageEditorModal from './ImageEditorModal.jsx'

const context = {
  save: vi.fn(), restore: vi.fn(), fillRect: vi.fn(), translate: vi.fn(), rotate: vi.fn(),
  scale: vi.fn(), drawImage: vi.fn(), filter: '', fillStyle: '',
  getImageData: vi.fn((_x, _y, width, height) => ({ data: new Uint8ClampedArray(width * height * 4) })),
  putImageData: vi.fn(),
}

describe('ImageEditorModal', () => {
  beforeEach(() => {
    vi.stubGlobal('Image', class {
      naturalWidth = 1200
      naturalHeight = 800
      set src(_value) { Promise.resolve().then(() => this.onload?.()) }
    })
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context)
  })

  afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals() })

  it('renders a horizontal tool strip and switches between one active adjustment at a time', async () => {
    render(<ImageEditorModal file={new File(['image'], 'source.jpg', { type: 'image/jpeg' })} onCancel={vi.fn()} onSave={vi.fn()} />)
    const toolbar = screen.getByRole('toolbar', { name: 'כלי עריכת תמונה' })
    expect(within(toolbar).getAllByRole('button')).toHaveLength(10)
    await waitFor(() => expect(screen.getByRole('button', { name: 'סיום' }).disabled).toBe(false))

    for (const [tool, value] of [['בהירות', '120'], ['ניגודיות', '80'], ['רוויה', '140'], ['חום', '35'], ['גוון', '-20'], ['חדות', '45'], ['וינייטה', '60']]) {
      fireEvent.click(within(toolbar).getByRole('button', { name: tool }))
      const slider = screen.getByRole('slider', { name: tool })
      fireEvent.change(slider, { target: { value } })
      expect(slider.value).toBe(value)
    }
    expect(screen.queryByRole('slider', { name: 'בהירות' })).toBeNull()
  })

  it('supports crop positioning, zoom, ratios, rotation, and reset', async () => {
    render(<ImageEditorModal file={new File(['image'], 'source.png', { type: 'image/png' })} onCancel={vi.fn()} onSave={vi.fn()} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'סיום' }).disabled).toBe(false))
    expect(screen.getByRole('slider', { name: 'זום' })).toBeTruthy()
    fireEvent.change(screen.getByRole('slider', { name: 'זום' }), { target: { value: '160' } })
    fireEvent.click(screen.getByRole('button', { name: 'יחס' }))
    fireEvent.click(screen.getByRole('button', { name: '4:5' }))
    expect(screen.getByRole('button', { name: '4:5' }).getAttribute('aria-pressed')).toBe('true')
    fireEvent.click(screen.getByRole('button', { name: 'סיבוב' }))
    expect(screen.getByText('סיבוב: 90°')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'איפוס כל העריכות' }))
    fireEvent.click(screen.getByRole('button', { name: 'יחס' }))
    expect(screen.getByRole('button', { name: 'מקורי' }).getAttribute('aria-pressed')).toBe('true')
  })

  it('keeps crop, ratio, and slider panels separate above the complete tool strip', async () => {
    render(<ImageEditorModal file={new File(['image'], 'source.png', { type: 'image/png' })} onCancel={vi.fn()} onSave={vi.fn()} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'סיום' }).disabled).toBe(false))
    const secondaryPanel = screen.getByTestId('image-editor-secondary-panel')
    const toolbar = screen.getByRole('toolbar', { name: 'כלי עריכת תמונה' })
    const expectedTools = ['חיתוך', 'יחס', 'סיבוב', 'בהירות', 'ניגודיות', 'רוויה', 'חום', 'גוון', 'חדות', 'וינייטה']

    expect(secondaryPanel.nextElementSibling).toBe(toolbar)
    expect(secondaryPanel.contains(toolbar)).toBe(false)
    expect(secondaryPanel.querySelector('.image-editor-crop-panel')).toBeTruthy()
    expect(screen.getByRole('slider', { name: 'מיקום אופקי' }).closest('.image-editor-secondary-panel')).toBe(secondaryPanel)
    expectedTools.forEach((name) => expect(within(toolbar).getByRole('button', { name })).toBeTruthy())

    fireEvent.click(within(toolbar).getByRole('button', { name: 'יחס' }))
    expect(screen.getByLabelText('יחס תמונה').closest('.image-editor-secondary-panel')).toBe(secondaryPanel)
    expectedTools.forEach((name) => expect(within(toolbar).getByRole('button', { name })).toBeTruthy())

    fireEvent.click(within(toolbar).getByRole('button', { name: 'בהירות' }))
    expect(screen.getByRole('slider', { name: 'בהירות' }).closest('.image-editor-secondary-panel')).toBe(secondaryPanel)
    expect(document.querySelector('.image-editor-footer')).toBeTruthy()
  })

  it('toggles the original preview without changing edits', async () => {
    render(<ImageEditorModal file={new File(['image'], 'source.jpg', { type: 'image/jpeg' })} onCancel={vi.fn()} onSave={vi.fn()} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'סיום' }).disabled).toBe(false))
    const original = screen.getByRole('button', { name: 'צפייה במקור' })
    fireEvent.click(original)
    expect(original.getAttribute('aria-pressed')).toBe('true')
    fireEvent.click(original)
    expect(original.getAttribute('aria-pressed')).toBe('false')
  })

  it('cancel leaves media untouched and save exports every edited value', async () => {
    const exportImage = vi.fn().mockResolvedValue(new File(['edited'], 'edited.jpg', { type: 'image/jpeg' }))
    const onCancel = vi.fn()
    const onSave = vi.fn()
    render(<ImageEditorModal file={new File(['image'], 'source.jpg', { type: 'image/jpeg' })} onCancel={onCancel} onSave={onSave} exportImage={exportImage} />)
    await waitFor(() => expect(screen.getByRole('button', { name: 'סיום' }).disabled).toBe(false))
    fireEvent.click(screen.getByRole('button', { name: 'חום' }))
    fireEvent.change(screen.getByRole('slider', { name: 'חום' }), { target: { value: '30' } })
    fireEvent.click(screen.getByRole('button', { name: 'חדות' }))
    fireEvent.change(screen.getByRole('slider', { name: 'חדות' }), { target: { value: '25' } })
    fireEvent.click(screen.getByRole('button', { name: 'סיום' }))
    await waitFor(() => expect(onSave).toHaveBeenCalled())
    expect(exportImage.mock.calls[0][2]).toEqual(expect.objectContaining({ warmth: 30, sharpness: 25 }))

    cleanup()
    render(<ImageEditorModal file={new File(['image'], 'source.jpg', { type: 'image/jpeg' })} onCancel={onCancel} onSave={onSave} exportImage={exportImage} />)
    fireEvent.click(screen.getByRole('button', { name: 'ביטול' }))
    expect(onCancel).toHaveBeenCalledTimes(1)
  })
})
