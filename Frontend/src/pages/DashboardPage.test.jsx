import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from './DashboardPage.jsx'
import api from '../services/api.js'

vi.mock('../services/api.js', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

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
