import {
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactElement,
  type ReactNode,
} from 'react'

import { AuthContext } from './authContextInstance'
import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore'
import type { AuthContextValue, AuthSession, AuthStatus } from './types'

/**
 * Provides the global application authentication state in memory.
 */
export function AuthProvider({ children }: { children: ReactNode }): ReactElement {
  const [session, setSessionState] = useState<AuthSession | null>(() => {
    const storedToken = getAccessToken()

    return storedToken ? { accessToken: storedToken } : null
  })
  const [status, setStatus] = useState<AuthStatus>(() => (getAccessToken() ? 'authenticated' : 'unauthenticated'))

  const setSession = useCallback((nextSession: AuthSession): void => {
    setSessionState({ ...nextSession })
    setAccessToken(nextSession.accessToken)
    setStatus('authenticated')
  }, [])

  const clearSession = useCallback((): void => {
    setSessionState(null)
    clearAccessToken()
    setStatus('unauthenticated')
  }, [])

  const updateAccessToken = useCallback((token: string | null): void => {
    if (token === null) {
      setAccessToken(null)
      setSessionState((currentSession) => (currentSession ? null : currentSession))
      setStatus('unauthenticated')
      return
    }

    setAccessToken(token)

    setSessionState((currentSession) => {
      if (!currentSession) {
        return { accessToken: token }
      }

      return {
        ...currentSession,
        accessToken: token,
      }
    })

    setStatus('authenticated')
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      isAuthenticated: status === 'authenticated',
      accessToken: session?.accessToken ?? getAccessToken(),
      user: session?.user ?? null,
      setSession,
      clearSession,
      updateAccessToken,
    }),
    [clearSession, session, setSession, status, updateAccessToken],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuthContext(): AuthContextValue {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('Auth context is unavailable. Ensure the application is wrapped in AuthProvider.')
  }

  return context
}
