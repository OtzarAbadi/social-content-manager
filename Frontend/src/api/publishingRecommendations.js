import api from '../services/api.js'

export async function getPublishingRecommendation(request, signal) {
  const response = await api.post('/contents/publishing-recommendations', request, { signal })
  return response.data
}
