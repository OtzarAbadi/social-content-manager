import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import AnalyticsPage from './AnalyticsPage.jsx'
import {
  formatAnalyticsChartDate as formatChartDate,
  showAnalyticsValue as show,
} from '../utils/analyticsFormat.js'
import {
  getAnalyticsProfile, getInstagramAccountInsights, getInstagramMediaInsights,
} from '../api/analytics.js'

vi.mock('../api/analytics.js', () => ({
  getAnalyticsProfile: vi.fn(),
  getInstagramAccountInsights: vi.fn(),
  getInstagramMediaInsights: vi.fn(),
}))
vi.mock('../components/PageShell.jsx', () => ({ default: ({ children }) => <main>{children}</main> }))
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  LineChart: ({ children }) => <div>{children}</div>, Line: () => null,
  CartesianGrid: () => null, Legend: () => null, Tooltip: () => null, XAxis: () => null, YAxis: () => null,
}))

const account = {
  followersCount: 1234, mediaCount: 20, reach: 74, views: 211, profileViews: null,
  accountsEngaged: 15, totalInteractions: 21, netFollowerChange: 3, engagementRate: 28.37,
  dailyTrend: [{ date: '2026-07-27', reach: 74, views: 211, totalInteractions: 21 }],
}
const item = {
  mediaId: 'm1', caption: 'פוסט אמיתי', mediaType: 'IMAGE', timestamp: '2026-07-27T10:00:00Z',
  reach: 74, views: 211, likes: 10, comments: 2, saved: 3, shares: 1, totalInteractions: 16,
  engagementRate: 21.62, permalink: 'https://instagram.com/p/example',
}
const media = { items: [item], topByReach: item, topByViews: item, topByEngagement: item }

describe('AnalyticsPage', () => {
  beforeEach(() => {
    getAnalyticsProfile.mockResolvedValue({ role: 'ADMIN' })
    getInstagramAccountInsights.mockResolvedValue(account)
    getInstagramMediaInsights.mockResolvedValue(media)
  })
  afterEach(() => { cleanup(); vi.clearAllMocks() })
  const renderPage = () => render(<AnalyticsPage isAuthenticated routes={{}} />)

  it('formats chart dates for Hebrew readers without raw ISO timestamps', () => {
    expect(formatChartDate('2026-07-22')).toBe('22.07')
    expect(formatChartDate('2026-07-22', true)).toContain('22')
    expect(formatChartDate('2026-07-22', true)).not.toContain('2026-07-22')
  })

  it('distinguishes unavailable values from an explicit zero', () => {
    expect(show(null)).toBe('לא זמין')
    expect(show(0)).toBe('0')
    expect(show(0, true)).toBe('0%')
  })

  it('loads real response values and Hebrew cards', async () => {
    renderPage()
    expect(await screen.findByText('1,234')).toBeTruthy()
    expect(screen.getByText('עוקבים')).toBeTruthy()
    expect(screen.getAllByText('211').length).toBeGreaterThan(0)
  })
  it('shows unavailable instead of inventing zero', async () => {
    renderPage()
    await screen.findByText('1,234')
    expect(screen.getAllByText('אין נתונים זמינים לתקופה שנבחרה.').length).toBeGreaterThan(0)
  })
  it('updates requests when media filter changes', async () => {
    renderPage(); await screen.findByText('1,234')
    fireEvent.change(screen.getByLabelText('סוג תוכן'), { target: { value: 'IMAGE' } })
    await waitFor(() => expect(getInstagramMediaInsights).toHaveBeenLastCalledWith(expect.objectContaining({ mediaType: 'IMAGE' })))
  })
  it('renders loading and media performance table', async () => {
    let resolve
    getInstagramAccountInsights.mockReturnValue(new Promise(r => { resolve = r }))
    renderPage()
    expect(await screen.findByText('טוען נתוני Instagram Insights...')).toBeTruthy()
    resolve(account)
    expect((await screen.findAllByText('פוסט אמיתי')).length).toBeGreaterThan(0)
    expect(screen.getByLabelText('צפייה בפוסט באינסטגרם').getAttribute('rel')).toBe('noopener noreferrer')
  })
  it('renders permission error and retry for temporary failures', async () => {
    getInstagramAccountInsights.mockRejectedValue({ response: { data: { code: 'MISSING_PERMISSION' }, status: 403 } })
    const view = renderPage()
    expect(await screen.findByText(/instagram_manage_insights/)).toBeTruthy()
    cleanup()
    getInstagramAccountInsights.mockRejectedValue(new Error('network'))
    view.unmount()
    renderPage()
    expect(await screen.findByRole('button', { name: 'ניסיון נוסף' })).toBeTruthy()
  })
  it('shows the safe backend configuration error instead of an empty state', async () => {
    getInstagramAccountInsights.mockRejectedValue({
      response: {
        status: 503,
        data: {
          code: 'NOT_CONFIGURED',
          message: 'Instagram Insights configuration is missing or invalid: Meta access token',
        },
      },
    })
    renderPage()
    expect(await screen.findByText(/Meta access token/)).toBeTruthy()
    expect(screen.queryByText('אין נתוני חשבון זמינים.')).toBeNull()
  })
  it('blocks CLIENT users without requesting insights', async () => {
    getAnalyticsProfile.mockResolvedValue({ role: 'CLIENT' })
    renderPage()
    expect(await screen.findByText('הגישה לאנליטיקה זמינה למנהלים בלבד.')).toBeTruthy()
    expect(getInstagramAccountInsights).not.toHaveBeenCalled()
  })
})
