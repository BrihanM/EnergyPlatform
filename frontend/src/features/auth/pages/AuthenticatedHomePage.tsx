import { useState, type ReactElement } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../../../core/auth'
import { authService } from '../services/auth.service'

/**
 * Minimal protected page used to validate route guards and logout flow.
 */
export function AuthenticatedHomePage(): ReactElement {
  const { user, clearSession } = useAuth()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const handleLogout = async (): Promise<void> => {
    if (isLoggingOut) {
      return
    }

    setIsLoggingOut(true)

    let notice = 'You have been signed out.'

    try {
      await authService.logout()
    } catch {
      notice = 'Signed out locally. We could not contact the server.'
    } finally {
      clearSession()
      navigate('/login', { replace: true, state: { logoutNotice: notice } })
      setIsLoggingOut(false)
    }
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
          width: 'min(420px, 100%)',
          background: '#ffffff',
          borderRadius: '20px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 24px 60px rgba(15, 23, 42, 0.08)',
          padding: '2rem',
        }}
      >
        <h1 style={{ margin: '0 0 0.75rem', fontSize: '1.8rem' }}>Authenticated area</h1>
        <p style={{ margin: '0 0 1.5rem', color: '#475569' }}>
          You are signed in as {user?.username ?? user?.email ?? 'member'}.
        </p>
        <button
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
          style={{
            width: '100%',
            border: 'none',
            borderRadius: '10px',
            padding: '0.8rem 1rem',
            background: '#dc2626',
            color: '#ffffff',
            fontWeight: 700,
            cursor: isLoggingOut ? 'not-allowed' : 'pointer',
            opacity: isLoggingOut ? 0.75 : 1,
          }}
        >
          {isLoggingOut ? 'Signing out...' : 'Sign out'}
        </button>
      </section>
    </main>
  )
}

export default AuthenticatedHomePage
