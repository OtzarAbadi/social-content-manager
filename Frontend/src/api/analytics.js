import api from './client.js'

export async function getAnalyticsDashboard() {
  return (await api.get('/analytics/dashboard')).data
}

export async function getAnalyticsProfile() {
  return (await api.get('/users/me')).data
}
