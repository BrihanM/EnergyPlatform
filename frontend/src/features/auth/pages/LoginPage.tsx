import { useState, type ReactElement } from 'react'

import { useAuth } from '../../../core/auth'
import { LoginForm } from '../components/LoginForm'
import { authService } from '../services/auth.service'

export function LoginPage(): ReactElement {
  const { isAuthenticated, user, clearSession } = useAuth()
  const [logoutMessage, setLogoutMessage] = useState<string | null>(null)

  const handleLogout = async (): Promise<void> => {
    try {
      await authService.logout()
    } catch {
      // If logout fails on the backend, the local session is still cleared.
    } finally {
      clearSession()
      setLogoutMessage('You have been signed out.')
    }
  }

  if (isAuthenticated) {
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
            width: 'min(420px, 100%)',
            background: '#ffffff',
            borderRadius: '20px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 24px 60px rgba(15, 23, 42, 0.08)',
            padding: '2rem',
          }}
        >
          <h1 style={{ margin: '0 0 0.75rem', fontSize: '1.8rem' }}>Signed in</h1>
          <p style={{ margin: '0 0 1.5rem', color: '#475569' }}>
            Welcome back, {user?.username ?? user?.email ?? 'member'}.
          </p>
          <button
            type="button"
            onClick={handleLogout}
            style={{
              width: '100%',
              border: 'none',
              borderRadius: '10px',
              padding: '0.8rem 1rem',
              background: '#dc2626',
              color: '#ffffff',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            Sign out
          </button>
          {logoutMessage ? (
            <div
              style={{
                marginTop: '1rem',
                padding: '0.75rem 1rem',
                borderRadius: '10px',
                background: '#eff6ff',
                color: '#1d4ed8',
              }}
            >
              {logoutMessage}
            </div>
          ) : null}
        </section>
      </main>
    )
  }

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
      </section>
    </main>
  )
}

export default LoginPage
