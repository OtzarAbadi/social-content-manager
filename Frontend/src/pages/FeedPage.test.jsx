import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PendingFeed } from './FeedPage.jsx'
import api from '../services/api.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn(), put: vi.fn() } }))

const imageContent = {
  content_id: 11, title: 'תמונת מוצר', description: 'תיאור מלא', status: 'WAITING_APPROVAL',
  file_url: 'https://example.com/photo.jpg', content_type: 'IMAGE',
}
const videoContent = {
  content_id: 12, title: 'סרטון מוצר', status: 'WAITING_APPROVAL',
  file_url: 'https://example.com/video.mp4', content_type: 'VIDEO',
}
const mixedContent = {
  content_id: 13, title: 'קרוסלה מעורבת', status: 'WAITING_APPROVAL',
  media: [
    { mediaUrl: 'https://example.com/first.jpg', mediaType: 'IMAGE', displayOrder: 0 },
    { mediaUrl: 'https://example.com/second.mp4', mediaType: 'VIDEO', displayOrder: 1 },
  ],
}

function load(contents) {
  api.get.mockImplementation((url) => Promise.resolve({ data: url.includes('/comments/') ? [] : contents }))
  return render(<PendingFeed profile={{ role: 'CLIENT' }} />)
}

describe('pending approval feed', () => {
  beforeEach(() => { api.put.mockResolvedValue({ data: {} }) })
  afterEach(() => { cleanup(); vi.clearAllMocks() })

  it('renders the empty state', async () => {
    load([])
    expect(await screen.findByText('אין כרגע תכנים שממתינים לאישור')).toBeTruthy()
  })

  it('defensively renders only WAITING_APPROVAL content', async () => {
    load([imageContent, { ...videoContent, status: 'APPROVED' }])
    expect(await screen.findByLabelText('פתיחת תמונת מוצר')).toBeTruthy()
    expect(screen.queryByLabelText('פתיחת סרטון מוצר')).toBeNull()
  })

  it('uses the media-only Instagram-style grid and keeps loading squares', async () => {
    let resolveRequest
    api.get.mockReturnValue(new Promise((resolve) => { resolveRequest = resolve }))
    const { container } = render(<PendingFeed profile={{ role: 'CLIENT' }} />)
    expect(await screen.findByRole('status', { name: 'טוען פיד' })).toBeTruthy()
    expect(container.querySelectorAll('.feed-skeleton > span')).toHaveLength(6)
    resolveRequest({ data: [imageContent] })
    await screen.findByLabelText('פתיחת תמונת מוצר')
    expect(container.querySelector('.feed-grid')).toBeTruthy()
    expect(container.querySelector('.feed-tile-hover')).toBeNull()
  })

  it('renders image, video, and carousel indicator', async () => {
    const { container } = load([imageContent, videoContent, mixedContent])
    await screen.findByLabelText('פתיחת קרוסלה מעורבת')
    expect(container.querySelectorAll('.feed-tile img')).toHaveLength(2)
    expect(container.querySelectorAll('.feed-tile video')).toHaveLength(1)
    expect(screen.getByLabelText('מספר פריטי מדיה')).toBeTruthy()
    expect(screen.getByLabelText('וידאו')).toBeTruthy()
  })

  it('orders pending content newest first', async () => {
    load([
      { ...imageContent, createdAt: '2026-08-01T10:00:00' },
      { ...videoContent, createdAt: '2026-08-02T10:00:00' },
    ])
    await screen.findByLabelText('פתיחת סרטון מוצר')
    const labels = [...document.querySelectorAll('.feed-tile')].map((tile) => tile.getAttribute('aria-label'))
    expect(labels).toEqual(['פתיחת סרטון מוצר', 'פתיחת תמונת מוצר'])
  })

  it('opens details and navigates a mixed carousel', async () => {
    load([mixedContent])
    fireEvent.click(await screen.findByLabelText('פתיחת קרוסלה מעורבת'))
    const dialog = await screen.findByRole('dialog', { name: 'קרוסלה מעורבת' })
    expect(within(dialog).getByText('1 / 2')).toBeTruthy()
    const carouselButtons = dialog.querySelectorAll('.content-media-carousel-controls button')
    fireEvent.click(carouselButtons[1])
    expect(within(dialog).getByText('2 / 2')).toBeTruthy()
  })

  it('does not make a no-media item clickable or open unrelated content', async () => {
    load([{ content_id: 20, title: 'ללא קובץ', status: 'WAITING_APPROVAL' }, imageContent])
    const placeholder = await screen.findByRole('img', { name: 'ללא קובץ: ללא מדיה' })
    expect(placeholder.tagName).toBe('DIV')
    fireEvent.click(placeholder)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.queryByLabelText('פתיחת ללא קובץ')).toBeNull()
  })

  it('opens content A and content B strictly by their stable IDs', async () => {
    load([imageContent, videoContent])
    fireEvent.click(await screen.findByLabelText('פתיחת תמונת מוצר'))
    expect(await screen.findByRole('dialog', { name: 'תמונת מוצר' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'סגירת פרטי התוכן' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    fireEvent.click(screen.getByLabelText('פתיחת סרטון מוצר'))
    expect(await screen.findByRole('dialog', { name: 'סרטון מוצר' })).toBeTruthy()
  })

  it('clears selection when the detail modal closes', async () => {
    load([imageContent])
    fireEvent.click(await screen.findByLabelText('פתיחת תמונת מוצר'))
    fireEvent.click(await screen.findByRole('button', { name: 'סגירת פרטי התוכן' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('approves with the existing API and removes the item', async () => {
    load([imageContent])
    fireEvent.click(await screen.findByLabelText('פתיחת תמונת מוצר'))
    fireEvent.click(await screen.findByRole('button', { name: 'אישור' }))
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/contents/11/approve'))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await screen.findByText('אין כרגע תכנים שממתינים לאישור')).toBeTruthy()
  })

  it('rejects with the existing reason flow and removes the item', async () => {
    load([imageContent])
    fireEvent.click(await screen.findByLabelText('פתיחת תמונת מוצר'))
    fireEvent.click(await screen.findByRole('button', { name: 'דחייה' }))
    fireEvent.change(await screen.findByLabelText('סיבת הדחייה'), { target: { value: 'נדרש תיקון' } })
    fireEvent.click(screen.getByRole('button', { name: 'דחיית התוכן' }))
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/contents/11/reject', { reason: 'נדרש תיקון' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('clears stale selection when the feed reloads', async () => {
    api.get.mockImplementation((url) => Promise.resolve({ data: url.includes('/comments/') ? [] : [imageContent] }))
    const { rerender } = render(<PendingFeed profile={{ role: 'CLIENT' }} reloadKey={0} />)
    fireEvent.click(await screen.findByLabelText('פתיחת תמונת מוצר'))
    expect(await screen.findByRole('dialog', { name: 'תמונת מוצר' })).toBeTruthy()
    rerender(<PendingFeed profile={{ role: 'CLIENT' }} reloadKey={1} />)
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })
})
