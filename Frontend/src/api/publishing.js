import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8081', withCredentials: true })

export async function getPublishingStatus({ signal } = {}) {
  const response = await api.get('/publishing/status', { signal })
  return response.data
}
