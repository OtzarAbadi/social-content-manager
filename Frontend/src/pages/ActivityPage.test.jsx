import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import ActivityPage from './ActivityPage.jsx'
import { getActivity } from '../api/activity.js'
import api from '../services/api.js'

vi.mock('../api/activity.js', () => ({ getActivity: vi.fn() }))
vi.mock('../services/api.js', () => ({ default: { get: vi.fn() } }))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

const activities = [
  { activityId: 'a1', type: 'CONTENT_CREATED', clientId: 1, clientName: 'Otzar', contentTitle: 'Otzar post', occurredAt: new Date().toISOString(), versionNumber: 1 },
  { activityId: 'a2', type: 'CONTENT_CREATED', clientId: 2, clientName: 'Second', contentTitle: 'Second post', occurredAt: new Date().toISOString(), versionNumber: 1 },
]

describe('ActivityPage client filtering', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks() })
  const renderPage = () => render(<ActivityPage isAuthenticated routes={{}} onNavigate={vi.fn()} />)

  it('lets ADMIN filter by stable client ID and restore all activity', async () => {
    getActivity.mockResolvedValue(activities)
    api.get.mockImplementation((url) => Promise.resolve({ data: url === '/users/me'
      ? { role: 'ADMIN' }
      : [{ client_id: 1, business_name: 'Otzar' }, { client_id: 2, business_name: 'Second' }] }))
    renderPage()

    const selector = await screen.findByLabelText('סינון פעילות לפי לקוח')
    await screen.findByText('Otzar post')
    expect(screen.getByText('Second post')).toBeTruthy()
    fireEvent.change(selector, { target: { value: '1' } })
    expect(screen.getByText('Otzar post')).toBeTruthy()
    expect(screen.queryByText('Second post')).toBeNull()
    fireEvent.change(selector, { target: { value: '' } })
    expect(screen.getByText('Second post')).toBeTruthy()
  })

  it('shows the selected-client empty message', async () => {
    getActivity.mockResolvedValue(activities.slice(0, 1))
    api.get.mockImplementation((url) => Promise.resolve({ data: url === '/users/me'
      ? { role: 'ADMIN' }
      : [{ client_id: 2, business_name: 'Second' }] }))
    renderPage()
    fireEvent.change(await screen.findByLabelText('סינון פעילות לפי לקוח'), { target: { value: '2' } })
    expect(screen.getByText('אין פעילות להצגה עבור הלקוח שנבחר')).toBeTruthy()
  })

  it('hides the selector for CLIENT and displays only the backend-scoped payload', async () => {
    getActivity.mockResolvedValue(activities.slice(0, 1))
    api.get.mockResolvedValue({ data: { role: 'CLIENT', clientId: 1 } })
    renderPage()
    await waitFor(() => expect(getActivity).toHaveBeenCalled())
    expect(screen.queryByLabelText('סינון פעילות לפי לקוח')).toBeNull()
    expect(await screen.findByText('Otzar post')).toBeTruthy()
    expect(screen.queryByText('Second post')).toBeNull()
    expect(api.get).not.toHaveBeenCalledWith('/clients')
  })
})
