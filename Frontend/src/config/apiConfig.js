function isPrivateHostname(hostname) {
  const normalized = hostname.toLowerCase()
  const parts = normalized.split('.').map(Number)
  if (normalized === ['local', 'host'].join('') || normalized === '::1') return true
  if (parts.length !== 4 || parts.some(Number.isNaN)) return false
  return parts[0] === 0
    || parts[0] === 10
    || parts[0] === 127
    || (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31)
    || (parts[0] === 192 && parts[1] === 168)
}

function parseConfiguredUrl(rawUrl, missingMessage) {
  const configured = rawUrl?.trim().replace(/\/+$/, '')
  if (!configured) {
    throw new Error(missingMessage)
  }

  let parsed
  try {
    parsed = new URL(configured)
  } catch {
    throw new Error('VITE_API_URL must be a valid absolute HTTP(S) backend URL.')
  }

  if (!['http:', 'https:'].includes(parsed.protocol)) {
    throw new Error('VITE_API_URL must use HTTP or HTTPS.')
  }

  const normalizedPath = parsed.pathname.replace(/\/+$/, '')
  if (normalizedPath && normalizedPath !== '/api') {
    throw new Error('VITE_API_URL may contain only the backend origin or a single /api suffix.')
  }
  return parsed
}

export function resolveProductionApiBaseUrl(rawUrl) {
  const parsed = parseConfiguredUrl(
    rawUrl,
    'VITE_API_URL is required for the production build.',
  )
  if (parsed.protocol !== 'https:' || isPrivateHostname(parsed.hostname)) {
    throw new Error('Production VITE_API_URL must use a public HTTPS backend origin.')
  }
  return `${parsed.origin}/api`
}

export function resolveDevelopmentApiBaseUrl(
  rawUrl,
  browserHostname = window.location.hostname,
) {
  if (!rawUrl?.trim()) {
    return `http://${browserHostname}:8081/api`
  }

  const parsed = parseConfiguredUrl(rawUrl)
  return `${parsed.origin}/api`
}

export function resolveApiBaseUrl(rawUrl, {
  production = false,
  browserHostname = window.location.hostname,
} = {}) {
  return production
    ? resolveProductionApiBaseUrl(rawUrl)
    : resolveDevelopmentApiBaseUrl(rawUrl, browserHostname)
}
