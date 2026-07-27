import api from '../services/api.js'

export async function getActivity({ limit = 50, type, signal } = {}) {
  const response = await api.get('/activity', {
    params: { limit, ...(type ? { type } : {}) },
    signal,
  })
  return response.data
}
