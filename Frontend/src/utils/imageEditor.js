export const DEFAULT_IMAGE_EDITS = Object.freeze({
  aspectRatio: 'original',
  cropMode: false,
  zoom: 1,
  rotation: 0,
  offsetX: 0,
  offsetY: 0,
  brightness: 100,
  contrast: 100,
  saturation: 100,
  warmth: 0,
  tint: 0,
  sharpness: 0,
  vignette: 0,
})

export const EDITABLE_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export function isEditableImage(file) {
  return Boolean(file && EDITABLE_IMAGE_TYPES.includes(file.type))
}

export function getAspectRatio(preset, image) {
  if (preset === '1:1') return 1
  if (preset === '4:5') return 4 / 5
  if (preset === '9:16') return 9 / 16
  return image.naturalWidth / image.naturalHeight
}

export function getOutputDimensions(image, edits, maxDimension = 1600) {
  let ratio = getAspectRatio(edits.aspectRatio, image)
  if (edits.aspectRatio === 'original' && Math.abs(edits.rotation % 180) === 90) ratio = 1 / ratio
  return ratio >= 1
    ? { width: maxDimension, height: Math.max(1, Math.round(maxDimension / ratio)) }
    : { width: Math.max(1, Math.round(maxDimension * ratio)), height: maxDimension }
}

export function renderImageToCanvas(canvas, image, edits, maxDimension = 1600) {
  const { width, height } = getOutputDimensions(image, edits, maxDimension)
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('CANVAS_UNAVAILABLE')

  const quarterTurn = Math.abs(edits.rotation % 180) === 90
  const rotatedWidth = quarterTurn ? image.naturalHeight : image.naturalWidth
  const rotatedHeight = quarterTurn ? image.naturalWidth : image.naturalHeight
  const baseScale = Math.max(width / rotatedWidth, height / rotatedHeight)
  const scale = baseScale * edits.zoom
  const overflowX = Math.max(0, rotatedWidth * scale - width)
  const overflowY = Math.max(0, rotatedHeight * scale - height)

  context.save()
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.translate(width / 2 - edits.offsetX * overflowX / 2, height / 2 - edits.offsetY * overflowY / 2)
  context.rotate(edits.rotation * Math.PI / 180)
  context.scale(scale, scale)
  context.filter = `brightness(${edits.brightness}%) contrast(${edits.contrast}%) saturate(${edits.saturation}%)`
  context.drawImage(image, -image.naturalWidth / 2, -image.naturalHeight / 2)
  context.restore()
  applyPixelAdjustments(context, width, height, edits)
  return canvas
}

function sharpenPixels(source, width, height, amount) {
  if (amount <= 0) return source
  const output = new Uint8ClampedArray(source)
  const strength = amount / 100
  const row = width * 4
  for (let y = 1; y < height - 1; y += 1) {
    for (let x = 1; x < width - 1; x += 1) {
      const index = y * row + x * 4
      for (let channel = 0; channel < 3; channel += 1) {
        const center = source[index + channel]
        const neighbors = source[index - row + channel] + source[index + row + channel]
          + source[index - 4 + channel] + source[index + 4 + channel]
        output[index + channel] = center + strength * (center * 4 - neighbors)
      }
    }
  }
  return output
}

export function applyPixelAdjustments(context, width, height, edits) {
  const warmth = edits.warmth || 0
  const tint = edits.tint || 0
  const sharpness = edits.sharpness || 0
  const vignette = edits.vignette || 0
  if (!warmth && !tint && !sharpness && !vignette) return

  const imageData = context.getImageData(0, 0, width, height)
  const pixels = sharpenPixels(imageData.data, width, height, sharpness)
  const centerX = width / 2
  const centerY = height / 2
  const maximumDistance = Math.hypot(centerX, centerY)
  for (let index = 0; index < pixels.length; index += 4) {
    const pixel = index / 4
    const x = pixel % width
    const y = Math.floor(pixel / width)
    pixels[index] += warmth * 0.55 + tint * 0.18
    pixels[index + 1] -= Math.abs(tint) * 0.08
    pixels[index + 2] -= warmth * 0.55 - tint * 0.42
    if (vignette > 0) {
      const distance = Math.hypot(x - centerX, y - centerY) / maximumDistance
      const shade = 1 - Math.max(0, (distance - 0.25) / 0.75) ** 2 * (vignette / 100) * 0.8
      pixels[index] *= shade
      pixels[index + 1] *= shade
      pixels[index + 2] *= shade
    }
  }
  imageData.data.set(pixels)
  context.putImageData(imageData, 0, 0)
}

export function canvasToBlob(canvas, type, quality = 0.92) {
  return new Promise((resolve, reject) => canvas.toBlob(
    (blob) => blob ? resolve(blob) : reject(new Error('IMAGE_EXPORT_FAILED')),
    type,
    quality,
  ))
}

export async function exportEditedImage(file, image, edits) {
  if (!isEditableImage(file)) throw new Error('UNSUPPORTED_IMAGE_FORMAT')
  const canvas = document.createElement('canvas')
  renderImageToCanvas(canvas, image, edits)
  const blob = await canvasToBlob(canvas, file.type)
  const extension = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.')) : '.jpg'
  const baseName = file.name.includes('.') ? file.name.slice(0, file.name.lastIndexOf('.')) : file.name
  return new File([blob], `${baseName}-edited${extension}`, { type: file.type, lastModified: Date.now() })
}
