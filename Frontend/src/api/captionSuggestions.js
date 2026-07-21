import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081',
  withCredentials: true,
})

export async function generateCaptionSuggestion(request, signal) {
  const response = await api.post('/contents/caption-suggestions', request, { signal })
  return response.data
}
