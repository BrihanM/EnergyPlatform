import { HttpError } from '../http/httpError'
import type { AuthSession } from './types'

export type RefreshReason = 'startup' | 'unauthorized'
type RefreshHandler = (reason: RefreshReason) => Promise<AuthSession | null>
type ClearSessionHandler = () => void

type SessionHandlers = {
  refreshSession: RefreshHandler
  clearSession: ClearSessionHandler
}

let handlers: SessionHandlers | null = null
let activeRefreshPromise: Promise<boolean> | null = null
let sessionInvalidationVersion = 0

export const registerSessionHandlers = (nextHandlers: SessionHandlers): void => {
  handlers = nextHandlers
}

export const unregisterSessionHandlers = (): void => {
  handlers = null
}

export const markSessionInvalidated = (): void => {
  sessionInvalidationVersion += 1
}

const hasSessionExpired = (error: unknown): boolean => {
  return error instanceof HttpError && (error.status === 401 || error.status === 403)
}

/**
 * Runs one refresh request at a time and lets concurrent callers await the same result.
 */
export const refreshSessionSingleFlight = async (reason: RefreshReason): Promise<boolean> => {
  if (!handlers) {
    return false
  }

  if (activeRefreshPromise) {
    return activeRefreshPromise
  }

  const refreshVersion = sessionInvalidationVersion

  activeRefreshPromise = (async () => {
    if (!handlers) {
      return false
    }

    try {
      const session = await handlers.refreshSession(reason)

      if (refreshVersion !== sessionInvalidationVersion) {
        return false
      }

      return Boolean(session?.accessToken)
    } catch (error) {
      if (hasSessionExpired(error)) {
        handlers.clearSession()
        return false
      }

      throw error
    } finally {
      activeRefreshPromise = null
    }
  })()

  return activeRefreshPromise
}
