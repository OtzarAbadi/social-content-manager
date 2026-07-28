import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { pwaManifest, pwaWorkbox } from '../pwa.config.js'

describe('PWA configuration', () => {
  it('defines installable RTL metadata and required icons', () => {
    expect(pwaManifest).toMatchObject({
      name: 'SocialContent',
      short_name: 'SocialContent',
      display: 'standalone',
      start_url: '/',
      scope: '/',
      lang: 'he',
      dir: 'rtl',
    })
    expect(pwaManifest.icons.map((icon) => icon.sizes)).toEqual(['192x192', '512x512', '512x512'])
    expect(pwaManifest.icons.some((icon) => icon.purpose === 'maskable')).toBe(true)
  })

  it('does not runtime-cache API or sensitive application data', () => {
    expect(pwaWorkbox.runtimeCaching).toEqual([])
    expect(pwaWorkbox.navigateFallbackDenylist.some((rule) => rule.test('/api/clients'))).toBe(true)
    expect(JSON.stringify(pwaWorkbox.globPatterns)).not.toMatch(/api|auth|client|content|insights|meta|publish/i)
  })

  it('preserves every existing React route', () => {
    const appSource = readFileSync('src/App.jsx', 'utf8')
    for (const path of [
      '/dashboard',
      '/content',
      '/clients',
      '/messages',
      '/calendar',
      '/analytics',
      '/notifications',
      '/activity',
      '/integrations',
      '/login',
    ]) {
      expect(appSource).toContain(`path: '${path}'`)
    }
  })

  it('configures Vercel SPA fallback without proxying api routes', () => {
    const vercel = JSON.parse(readFileSync('vercel.json', 'utf8'))
    expect(vercel.routes).toContainEqual({ src: '/api(?:/.*)?', status: 404 })
    expect(vercel.routes.at(-1)).toEqual({ src: '/.*', dest: '/index.html' })
    expect(JSON.stringify(vercel)).not.toMatch(/https?:\/\//)
  })
})
