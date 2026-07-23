import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8081', withCredentials: true })

export async function getActivity({ limit = 50, type, signal } = {}) {
  const response = await api.get('/activity', {
    params: { limit, ...(type ? { type } : {}) },
    signal,
  })
  return response.data
}
