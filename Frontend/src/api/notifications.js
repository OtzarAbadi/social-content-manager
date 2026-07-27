import api from '../services/api.js'

export const NOTIFICATIONS_UPDATED_EVENT = 'notifications:updated'

export function getNotificationPath(notification) {
  const contentId = notification.relatedContentId ?? notification.contentId
  const entityId = notification.entityId ?? contentId
  if (notification.type === 'NEW_ACTIVITY') return '/activity'
  if (!contentId) return null
  if (notification.type === 'COMMENT_ADDED') {
    return `/content/${contentId}?tab=comments&highlightId=${entityId}`
  }
  if (notification.type === 'CONTENT_APPROVED' || notification.type === 'CONTENT_REJECTED') {
    return `/content/${contentId}?highlightId=${entityId}`
  }
  return null
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
