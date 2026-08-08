import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import DashboardPage from './DashboardPage.jsx'
import api from '../services/api.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}{location.search}</output>
}

function mockDashboardData({ role = 'ADMIN', clients = [], contents = [], comments = [] } = {}) {
  api.get.mockImplementation((url) => {
    if (url === '/users/me') return Promise.resolve({ data: { role, id: 1, clientId: role === 'CLIENT' ? 1 : null } })
    if (url === '/clients') return Promise.resolve({ data: clients })
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

describe('Clients Management content filtering navigation', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks(); vi.restoreAllMocks() })

  const clients = [
    { client_id: 1, business_name: 'Client A' },
    { client_id: 2, business_name: 'Client B' },
  ]
  const contents = [
    { contentId: 101, clientId: 1, title: 'Only A', status: 'DRAFT' },
    { contentId: 202, clientId: 2, title: 'Only B', status: 'DRAFT' },
  ]

  function renderDashboard(activeRoute, initialEntry) {
    return render(<MemoryRouter initialEntries={[initialEntry]}>
      <DashboardPage activeRoute={activeRoute} routes={{}} onNavigate={vi.fn()} isAuthenticated onLogout={vi.fn()} />
      <LocationProbe />
    </MemoryRouter>)
  }

  it('navigates Client A and Client B with their stable clientId', async () => {
    mockDashboardData({ clients, contents })
    renderDashboard('clients', '/clients')

    const cardA = (await screen.findByText('Client A')).closest('article')
    const cardB = screen.getByText('Client B').closest('article')
    fireEvent.click(within(cardA).getAllByRole('button')[1])
    expect(screen.getByTestId('location').textContent).toBe('/content?clientId=1')
    fireEvent.click(within(cardB).getAllByRole('button')[1])
    expect(screen.getByTestId('location').textContent).toBe('/content?clientId=2')
  })

  it('restores the query filter on refresh and shows only Client B content', async () => {
    mockDashboardData({ clients, contents })
    const { container } = renderDashboard('content', '/content?clientId=2')

    await waitFor(() => expect(container.querySelector('select[name="clientId"]').value).toBe('2'))
    expect(screen.getByText('Only B')).toBeTruthy()
    expect(screen.queryByText('Only A')).toBeNull()
  })

  it('shows no unrelated content for an invalid clientId and clears stale selection when absent', async () => {
    mockDashboardData({ clients, contents })
    const { unmount } = renderDashboard('content', '/content?clientId=999')
    await screen.findByText(/לא ניתן לסנן לפי הלקוח המבוקש/)
    expect(screen.queryByText('Only A')).toBeNull()
    expect(screen.queryByText('Only B')).toBeNull()
    unmount()

    mockDashboardData({ clients, contents })
    renderDashboard('content', '/content')
    expect(await screen.findByText('Only A')).toBeTruthy()
    expect(screen.getByText('Only B')).toBeTruthy()
  })

  it('does not navigate when edit or delete actions are used', async () => {
    mockDashboardData({ clients, contents })
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderDashboard('clients', '/clients')

    const actions = within((await screen.findByText('Client A')).closest('article')).getAllByRole('button')
    fireEvent.click(actions[2])
    expect(screen.getByTestId('location').textContent).toBe('/clients')
    fireEvent.click(actions[0])
    expect(screen.getByTestId('location').textContent).toBe('/clients')
  })

  it('keeps CLIENT backend scoping and ignores a clientId query override', async () => {
    mockDashboardData({ role: 'CLIENT', clients: [clients[0]], contents: [contents[0]] })
    const { container } = renderDashboard('content', '/content?clientId=2')

    expect(await screen.findByText('Only A')).toBeTruthy()
    expect(screen.queryByText('Only B')).toBeNull()
    expect(container.querySelector('select[name="clientId"]')).toBeNull()
  })

  it('keeps existing manual client filtering behavior', async () => {
    mockDashboardData({ clients, contents })
    const { container } = renderDashboard('content', '/content')
    await screen.findByText('Only A')

    fireEvent.change(container.querySelector('select[name="clientId"]'), { target: { name: 'clientId', value: '1' } })
    expect(screen.getByText('Only A')).toBeTruthy()
    expect(screen.queryByText('Only B')).toBeNull()
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
