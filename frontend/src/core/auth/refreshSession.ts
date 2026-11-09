import { httpClient } from '../http/httpClient'
import type { AuthenticatedUser } from './types'

type RefreshResponse = {
  authenticated: boolean
  accessToken: string
  tokenType: string
  expiresIn: number
  roles: string[]
  user: {
    id: string
    firstName: string
    lastName: string
    username: string
    email: string
    createdAt?: string
  }
}

export type RefreshedSessionPayload = {
  accessToken: string
  user: AuthenticatedUser
}

/**
 * Uses the HttpOnly refresh cookie to request a new access token.
 */
export const requestSessionRefresh = async (): Promise<RefreshedSessionPayload> => {
  const response = await httpClient.post<RefreshResponse>('/api/v1/auth/refresh', {
    credentials: 'include',
    skipAuthRefresh: true,
  })

  return {
    accessToken: response.accessToken,
    user: {
      id: response.user.id,
      firstName: response.user.firstName,
      lastName: response.user.lastName,
      username: response.user.username,
      email: response.user.email,
      roles: response.roles,
    },
  }
}
