import api from '../services/api.js'

export async function getPublishingStatus({ signal } = {}) {
  const response = await api.get('/publishing/status', { signal })
  return response.data
}
