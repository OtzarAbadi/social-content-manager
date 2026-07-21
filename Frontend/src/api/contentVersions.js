import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081',
  withCredentials: true,
})

export async function getContentVersions(contentId, signal) {
  const response = await api.get(`/contents/${contentId}/versions`, { signal })
  return response.data
}
