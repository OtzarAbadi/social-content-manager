import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '../services/api.js'
import { getNotificationPath, getUnreadNotificationCount } from './notifications.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn() } }))

beforeEach(() => vi.clearAllMocks())

describe('getNotificationPath', () => {
  it('uses relatedContentId for comment content and entityId only for the exact comment', () => {
    expect(getNotificationPath({
      type: 'COMMENT_ADDED',
      relatedContentId: 42,
      entityId: 900,
    })).toBe('/content/42?tab=comments&highlightId=900')
  })

  it('does not create a content link without a related content ID', () => {
    expect(getNotificationPath({ type: 'COMMENT_ADDED', entityId: 900 })).toBeNull()
  })

  it('keeps existing non-comment content notification navigation', () => {
    expect(getNotificationPath({ type: 'CONTENT_APPROVED', relatedContentId: 42 }))
      .toBe('/content/42?highlightId=42')
  })
})

describe('notification polling', () => {
  it('suppresses the global toast for background unread-count failures', async () => {
    api.get.mockRejectedValue({ response: { status: 403 } })

    await expect(getUnreadNotificationCount()).rejects.toEqual({ response: { status: 403 } })
    expect(api.get).toHaveBeenCalledWith('/notifications/unread-count', {
      suppressGlobalErrorToast: true,
    })
  })
})
