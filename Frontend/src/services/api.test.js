import { afterEach, describe, expect, it, vi } from 'vitest'
import api from './api.js'

function forbiddenAdapter(config) {
  return Promise.reject({ config, response: { status: 403 } })
}

describe('global API authorization feedback', () => {
  afterEach(() => vi.restoreAllMocks())

  it('does not dispatch a toast for a suppressed startup request', async () => {
    const listener = vi.fn()
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    window.addEventListener('sscm:api-error', listener)

    await expect(api.get('/startup-check', {
      suppressGlobalErrorToast: true,
      adapter: forbiddenAdapter,
    })).rejects.toBeTruthy()

    expect(listener).not.toHaveBeenCalled()
    expect(consoleError).toHaveBeenCalled()
    window.removeEventListener('sscm:api-error', listener)
  })

  it('still dispatches the authorization toast for a forbidden user action', async () => {
    const listener = vi.fn()
    vi.spyOn(console, 'error').mockImplementation(() => {})
    window.addEventListener('sscm:api-error', listener)

    await expect(api.post('/forbidden-user-action', {}, {
      adapter: forbiddenAdapter,
    })).rejects.toBeTruthy()

    expect(listener).toHaveBeenCalledTimes(1)
    expect(listener.mock.calls[0][0].detail.message).toBe('אין הרשאה לבצע את הפעולה הזו.')
    window.removeEventListener('sscm:api-error', listener)
  })
})
