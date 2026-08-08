import { describe, expect, it } from 'vitest'
import { getNotificationPath } from './notifications.js'

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
