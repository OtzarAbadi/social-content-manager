import { describe, expect, it, vi } from 'vitest'
import { applyPixelAdjustments, DEFAULT_IMAGE_EDITS, getOutputDimensions, isEditableImage, renderImageToCanvas } from './imageEditor.js'

describe('image editor Canvas utilities', () => {
  it('accepts maintained static image formats and rejects video/GIF', () => {
    expect(isEditableImage({ type: 'image/jpeg' })).toBe(true)
    expect(isEditableImage({ type: 'image/png' })).toBe(true)
    expect(isEditableImage({ type: 'image/webp' })).toBe(true)
    expect(isEditableImage({ type: 'video/mp4' })).toBe(false)
    expect(isEditableImage({ type: 'image/gif' })).toBe(false)
  })

  it('keeps aspect ratio without stretching and applies Canvas filters', () => {
    const image = { naturalWidth: 1200, naturalHeight: 800 }
    expect(getOutputDimensions(image, { ...DEFAULT_IMAGE_EDITS, aspectRatio: '1:1' }, 1000)).toEqual({ width: 1000, height: 1000 })
    expect(getOutputDimensions(image, { ...DEFAULT_IMAGE_EDITS, aspectRatio: '4:5' }, 1000)).toEqual({ width: 800, height: 1000 })
    const context = { save: vi.fn(), restore: vi.fn(), fillRect: vi.fn(), translate: vi.fn(), rotate: vi.fn(), scale: vi.fn(), drawImage: vi.fn() }
    const canvas = { getContext: () => context }
    renderImageToCanvas(canvas, image, { ...DEFAULT_IMAGE_EDITS, brightness: 120, contrast: 80, saturation: 140 }, 1000)
    expect(context.filter).toBe('brightness(120%) contrast(80%) saturate(140%)')
    expect(context.drawImage).toHaveBeenCalled()
  })

  it('bakes warmth, tint, sharpness, and vignette into exported Canvas pixels', () => {
    const pixels = new Uint8ClampedArray(3 * 3 * 4).fill(120)
    const imageData = { data: pixels }
    const context = { getImageData: vi.fn(() => imageData), putImageData: vi.fn() }
    applyPixelAdjustments(context, 3, 3, { warmth: 40, tint: 20, sharpness: 30, vignette: 50 })
    expect(context.getImageData).toHaveBeenCalledWith(0, 0, 3, 3)
    expect(context.putImageData).toHaveBeenCalledWith(imageData, 0, 0)
    expect([...pixels]).not.toEqual(new Array(pixels.length).fill(120))
  })
})
