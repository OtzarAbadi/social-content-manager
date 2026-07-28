import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PwaStatus from './PwaStatus.jsx'

const sw = vi.hoisted(() => ({
  needRefresh: false,
  setNeedRefresh: vi.fn(),
  updateServiceWorker: vi.fn(),
}))

vi.mock('virtual:pwa-register/react', () => ({
  useRegisterSW: () => ({
    needRefresh: [sw.needRefresh, sw.setNeedRefresh],
    updateServiceWorker: sw.updateServiceWorker,
  }),
}))

const originalUserAgent = window.navigator.userAgent

function setNavigatorValue(key, value) {
  Object.defineProperty(window.navigator, key, { configurable: true, value })
}

function createInstallEvent() {
  const event = new Event('beforeinstallprompt', { cancelable: true })
  Object.defineProperties(event, {
    prompt: { value: vi.fn().mockResolvedValue(undefined) },
    userChoice: { value: Promise.resolve({ outcome: 'accepted' }) },
  })
  return event
}

describe('PwaStatus', () => {
  afterEach(cleanup)

  beforeEach(() => {
    window.localStorage.clear()
    sw.needRefresh = false
    sw.setNeedRefresh.mockReset()
    sw.updateServiceWorker.mockReset()
    setNavigatorValue('onLine', true)
    setNavigatorValue('userAgent', originalUserAgent)
  })

  it('shows the Android automatic install prompt with the full app name', async () => {
    setNavigatorValue('userAgent', 'Mozilla/5.0 (Linux; Android 15) Chrome/140')
    const event = createInstallEvent()
    render(<PwaStatus />)

    fireEvent(window, event)
    expect(await screen.findByText('התקנת SocialContent')).toBeTruthy()
    expect(screen.getByText('התקינו את המערכת כאפליקציה במכשיר לקבלת גישה מהירה ונוחה.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'התקנת האפליקציה' }))

    await waitFor(() => expect(event.prompt).toHaveBeenCalledOnce())
  })

  it('shows manual Chrome guidance when Android has no automatic prompt', () => {
    setNavigatorValue('userAgent', 'Mozilla/5.0 (Linux; Android 15) Chrome/140')
    render(<PwaStatus />)

    expect(screen.getByText(/ב־Chrome לחצו על תפריט שלוש הנקודות/)).toBeTruthy()
    expect(screen.getByText(/״התקנת האפליקציה״ או ״הוספה למסך הבית״/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'התקנת האפליקציה' })).toBeNull()
  })

  it('shows separate Safari guidance on iPhone', () => {
    setNavigatorValue('userAgent', 'Mozilla/5.0 (iPhone; CPU iPhone OS 19_0) AppleWebKit Safari')
    render(<PwaStatus />)

    expect(screen.getByText(/ב־Safari לחצו על כפתור השיתוף/)).toBeTruthy()
    expect(screen.getByText(/״הוספה למסך הבית״/)).toBeTruthy()
    expect(screen.queryByText(/ב־Chrome/)).toBeNull()
  })

  it('persists guidance dismissal and does not show it again', () => {
    setNavigatorValue('userAgent', 'Mozilla/5.0 (Linux; Android 15) Chrome/140')
    const { unmount } = render(<PwaStatus />)
    fireEvent.click(screen.getByRole('button', { name: 'לא עכשיו' }))
    expect(window.localStorage.getItem('sscm-pwa-install-dismissed')).toBe('true')

    unmount()
    render(<PwaStatus />)
    expect(screen.queryByText('התקנת SocialContent')).toBeNull()
  })

  it('shows the safe offline notice', () => {
    render(<PwaStatus />)
    setNavigatorValue('onLine', false)
    fireEvent(window, new Event('offline'))
    expect(screen.getByText('אין חיבור לאינטרנט. יש להתחבר מחדש כדי לבצע פעולה זו.')).toBeTruthy()
  })

  it('lets the user explicitly apply a waiting update', () => {
    sw.needRefresh = true
    render(<PwaStatus />)
    expect(screen.getByText('גרסה חדשה זמינה')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'עדכון עכשיו' }))
    expect(sw.updateServiceWorker).toHaveBeenCalledWith(true)
  })
})
