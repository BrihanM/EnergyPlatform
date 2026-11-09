import { useMemo, useState, type ChangeEvent, type FormEvent, type ReactElement } from 'react'

import { useLogin } from '../hooks/useLogin'
import type { LoginFormValues } from '../types'

const initialValues: LoginFormValues = {
  identifier: '',
  password: '',
}

export function LoginForm(): ReactElement {
  const { submit, error, status, isSubmitting } = useLogin()
  const [values, setValues] = useState<LoginFormValues>(initialValues)
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof LoginFormValues, string>>>({})

  const hasError = status === 'error'

  const validationErrors = useMemo(() => {
    const nextErrors: Partial<Record<keyof LoginFormValues, string>> = {}

    if (!values.identifier.trim()) {
      nextErrors.identifier = 'Username or email is required.'
    }

    if (!values.password) {
      nextErrors.password = 'Password is required.'
    }

    return nextErrors
  }, [values])

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const nextFieldErrors = {
      identifier: values.identifier.trim() ? undefined : 'Username or email is required.',
      password: values.password ? undefined : 'Password is required.',
    }

    setFieldErrors(nextFieldErrors)

    if (nextFieldErrors.identifier || nextFieldErrors.password) {
      return
    }

    await submit({
      identifier: values.identifier.trim(),
      password: values.password,
    })
  }

  const identifierError = fieldErrors.identifier ?? validationErrors.identifier
  const passwordError = fieldErrors.password ?? validationErrors.password

  return (
    <form onSubmit={handleSubmit} noValidate style={{ display: 'grid', gap: '1rem' }}>
      <div>
        <label htmlFor="identifier" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
          Username or email
        </label>
        <input
          id="identifier"
          name="identifier"
          type="text"
          autoComplete="username"
          value={values.identifier}
          onChange={handleChange}
          placeholder="jdoe or john@example.com"
          aria-invalid={Boolean(identifierError)}
          aria-describedby={identifierError ? 'identifier-error' : undefined}
          style={{
            width: '100%',
            border: identifierError ? '1px solid #dc2626' : '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '0.8rem 0.9rem',
            fontSize: '1rem',
            outline: 'none',
          }}
        />
        {identifierError ? (
          <div id="identifier-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {identifierError}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="password" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
          Password
        </label>
        <div style={{ position: 'relative' }}>
          <input
            id="password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            value={values.password}
            onChange={handleChange}
            placeholder="Enter your password"
            aria-invalid={Boolean(passwordError)}
            aria-describedby={passwordError ? 'password-error' : undefined}
            style={{
              width: '100%',
              border: passwordError ? '1px solid #dc2626' : '1px solid #cbd5e1',
              borderRadius: '10px',
              padding: '0.8rem 2.75rem 0.8rem 0.9rem',
              fontSize: '1rem',
              outline: 'none',
            }}
          />
          <button
            type="button"
            onClick={() => setShowPassword((current) => !current)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            style={{
              position: 'absolute',
              right: '0.65rem',
              top: '50%',
              transform: 'translateY(-50%)',
              border: 'none',
              background: 'transparent',
              color: '#475569',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {showPassword ? 'Hide' : 'Show'}
          </button>
        </div>
        {passwordError ? (
          <div id="password-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {passwordError}
          </div>
        ) : null}
      </div>

      <button
        type="submit"
        disabled={isSubmitting}
        style={{
          border: 'none',
          borderRadius: '10px',
          padding: '0.85rem 1rem',
          background: '#2563eb',
          color: '#ffffff',
          fontWeight: 700,
          cursor: isSubmitting ? 'not-allowed' : 'pointer',
          opacity: isSubmitting ? 0.75 : 1,
        }}
      >
        {isSubmitting ? 'Signing in...' : 'Sign in'}
      </button>

      {hasError && error ? (
        <div
          role="alert"
          style={{
            padding: '0.85rem 1rem',
            borderRadius: '10px',
            background: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#b91c1c',
          }}
        >
          {error}
        </div>
      ) : null}
    </form>
  )
}
