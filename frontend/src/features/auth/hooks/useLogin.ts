import { useCallback, useState } from 'react'

import { useAuth } from '../../../core/auth'
import { authService } from '../services/auth.service'
import type { LoginFormValues, LoginStatus } from '../types'

const resolveErrorMessage = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const maybeHttpError = error as { message?: unknown }

    if (typeof maybeHttpError.message === 'string' && maybeHttpError.message.trim()) {
      return maybeHttpError.message
    }
  }

  return 'Unable to sign in. Please try again.'
}

export function useLogin() {
  const { setSession } = useAuth()
  const [status, setStatus] = useState<LoginStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const submit = useCallback(
    async (values: LoginFormValues): Promise<boolean> => {
      const identifier = values.identifier.trim()
      const password = values.password

      if (!identifier || !password) {
        setStatus('error')
        setError('Username or email and password are required.')
        return false
      }

      setStatus('loading')
      setError(null)
      setIsSubmitting(true)

      try {
        const response = await authService.login({ identifier, password })

        setSession({
          accessToken: response.accessToken,
          user: {
            id: response.user.id,
            firstName: response.user.firstName,
            lastName: response.user.lastName,
            email: response.user.email,
            username: response.user.username,
          },
        })

        setStatus('success')
        return true
      } catch (caughtError) {
        setStatus('error')
        setError(resolveErrorMessage(caughtError))
        return false
      } finally {
        setIsSubmitting(false)
      }
    },
    [setSession],
  )

  return {
    status,
    error,
    isSubmitting,
    submit,
    clearError: () => setError(null),
  }
}
