import api from '../services/api.js'

export async function generateCaptionSuggestion(request, signal) {
  const response = await api.post('/contents/caption-suggestions', request, { signal })
  return response.data
}
