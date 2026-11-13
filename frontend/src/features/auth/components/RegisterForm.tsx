import { useMemo, useState, type ChangeEvent, type FormEvent, type ReactElement } from 'react'

import { useRegister } from '../hooks/useRegister'
import type { RegisterFormValues } from '../types'

const initialValues: RegisterFormValues = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
}

type RegisterFormProps = {
  onSwitchToLogin?: () => void
}

export function RegisterForm({ onSwitchToLogin }: RegisterFormProps): ReactElement {
  const { submit, error, status, isSubmitting } = useRegister()
  const [values, setValues] = useState<RegisterFormValues>(initialValues)
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof RegisterFormValues, string>>>({})

  const isSuccess = status === 'success'

  const validationErrors = useMemo(() => {
    const nextErrors: Partial<Record<keyof RegisterFormValues, string>> = {}

    if (!values.firstName.trim()) {
      nextErrors.firstName = 'First name is required.'
    }

    if (!values.lastName.trim()) {
      nextErrors.lastName = 'Last name is required.'
    }

    if (!values.username.trim()) {
      nextErrors.username = 'Username is required.'
    } else if (!/^[A-Za-z0-9][A-Za-z0-9._-]{1,48}[A-Za-z0-9]$/.test(values.username.trim())) {
      nextErrors.username = 'Username must be 3-50 characters using letters, numbers, dots, underscores, or dashes.'
    }

    if (!values.email.trim()) {
      nextErrors.email = 'Email is required.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      nextErrors.email = 'Enter a valid email address.'
    }

    if (!values.password) {
      nextErrors.password = 'Password is required.'
    } else if (values.password.length < 12) {
      nextErrors.password = 'Password must be at least 12 characters long.'
    }

    if (!values.confirmPassword) {
      nextErrors.confirmPassword = 'Please confirm your password.'
    } else if (values.confirmPassword !== values.password) {
      nextErrors.confirmPassword = 'Passwords do not match.'
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
      firstName: values.firstName.trim() ? undefined : 'First name is required.',
      lastName: values.lastName.trim() ? undefined : 'Last name is required.',
      username: values.username.trim()
        ? /^[A-Za-z0-9][A-Za-z0-9._-]{1,48}[A-Za-z0-9]$/.test(values.username.trim())
          ? undefined
          : 'Username must be 3-50 characters using letters, numbers, dots, underscores, or dashes.'
        : 'Username is required.',
      email: values.email.trim()
        ? /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())
          ? undefined
          : 'Enter a valid email address.'
        : 'Email is required.',
      password: values.password ? (values.password.length >= 12 ? undefined : 'Password must be at least 12 characters long.') : 'Password is required.',
      confirmPassword: values.confirmPassword
        ? values.confirmPassword === values.password
          ? undefined
          : 'Passwords do not match.'
        : 'Please confirm your password.',
    }

    setFieldErrors(nextFieldErrors)

    if (Object.values(nextFieldErrors).some(Boolean)) {
      return
    }

    const didSubmit = await submit({
      firstName: values.firstName,
      lastName: values.lastName,
      username: values.username,
      email: values.email,
      password: values.password,
      confirmPassword: values.confirmPassword,
    })

    if (didSubmit) {
      setValues(initialValues)
      setFieldErrors({})
    }
  }

  const fieldProps = (name: keyof RegisterFormValues) => ({
    name,
    onChange: handleChange,
    value: values[name],
    'aria-invalid': Boolean(fieldErrors[name] ?? validationErrors[name]),
    'aria-describedby': fieldErrors[name] ?? validationErrors[name] ? `${name}-error` : undefined,
  })

  if (isSuccess) {
    return (
      <div className="auth-form-fields">
        <div
          className="auth-feedback auth-feedback-success"
          role="status"
        >
          Account created successfully. Please sign in.
        </div>
        {onSwitchToLogin ? (
          <button
            className="primary-button"
            type="button"
            onClick={onSwitchToLogin}
          >
            Go to sign in
          </button>
        ) : null}
      </div>
    )
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <div className="auth-name-fields">
        <div>
          <label htmlFor="firstName">
            First name
          </label>
          <input
            id="firstName"
            type="text"
            autoComplete="given-name"
            {...fieldProps('firstName')}
            placeholder="Ana"
          />
          {(fieldErrors.firstName ?? validationErrors.firstName) ? (
            <div id="firstName-error" className="auth-field-error">
              {fieldErrors.firstName ?? validationErrors.firstName}
            </div>
          ) : null}
        </div>

        <div>
          <label htmlFor="lastName">
            Last name
          </label>
          <input
            id="lastName"
            type="text"
            autoComplete="family-name"
            {...fieldProps('lastName')}
            placeholder="García"
          />
          {(fieldErrors.lastName ?? validationErrors.lastName) ? (
            <div id="lastName-error" className="auth-field-error">
              {fieldErrors.lastName ?? validationErrors.lastName}
            </div>
          ) : null}
        </div>
      </div>

      <div>
        <label htmlFor="username">
          Username
        </label>
        <input
          id="username"
          type="text"
          autoComplete="username"
          {...fieldProps('username')}
          placeholder="ana.garcia"
        />
        {(fieldErrors.username ?? validationErrors.username) ? (
          <div id="username-error" className="auth-field-error">
            {fieldErrors.username ?? validationErrors.username}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="email">
          Email
        </label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          {...fieldProps('email')}
          placeholder="ana@example.com"
        />
        {(fieldErrors.email ?? validationErrors.email) ? (
          <div id="email-error" className="auth-field-error">
            {fieldErrors.email ?? validationErrors.email}
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
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            {...fieldProps('password')}
            placeholder="Create a password"
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
        {(fieldErrors.password ?? validationErrors.password) ? (
          <div id="password-error" className="auth-field-error">
            {fieldErrors.password ?? validationErrors.password}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="confirmPassword">
          Confirm password
        </label>
        <input
          id="confirmPassword"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          {...fieldProps('confirmPassword')}
          placeholder="Repeat your password"
        />
        {(fieldErrors.confirmPassword ?? validationErrors.confirmPassword) ? (
          <div id="confirmPassword-error" className="auth-field-error">
            {fieldErrors.confirmPassword ?? validationErrors.confirmPassword}
          </div>
        ) : null}
      </div>

      <button
        className="primary-button"
        type="submit"
        disabled={isSubmitting}
      >
        {isSubmitting ? 'Creating account...' : 'Create account'}
      </button>

      {status === 'error' && error ? (
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

export default RegisterForm
