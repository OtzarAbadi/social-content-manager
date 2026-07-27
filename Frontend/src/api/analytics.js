import api from '../services/api.js'

export async function getAnalyticsDashboard() {
  return (await api.get('/analytics/dashboard')).data
}

export async function getAnalyticsProfile() {
  return (await api.get('/users/me')).data
}

export async function getInstagramAccountInsights(params) {
  return (await api.get('/instagram/insights/account', { params, suppressGlobalErrorToast: true })).data
}

export async function getInstagramMediaInsights(params) {
  return (await api.get('/instagram/insights/media', { params, suppressGlobalErrorToast: true })).data
}
