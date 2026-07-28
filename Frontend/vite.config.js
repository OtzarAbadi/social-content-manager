import { defineConfig } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import { VitePWA } from 'vite-plugin-pwa'
import { pwaManifest, pwaWorkbox } from './pwa.config.js'

function removeDevelopmentHostFallback() {
  return {
    name: 'production-host-sanitizer',
    apply: 'build',
    enforce: 'post',
    renderChunk(code) {
      if (!code.includes('http://localhost')) return null
      return {
        code: code.replaceAll('http://localhost', 'https://invalid.invalid'),
        map: null,
      }
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  test: {
    environment: 'jsdom',
  },
  server: {
    host: '0.0.0.0',
  },
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] }),
    removeDevelopmentHostFallback(),
    VitePWA({
      registerType: 'prompt',
      injectRegister: false,
      manifest: pwaManifest,
      workbox: pwaWorkbox,
    }),
  ],
})
