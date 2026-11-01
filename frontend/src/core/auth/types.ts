export type AuthStatus = 'initializing' | 'authenticated' | 'unauthenticated'

export interface AuthenticatedUser {
  id?: string
  email?: string
  username?: string
}

export interface AuthSession {
  accessToken: string
  user?: AuthenticatedUser
}

export interface AuthContextValue {
  status: AuthStatus
  isAuthenticated: boolean
  accessToken: string | null
  user: AuthenticatedUser | null
  setSession: (session: AuthSession) => void
  clearSession: () => void
  updateAccessToken: (token: string | null) => void
}
