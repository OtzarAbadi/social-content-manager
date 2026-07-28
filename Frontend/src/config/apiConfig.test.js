import { describe, expect, it } from 'vitest'
import { resolveApiBaseUrl } from './apiConfig.js'

describe('production API URL resolution', () => {
  it('uses the configured public HTTPS origin', () => {
    expect(resolveApiBaseUrl('https://api.example.com', {
      production: true,
      browserHostname: 'app.example.com',
    })).toBe('https://api.example.com/api')
  })

  it('does not duplicate an existing api suffix', () => {
    expect(resolveApiBaseUrl('https://api.example.com/api/', {
      production: true,
    })).toBe('https://api.example.com/api')
  })

  it.each([
    'http://localhost:8081',
    'https://127.0.0.1:8081',
    'https://192.168.1.20:8081',
    'https://10.0.0.5:8081',
  ])('rejects private or insecure production URL %s', (value) => {
    expect(() => resolveApiBaseUrl(value, { production: true })).toThrow(
      'public HTTPS backend origin',
    )
  })

  it('fails safely when the production URL is missing', () => {
    expect(() => resolveApiBaseUrl('', { production: true })).toThrow(
      'VITE_API_URL is required',
    )
  })

  it('preserves local LAN development substitution', () => {
    expect(resolveApiBaseUrl('http://localhost:8081', {
      browserHostname: '192.168.1.25',
    })).toBe('http://192.168.1.25:8081/api')
  })
})
