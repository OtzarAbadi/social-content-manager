import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import IntegrationsPage from './IntegrationsPage.jsx'
import {
  getInstagramSettings, getIntegrationClients, getIntegrationProfile, getPublishingStatus,
} from '../api/publishing.js'

vi.mock('../api/publishing.js', () => ({
  getInstagramSettings: vi.fn(), getIntegrationClients: vi.fn(),
  getIntegrationProfile: vi.fn(), getPublishingStatus: vi.fn(),
}))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))

describe('IntegrationsPage Instagram settings', () => {
  beforeEach(() => {
    getPublishingStatus.mockResolvedValue({ activeProvider: 'META', pollingIntervalSeconds: 60 })
    getIntegrationProfile.mockResolvedValue({ role: 'ADMIN' })
    getIntegrationClients.mockResolvedValue([{ client_id: 7, business_name: 'Otzar social' }])
    getInstagramSettings.mockResolvedValue({ connected: true, accessTokenConfigured: true, connectionSource: 'SERVER_CONFIGURATION' })
  })
  afterEach(() => { cleanup(); vi.clearAllMocks() })

  it('shows server connection status without technical configuration fields', async () => {
    render(<IntegrationsPage isAuthenticated routes={{}} />)
    fireEvent.change(await screen.findByLabelText('לקוח'), { target: { value: '7' } })
    expect(await screen.findByText('כן — דרך הגדרות השרת')).toBeTruthy()
    expect(getInstagramSettings).toHaveBeenCalledWith('7')
    expect(screen.queryByText(/178900000000002|graph\.facebook/i)).toBeNull()
    expect(screen.queryByRole('textbox')).toBeNull()
  })

  it('shows disconnected status for another selected client', async () => {
    getInstagramSettings.mockRejectedValue({ response: { status: 404 } })
    render(<IntegrationsPage isAuthenticated routes={{}} />)
    fireEvent.change(await screen.findByLabelText('לקוח'), { target: { value: '7' } })
    expect(await screen.findByText('Instagram account is not connected for this client.')).toBeTruthy()
    expect(screen.queryByRole('textbox')).toBeNull()
  })

  it('loads a CLIENT connection without showing a client selector', async () => {
    getIntegrationProfile.mockResolvedValue({ role: 'CLIENT' })
    render(<IntegrationsPage isAuthenticated routes={{}} />)
    expect(await screen.findByText('כן — דרך הגדרות השרת')).toBeTruthy()
    expect(getInstagramSettings).toHaveBeenCalledWith(undefined)
    expect(screen.queryByLabelText('לקוח')).toBeNull()
  })
})
