import { createContext } from 'react'

import type { AuthContextValue } from './types'

/**
 * React Context for managing application-wide authentication state.
 * Provides access to authentication status, user information, and session management methods.
 * 
 * @type {React.Context<AuthContextValue | null>}
 */
export const AuthContext = createContext<AuthContextValue | null>(null)
