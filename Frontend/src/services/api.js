import axios from 'axios'

const configuredUrl = import.meta.env.VITE_API_URL?.replace(/\/+$/, '')

if (!configuredUrl) {
  throw new Error('VITE_API_URL is required. Add it to the Frontend/.env file.')
}
const configuredHost = new URL(configuredUrl).hostname
const browserHost = window.location.hostname

// On a phone, "localhost" means the phone. Reuse the configured protocol/port
// with the hostname that served the frontend so the same build works over LAN.
const apiOrigin = (
  configuredHost === 'localhost' && browserHost !== 'localhost'
    ? configuredUrl.replace('localhost', browserHost)
    : configuredUrl
)

export const API_BASE_URL = `${apiOrigin}/api`

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

api.interceptors.response.use(
  (response) => {
    console.debug(`[API] ${response.config.method?.toUpperCase()} ${response.config.url}`, response.data)
    return response
  },
  (error) => {
    if (axios.isCancel(error)) return Promise.reject(error)
    const method = error.config?.method?.toUpperCase() || 'REQUEST'
    const url = error.config?.url || 'unknown URL'
    console.error(`[API] ${method} ${url} failed`, {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message,
    })
    window.dispatchEvent(new CustomEvent('sscm:api-error', {
      detail: { message: getApiErrorMessage(error) },
    }))
    return Promise.reject(error)
  },
)

export function getApiErrorMessage(error, fallback = 'הפעולה נכשלה. אפשר לנסות שוב בעוד רגע.') {
  if (!error.response) return 'לא ניתן להתחבר לשרת. בדקו את החיבור ונסו שוב.'
  if (error.response.status === 401) return 'ההתחברות פגה. יש להתחבר מחדש.'
  if (error.response.status === 403) return 'אין הרשאה לבצע את הפעולה הזו.'
  if (error.response.status >= 500) return 'אירעה תקלה זמנית בשרת. אפשר לנסות שוב בעוד רגע.'
  return error.response?.data?.message || fallback
}

export default api
