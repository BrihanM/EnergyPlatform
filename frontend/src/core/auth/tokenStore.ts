let currentAccessToken: string | null = null

/**
 * Retrieves the currently stored access token from memory.
 * 
 * @returns {string | null} The stored access token or null if not available.
 */
export const getAccessToken = (): string | null => currentAccessToken

/**
 * Stores an access token in memory.
 * This token is used for authenticating subsequent API requests.
 * 
 * @param {string | null} token - The access token to store, or null to clear it.
 * @returns {void}
 */
export const setAccessToken = (token: string | null): void => {
  currentAccessToken = token
}

/**
 * Clears the stored access token from memory.
 * 
 * @returns {void}
 */
export const clearAccessToken = (): void => {
  currentAccessToken = null
}
