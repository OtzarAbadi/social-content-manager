import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import ActivityPage from './ActivityPage.jsx'
import { getActivity } from '../api/activity.js'
import api from '../services/api.js'

vi.mock('../api/activity.js', () => ({ getActivity: vi.fn() }))
vi.mock('../services/api.js', () => ({ default: { get: vi.fn() } }))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

const activities = [
  { activityId: 'a1', type: 'COMMENT_ADDED', contentId: 101, clientId: 1, clientName: 'Otzar', contentTitle: 'Otzar post', occurredAt: new Date().toISOString(), versionNumber: 1 },
  { activityId: 'a2', type: 'COMMENT_ADDED', contentId: 202, clientId: 2, clientName: 'Second', contentTitle: 'Second post', occurredAt: new Date().toISOString(), versionNumber: 1 },
]

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}{location.search}</output>
}

describe('ActivityPage client filtering', () => {
  afterEach(() => { cleanup(); vi.clearAllMocks() })
  const renderPage = () => render(<MemoryRouter>
    <ActivityPage isAuthenticated routes={{}} onNavigate={vi.fn()} />
    <LocationProbe />
  </MemoryRouter>)

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

  it('opens each activity by its exact content ID without reusing a stale selection', async () => {
    getActivity.mockResolvedValue(activities)
    api.get.mockImplementation((url) => Promise.resolve({ data: url === '/users/me'
      ? { role: 'ADMIN' }
      : [{ client_id: 1, business_name: 'Otzar' }, { client_id: 2, business_name: 'Second' }] }))
    renderPage()

    fireEvent.click((await screen.findByText('Otzar post')).closest('[role="link"]'))
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
    fireEvent.click(screen.getByText('Second post').closest('[role="link"]'))
    expect(screen.getByTestId('location').textContent).toBe('/content/202?highlightId=202')
  })

  it('supports Enter and Space, and leaves activity without a content ID inert', async () => {
    getActivity.mockResolvedValue([
      activities[0],
      { ...activities[1], activityId: 'missing', contentId: null, contentTitle: 'Deleted post' },
    ])
    api.get.mockImplementation((url) => Promise.resolve({ data: url === '/users/me'
      ? { role: 'ADMIN' }
      : [{ client_id: 1, business_name: 'Otzar' }, { client_id: 2, business_name: 'Second' }] }))
    renderPage()

    const linkedCard = (await screen.findByText('Otzar post')).closest('article')
    fireEvent.keyDown(linkedCard, { key: 'Enter' })
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
    fireEvent.keyDown(linkedCard, { key: ' ' })
    expect(screen.getByTestId('location').textContent).toBe('/content/101?highlightId=101')
    expect(screen.getByText('Deleted post').closest('article').getAttribute('role')).toBeNull()
  })
})
