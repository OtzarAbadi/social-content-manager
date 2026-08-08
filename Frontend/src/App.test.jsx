import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App.jsx'
import api from './services/api.js'

vi.mock('./services/api.js', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
vi.mock('./components/PwaStatus.jsx', () => ({ default: () => null }))
vi.mock('./components/GlobalToast.jsx', () => ({ default: () => null }))
vi.mock('./pages/DashboardPage.jsx', () => ({ default: ({ activeRoute }) => <><h1>Dashboard page</h1><output data-testid="active-route">{activeRoute}</output></> }))
vi.mock('./pages/IntegrationsPage.jsx', () => ({ default: () => <h1>Integrations page</h1> }))

describe('Integrations route permissions', () => {
  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('redirects a CLIENT who opens the integrations route directly', async () => {
    api.get.mockResolvedValue({ data: { role: 'CLIENT' } })
    render(<MemoryRouter initialEntries={['/integrations']}><App /></MemoryRouter>)

    expect(await screen.findByText('Dashboard page')).toBeTruthy()
    expect(screen.queryByText('Integrations page')).toBeNull()
  })

  it('allows an ADMIN to open the integrations route directly', async () => {
    api.get.mockResolvedValue({ data: { role: 'ADMIN' } })
    render(<MemoryRouter initialEntries={['/integrations']}><App /></MemoryRouter>)

    expect(await screen.findByText('Integrations page')).toBeTruthy()
  })
})

describe('Messages route permissions', () => {
  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('redirects a CLIENT who opens the messages route directly', async () => {
    api.get.mockResolvedValue({ data: { role: 'CLIENT' } })
    render(<MemoryRouter initialEntries={['/messages']}><App /></MemoryRouter>)

    await waitFor(() => expect(screen.getByTestId('active-route').textContent).toBe('dashboard'))
  })

  it('redirects a CLIENT comment deep link that resolves to messages', async () => {
    api.get.mockResolvedValue({ data: { role: 'CLIENT' } })
    render(<MemoryRouter initialEntries={['/content/42?tab=comments&highlightId=7']}><App /></MemoryRouter>)

    await waitFor(() => expect(screen.getByTestId('active-route').textContent).toBe('dashboard'))
  })

  it('allows an ADMIN to open the messages route directly', async () => {
    api.get.mockResolvedValue({ data: { role: 'ADMIN' } })
    render(<MemoryRouter initialEntries={['/messages']}><App /></MemoryRouter>)

    expect((await screen.findByTestId('active-route')).textContent).toBe('messages')
  })
})
