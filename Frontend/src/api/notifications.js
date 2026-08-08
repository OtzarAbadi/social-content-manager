import api from '../services/api.js'

export const NOTIFICATIONS_UPDATED_EVENT = 'notifications:updated'

export function getNotificationPath(notification) {
  const contentId = notification.relatedContentId ?? notification.contentId
  if (notification.type === 'NEW_ACTIVITY') return '/activity'
  if (!contentId) return null
  if (notification.type === 'COMMENT_ADDED') {
    const commentId = notification.entityId
    return `/content/${contentId}?tab=comments${commentId ? `&highlightId=${commentId}` : ''}`
  }
  return `/content/${contentId}?highlightId=${contentId}`
}

export async function getNotifications() {
  return (await api.get('/notifications')).data
}

export async function getUnreadNotificationCount() {
  return (await api.get('/notifications/unread-count')).data.count || 0
}

export async function markNotificationRead(notificationId) {
  await api.put(`/notifications/${notificationId}/read`)
}

export async function markAllNotificationsRead() {
  await api.put('/notifications/read-all')
}

export function announceNotificationsUpdated() {
  window.dispatchEvent(new Event(NOTIFICATIONS_UPDATED_EVENT))
}
