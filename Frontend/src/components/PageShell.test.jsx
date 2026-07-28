import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import PageShell from './PageShell.jsx'
import api from '../services/api.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn() } }))
vi.mock('./NotificationsMenu.jsx', () => ({ default: () => <button type="button">התראות משתמש</button> }))

const routes = Object.fromEntries([
  'dashboard', 'calendar', 'content', 'clients', 'messages',
  'analytics', 'notifications', 'activity', 'integrations',
].map((key) => [key, { path: `/${key}` }]))

function renderShell(activeRoute = 'dashboard') {
  return render(
    <PageShell
      activeRoute={activeRoute}
      routes={routes}
      onNavigate={vi.fn()}
      isAuthenticated
      onLogout={vi.fn()}
    >
      <p>תוכן העמוד</p>
    </PageShell>,
  )
}

describe('PageShell mobile navigation', () => {
  beforeEach(() => {
    Element.prototype.scrollIntoView = vi.fn()
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('renders every admin route including integrations in one scrollable navigation', async () => {
    api.get.mockResolvedValue({ data: { role: 'ADMIN', fullName: 'מנהלת' } })
    renderShell()
    const navigation = screen.getByRole('navigation', { name: 'ניווט לנייד' })

    await waitFor(() => expect(within(navigation).getByText('אינטגרציות')).toBeTruthy())
    for (const label of ['בית', 'לוח שנה', 'תוכן', 'לקוחות', 'הודעות', 'אנליטיקה', 'התראות', 'פעילות', 'אינטגרציות', 'יציאה']) {
      expect(within(navigation).getByText(label)).toBeTruthy()
    }
    expect(navigation.dataset.scrollable).toBe('horizontal')
  })

  it('scrolls the active mobile route into view after role-aware items render', async () => {
    api.get.mockResolvedValue({ data: { role: 'ADMIN', fullName: 'מנהלת' } })
    renderShell('integrations')

    const navigation = screen.getByRole('navigation', { name: 'ניווט לנייד' })
    const activeItem = await within(navigation).findByText('אינטגרציות')
    await waitFor(() => expect(activeItem.closest('a').scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'center',
    }))
  })

  it('keeps admin-only routes hidden for clients while retaining client routes', async () => {
    api.get.mockResolvedValue({ data: { role: 'CLIENT', fullName: 'לקוח' } })
    renderShell()
    const navigation = screen.getByRole('navigation', { name: 'ניווט לנייד' })

    await within(navigation).findByText('התראות')
    expect(within(navigation).queryByText('לקוחות')).toBeNull()
    expect(within(navigation).queryByText('אנליטיקה')).toBeNull()
    expect(within(navigation).queryByText('אינטגרציות')).toBeNull()
    expect(within(navigation).getByText('התראות')).toBeTruthy()
    expect(within(navigation).getByText('פעילות')).toBeTruthy()
  })
})
