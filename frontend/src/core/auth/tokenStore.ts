let currentAccessToken: string | null = null

export const getAccessToken = (): string | null => currentAccessToken

export const setAccessToken = (token: string | null): void => {
  currentAccessToken = token
}

export const clearAccessToken = (): void => {
  currentAccessToken = null
}
