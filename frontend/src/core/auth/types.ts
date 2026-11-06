/**
 * Represents the authentication status of the application.
 * 
 * @typedef {string} AuthStatus
 * @type {'initializing' | 'authenticated' | 'unauthenticated'}
 */
export type AuthStatus = 'initializing' | 'authenticated' | 'unauthenticated'

/**
 * Represents an authenticated user in the system.
 */
export interface AuthenticatedUser {
  /** The unique identifier of the user. */
  id?: string
<<<<<<< Updated upstream
  firstName?: string
  lastName?: string
=======
  /** The email address of the user. */
>>>>>>> Stashed changes
  email?: string
  /** The username of the user. */
  username?: string
  roles?: string[]
}

/**
 * Represents an authenticated session containing user and token information.
 */
export interface AuthSession {
  /** The JWT access token for authenticating API requests. */
  accessToken: string
  /** The authenticated user information. */
  user?: AuthenticatedUser
}

/**
 * Provides the shape of the authentication context value.
 * This is the main interface for accessing authentication state and methods throughout the application.
 */
export interface AuthContextValue {
  /** The current authentication status. */
  status: AuthStatus
  /** Whether the user is currently authenticated. */
  isAuthenticated: boolean
  /** The current access token or null if not authenticated. */
  accessToken: string | null
  /** The current authenticated user or null if not authenticated. */
  user: AuthenticatedUser | null
  /**
   * Updates the session with new user and token information.
   * @param {AuthSession} session - The new session data.
   */
  setSession: (session: AuthSession) => void
  /**
   * Clears the current session and authentication state.
   */
  clearSession: () => void
  /**
   * Updates only the access token without affecting other session data.
   * @param {string | null} token - The new access token or null to clear it.
   */
  updateAccessToken: (token: string | null) => void
}
