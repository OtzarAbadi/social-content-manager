import api from './client.js'

export async function getPublishingRecommendation(request, signal) {
  const response = await api.post('/contents/publishing-recommendations', request, { signal })
  return response.data
}
