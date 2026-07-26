import axios from 'axios'

const hostname = window.location.hostname

export const API_BASE_URL = (
  import.meta.env.VITE_API_URL || `http://${hostname}:8081`
).replace(/\/+$/, '')

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

export default api
