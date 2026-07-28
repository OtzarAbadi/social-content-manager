export const pwaManifest = {
  name: 'SocialContent',
  short_name: 'SocialContent',
  description: 'מערכת חכמה לניהול, אישור, תזמון, פרסום וניתוח תכנים ברשתות חברתיות',
  lang: 'he',
  dir: 'rtl',
  start_url: '/',
  scope: '/',
  display: 'standalone',
  orientation: 'portrait-primary',
  theme_color: '#4f3c2d',
  background_color: '#f8f4ee',
  icons: [
    { src: '/icons/sscm-content-192x192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: '/icons/sscm-content-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
    { src: '/icons/sscm-content-maskable-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ],
}

// Only versioned frontend build assets are precached. With no runtime cache
// routes, API, authentication and personal data stay online-only.
export const pwaWorkbox = {
  globPatterns: ['**/*.{js,css,html,png,svg,woff,woff2}'],
  navigateFallback: 'index.html',
  navigateFallbackDenylist: [/^\/api(?:\/|$)/],
  runtimeCaching: [],
  cleanupOutdatedCaches: true,
  skipWaiting: false,
  clientsClaim: false,
}
