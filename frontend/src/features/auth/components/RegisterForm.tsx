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
      <div style={{ display: 'grid', gap: '1rem' }}>
        <div
          role="status"
          style={{
            padding: '0.9rem 1rem',
            borderRadius: '10px',
            background: '#ecfdf5',
            border: '1px solid #a7f3d0',
            color: '#166534',
          }}
        >
          Account created successfully. Please sign in.
        </div>
        {onSwitchToLogin ? (
          <button
            type="button"
            onClick={onSwitchToLogin}
            style={{
              border: 'none',
              borderRadius: '10px',
              padding: '0.85rem 1rem',
              background: '#2563eb',
              color: '#ffffff',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            Go to sign in
          </button>
        ) : null}
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} noValidate style={{ display: 'grid', gap: '1rem' }}>
      <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
        <div>
          <label htmlFor="firstName" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
            First name
          </label>
          <input
            id="firstName"
            type="text"
            autoComplete="given-name"
            {...fieldProps('firstName')}
            placeholder="Ana"
            style={{
              width: '100%',
              border: fieldErrors.firstName || validationErrors.firstName ? '1px solid #dc2626' : '1px solid #cbd5e1',
              borderRadius: '10px',
              padding: '0.8rem 0.9rem',
              fontSize: '1rem',
              outline: 'none',
            }}
          />
          {(fieldErrors.firstName ?? validationErrors.firstName) ? (
            <div id="firstName-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
              {fieldErrors.firstName ?? validationErrors.firstName}
            </div>
          ) : null}
        </div>

        <div>
          <label htmlFor="lastName" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
            Last name
          </label>
          <input
            id="lastName"
            type="text"
            autoComplete="family-name"
            {...fieldProps('lastName')}
            placeholder="García"
            style={{
              width: '100%',
              border: fieldErrors.lastName || validationErrors.lastName ? '1px solid #dc2626' : '1px solid #cbd5e1',
              borderRadius: '10px',
              padding: '0.8rem 0.9rem',
              fontSize: '1rem',
              outline: 'none',
            }}
          />
          {(fieldErrors.lastName ?? validationErrors.lastName) ? (
            <div id="lastName-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
              {fieldErrors.lastName ?? validationErrors.lastName}
            </div>
          ) : null}
        </div>
      </div>

      <div>
        <label htmlFor="username" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
          Username
        </label>
        <input
          id="username"
          type="text"
          autoComplete="username"
          {...fieldProps('username')}
          placeholder="ana.garcia"
          style={{
            width: '100%',
            border: fieldErrors.username || validationErrors.username ? '1px solid #dc2626' : '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '0.8rem 0.9rem',
            fontSize: '1rem',
            outline: 'none',
          }}
        />
        {(fieldErrors.username ?? validationErrors.username) ? (
          <div id="username-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {fieldErrors.username ?? validationErrors.username}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="email" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
          Email
        </label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          {...fieldProps('email')}
          placeholder="ana@example.com"
          style={{
            width: '100%',
            border: fieldErrors.email || validationErrors.email ? '1px solid #dc2626' : '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '0.8rem 0.9rem',
            fontSize: '1rem',
            outline: 'none',
          }}
        />
        {(fieldErrors.email ?? validationErrors.email) ? (
          <div id="email-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {fieldErrors.email ?? validationErrors.email}
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
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            {...fieldProps('password')}
            placeholder="Create a password"
            style={{
              width: '100%',
              border: fieldErrors.password || validationErrors.password ? '1px solid #dc2626' : '1px solid #cbd5e1',
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
        {(fieldErrors.password ?? validationErrors.password) ? (
          <div id="password-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {fieldErrors.password ?? validationErrors.password}
          </div>
        ) : null}
      </div>

      <div>
        <label htmlFor="confirmPassword" style={{ display: 'block', marginBottom: '0.4rem', fontWeight: 600 }}>
          Confirm password
        </label>
        <input
          id="confirmPassword"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          {...fieldProps('confirmPassword')}
          placeholder="Repeat your password"
          style={{
            width: '100%',
            border: fieldErrors.confirmPassword || validationErrors.confirmPassword ? '1px solid #dc2626' : '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '0.8rem 0.9rem',
            fontSize: '1rem',
            outline: 'none',
          }}
        />
        {(fieldErrors.confirmPassword ?? validationErrors.confirmPassword) ? (
          <div id="confirmPassword-error" style={{ marginTop: '0.35rem', color: '#b91c1c', fontSize: '0.85rem' }}>
            {fieldErrors.confirmPassword ?? validationErrors.confirmPassword}
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
        {isSubmitting ? 'Creating account...' : 'Create account'}
      </button>

      {status === 'error' && error ? (
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

export default RegisterForm
