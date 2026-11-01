import type { ReactElement } from 'react'
import { Link, useLocation } from 'react-router-dom'

import { LoginForm } from '../components/LoginForm'
type LoginLocationState = {
  logoutNotice?: string
}

export function LoginPage(): ReactElement {
  const location = useLocation()
  const state = location.state as LoginLocationState | null
  const logoutNotice = state?.logoutNotice

  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        padding: '2rem 1rem',
        background: 'linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%)',
      }}
    >
      <section
        style={{
          width: 'min(440px, 100%)',
          background: '#ffffff',
          borderRadius: '20px',
          boxShadow: '0 24px 60px rgba(15, 23, 42, 0.08)',
          border: '1px solid #e2e8f0',
          padding: '2rem',
        }}
      >
        <p
          style={{
            margin: 0,
            color: '#475569',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            fontSize: '0.72rem',
            fontWeight: 700,
          }}
        >
          Access your account
        </p>
        <h1 style={{ margin: '0.8rem 0 0.5rem', fontSize: '2rem' }}>Sign in</h1>
        <p style={{ margin: '0 0 1.5rem', color: '#475569', lineHeight: 1.6 }}>
          Use your username or email and password to continue.
        </p>
        <LoginForm />

        {logoutNotice ? (
          <div
            role="status"
            style={{
              marginTop: '1rem',
              padding: '0.75rem 1rem',
              borderRadius: '10px',
              background: '#eff6ff',
              color: '#1d4ed8',
            }}
          >
            {logoutNotice}
          </div>
        ) : null}

        <p style={{ marginTop: '1.25rem', textAlign: 'center', color: '#475569' }}>
          Need an account?{' '}
          <Link to="/register" style={{ color: '#2563eb', fontWeight: 700 }}>
            Create one
          </Link>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
