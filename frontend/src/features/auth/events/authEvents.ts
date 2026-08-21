export const AUTH_SESSION_EXPIRED_EVENT = 'booknest:auth-session-expired'

export function notifyAuthSessionExpired() {
  window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT))
}
