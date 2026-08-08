import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotificationsPage from './NotificationsPage.jsx'
import { getNotifications } from '../api/notifications.js'

vi.mock('../api/notifications.js', () => ({
  announceNotificationsUpdated: vi.fn(),
  getNotifications: vi.fn(),
  getNotificationPath: vi.fn(() => '/content/42?highlightId=42'),
  markAllNotificationsRead: vi.fn(),
  markNotificationRead: vi.fn(),
  NOTIFICATIONS_UPDATED_EVENT: 'notifications:updated',
}))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

describe('NotificationsPage type labels', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks() })

  it('shows CONTENT_APPROVED in Hebrew while preserving useful notification content', async () => {
    getNotifications.mockResolvedValue([{
      notificationId: 1,
      type: 'CONTENT_APPROVED',
      title: 'התוכן אושר',
      message: 'הלקוח אישר את התוכן ״קמפיין קיץ״',
      relatedContentId: 42,
      read: true,
      createdAt: '2026-08-08T10:00:00Z',
    }])

    render(<MemoryRouter><NotificationsPage activeRoute="notifications" routes={{}} isAuthenticated /></MemoryRouter>)

    expect(await screen.findByText('התוכן אושר', { selector: '.notifications-page-type' })).toBeTruthy()
    expect(screen.queryByText(/content approved/i)).toBeNull()
    expect(screen.queryByText('CONTENT_APPROVED')).toBeNull()
    expect(screen.getByText('הלקוח אישר את התוכן ״קמפיין קיץ״')).toBeTruthy()
  })
})
