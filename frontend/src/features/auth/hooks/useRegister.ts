import { useCallback, useState } from 'react'

import { authService } from '../services/auth.service'
import type { RegisterFormValues, RegisterStatus } from '../types'

const resolveErrorMessage = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const maybeHttpError = error as { message?: unknown }

    if (typeof maybeHttpError.message === 'string' && maybeHttpError.message.trim()) {
      return maybeHttpError.message
    }
  }

  return 'Unable to create your account. Please try again.'
}

export function useRegister() {
  const [status, setStatus] = useState<RegisterStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const submit = useCallback(async (values: RegisterFormValues): Promise<boolean> => {
    const { confirmPassword: _confirmPassword, ...payload } = values
    void _confirmPassword

    const sanitizedPayload = {
      firstName: payload.firstName.trim(),
      lastName: payload.lastName.trim(),
      username: payload.username.trim(),
      email: payload.email.trim(),
      password: payload.password,
    }

    if (!sanitizedPayload.firstName || !sanitizedPayload.lastName || !sanitizedPayload.username || !sanitizedPayload.email || !sanitizedPayload.password) {
      setStatus('error')
      setError('Please complete all required fields.')
      return false
    }

    setStatus('loading')
    setError(null)
    setIsSubmitting(true)

    try {
      await authService.register(sanitizedPayload)
      setStatus('success')
      return true
    } catch (caughtError) {
      setStatus('error')
      setError(resolveErrorMessage(caughtError))
      return false
    } finally {
      setIsSubmitting(false)
    }
  }, [])

  return {
    status,
    error,
    isSubmitting,
    submit,
    clearError: () => setError(null),
  }
}
