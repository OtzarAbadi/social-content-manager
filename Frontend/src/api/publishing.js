import api from './client.js'

export async function getPublishingStatus({ signal } = {}) {
  const response = await api.get('/publishing/status', { signal })
  return response.data
}
