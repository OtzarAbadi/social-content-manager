import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import DashboardPage from './DashboardPage.jsx'
import api from '../services/api.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}{location.search}</output>
}

function mockDashboardData({ role = 'ADMIN', contents = [], comments = [] } = {}) {
  api.get.mockImplementation((url) => {
    if (url === '/users/me') return Promise.resolve({ data: { role, id: 1, clientId: role === 'CLIENT' ? 1 : null } })
    if (url === '/contents') return Promise.resolve({ data: contents })
    if (url === '/comments') return Promise.resolve({ data: comments })
    return Promise.resolve({ data: [] })
  })
}

describe('Clients Management cards', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks() })

  it('keeps client details without rendering an initial avatar', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/users/me') return Promise.resolve({ data: { role: 'ADMIN', id: 1 } })
      if (url === '/clients') return Promise.resolve({ data: [{ client_id: 7, business_name: 'Otzar social', phone: '050-1234567', user_id: 2 }] })
      if (url === '/users') return Promise.resolve({ data: [{ user_id: 2, full_name: 'Otzar Client' }] })
      if (url === '/users/social-managers') return Promise.resolve({ data: [] })
      return Promise.resolve({ data: [] })
    })

    const { container } = render(
      <MemoryRouter>
        <DashboardPage activeRoute="clients" routes={{}} onNavigate={vi.fn()} isAuthenticated onLogout={vi.fn()} />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Otzar social')).toBeTruthy()
    expect(screen.getByText(/050-1234567/)).toBeTruthy()
    expect(container.querySelector('#clients-title')?.closest('.management-section')?.querySelector('.entity-mark')).toBeNull()
  })
})

describe('Activity Center comment navigation', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks() })

  const contentA = { contentId: 101, clientId: 1, title: 'Content A', status: 'DRAFT' }
  const contentB = { contentId: 202, clientId: 2, title: 'Content B', status: 'DRAFT' }
  const commentA = { commentId: 11, contentId: 101, userId: 1, commentText: 'Comment A' }
  const commentB = { commentId: 22, contentId: 202, userId: 1, commentText: 'Comment B' }

  function renderMessages(initialEntries = ['/messages']) {
    return render(<MemoryRouter initialEntries={initialEntries}>
      <DashboardPage activeRoute="messages" routes={{}} onNavigate={vi.fn()} isAuthenticated onLogout={vi.fn()} />
      <LocationProbe />
    </MemoryRouter>)
  }

  it('opens content A and content B by exact ID without stale selection', async () => {
    mockDashboardData({ contents: [contentA, contentB], comments: [commentA, commentB] })
    renderMessages()

    const firstCard = (await screen.findByText('Comment A')).closest('article')
    fireEvent.click(firstCard)
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
    fireEvent.click(screen.getByText('Comment B').closest('article'))
    expect(screen.getByTestId('location').textContent).toBe('/content/202?highlightId=202')
  })

  it('supports keyboard activation for a comment card', async () => {
    mockDashboardData({ contents: [contentA], comments: [commentA] })
    renderMessages()

    const card = (await screen.findByText('Comment A')).closest('article')
    fireEvent.keyDown(card, { key: 'Enter' })
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
    fireEvent.keyDown(card, { key: ' ' })
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
  })

  it('does not link missing content or content outside a CLIENT scoped response', async () => {
    mockDashboardData({ role: 'CLIENT', contents: [contentA], comments: [
      commentA,
      { commentId: 99, contentId: 999, userId: 1, commentText: 'Foreign comment' },
    ] })
    renderMessages()

    expect((await screen.findByText('Comment A')).closest('article').getAttribute('role')).toBe('link')
    const foreignCard = screen.getByText('Foreign comment').closest('article')
    expect(foreignCard.getAttribute('role')).toBeNull()
    fireEvent.click(foreignCard)
    expect(screen.getByTestId('location').textContent).toBe('/messages')
  })

  it('shows a safe message instead of opening stale content when a deep-linked content is missing', async () => {
    mockDashboardData({ contents: [contentA], comments: [] })
    render(<MemoryRouter initialEntries={['/content/999?highlightId=999']}>
      <DashboardPage activeRoute="content" routes={{}} onNavigate={vi.fn()} isAuthenticated onLogout={vi.fn()} />
    </MemoryRouter>)

    expect(await screen.findByText('לא ניתן לפתוח את התוכן המבוקש. ייתכן שהוא נמחק או שאין לך הרשאה לצפות בו.')).toBeTruthy()
  })
})
