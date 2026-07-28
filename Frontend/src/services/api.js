import axios from 'axios'
import {
  resolveDevelopmentApiBaseUrl,
  resolveProductionApiBaseUrl,
} from '../config/apiConfig.js'

export const API_BASE_URL = import.meta.env.PROD
  ? resolveProductionApiBaseUrl(import.meta.env.VITE_API_URL)
  : resolveDevelopmentApiBaseUrl(import.meta.env.VITE_API_URL)

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isCancel(error)) return Promise.reject(error)
    const method = error.config?.method?.toUpperCase() || 'REQUEST'
    const url = error.config?.url || 'unknown URL'
    console.error(`[API] ${method} ${url} failed with status ${error.response?.status ?? 'network-error'}`)
    if (!error.config?.suppressGlobalErrorToast) {
      window.dispatchEvent(new CustomEvent('sscm:api-error', {
        detail: { message: getApiErrorMessage(error) },
      }))
    }
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
