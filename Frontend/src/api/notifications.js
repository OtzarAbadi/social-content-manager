import api from './client.js'

export const NOTIFICATIONS_UPDATED_EVENT = 'notifications:updated'

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
