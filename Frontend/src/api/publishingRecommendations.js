import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081',
  withCredentials: true,
})

export async function getPublishingRecommendation(request, signal) {
  const response = await api.post('/contents/publishing-recommendations', request, { signal })
  return response.data
}
