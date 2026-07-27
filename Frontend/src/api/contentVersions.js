import api from '../services/api.js'

export async function getContentVersions(contentId, signal) {
  const response = await api.get(`/contents/${contentId}/versions`, { signal })
  return response.data
}

export async function restoreContentVersion(contentId, versionNumber) {
  const response = await api.post(`/contents/${contentId}/versions/${versionNumber}/restore`)
  return response.data
}
