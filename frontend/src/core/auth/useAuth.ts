import { useContext } from 'react'

import { AuthContext } from './AuthContext'
import type { AuthContextValue } from './types'

/**
 * Exposes the application authentication state without importing the raw context directly.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider.')
  }

  return context
}
