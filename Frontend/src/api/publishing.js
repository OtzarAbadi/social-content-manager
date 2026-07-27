import api from '../services/api.js'

export async function getPublishingStatus({ signal } = {}) {
  const response = await api.get('/publishing/status', { signal })
  return response.data
}

export async function publishContentToInstagram(contentId) {
  const response = await api.post(
    `/contents/${contentId}/publish/instagram`,
    undefined,
    { suppressGlobalErrorToast: true },
  )
  return response.data
}

export function getInstagramPublishErrorMessage(error) {
  const status = error?.response?.status
  const backendMessage = String(error?.response?.data?.message || '').toLowerCase()
  if (status === 401) return 'ההתחברות פגה. יש להתחבר מחדש לפני הפרסום.'
  if (status === 403) return 'רק מנהל מערכת רשאי לפרסם באינסטגרם.'
  if (backendMessage.includes('approved')) return 'ניתן לפרסם באינסטגרם רק תוכן מאושר.'
  if (backendMessage.includes('public https') || backendMessage.includes('image must')) {
    return 'כתובת התמונה אינה ציבורית או מאובטחת. יש להעלות את התמונה מחדש.'
  }
  if (backendMessage.includes('token') || backendMessage.includes('oauth')) {
    return 'החיבור ל-Meta פג או אינו תקין. יש לעדכן את הרשאות החשבון.'
  }
  if (status === 502 || backendMessage.includes('meta') || backendMessage.includes('instagram')) {
    return 'הפרסום באינסטגרם נכשל. בדקו את חיבור Meta ונסו שוב.'
  }
  return 'לא הצלחנו לפרסם באינסטגרם. אפשר לנסות שוב בעוד רגע.'
}
