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
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <div>
        <label htmlFor="identifier">
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
        />
        {identifierError ? (
          <div id="identifier-error" className="auth-field-error">
            {identifierError}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="password">
          Password
        </label>
        <div className="auth-password-field">
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
          />
          <button
            className="password-toggle"
            type="button"
            onClick={() => setShowPassword((current) => !current)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? 'Hide' : 'Show'}
          </button>
        </div>
        {passwordError ? (
          <div id="password-error" className="auth-field-error">
            {passwordError}
          </div>
        ) : null}
      </div>

      <button
        className="primary-button"
        type="submit"
        disabled={isSubmitting}
      >
        {isSubmitting ? 'Signing in...' : 'Sign in'}
      </button>

      {hasError && error ? (
        <div
          className="auth-feedback auth-feedback-error"
          role="alert"
        >
          {error}
        </div>
      ) : null}
    </form>
  )
}
