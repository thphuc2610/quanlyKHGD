import type { AuthSession } from './auth.types';

const AUTH_STORAGE_KEY = 'klgd.auth';

export function readStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveStoredSession(session: AuthSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}
