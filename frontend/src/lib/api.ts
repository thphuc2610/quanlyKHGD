import axios from 'axios';
import { clearStoredSession, readStoredSession } from '../features/auth/auth.storage';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30000,
  headers: {
    Accept: 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const session = readStoredSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredSession();
    }
    return Promise.reject(error);
  }
);
