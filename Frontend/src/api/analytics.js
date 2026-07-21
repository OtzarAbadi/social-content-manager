import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8081', withCredentials: true })

export async function getAnalyticsDashboard() {
  return (await api.get('/analytics/dashboard')).data
}

export async function getAnalyticsProfile() {
  return (await api.get('/users/me')).data
}
