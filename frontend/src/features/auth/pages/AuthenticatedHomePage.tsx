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
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-card-body">
        <h1>Authenticated area</h1>
        <p className="auth-description">
          You are signed in as {user?.username ?? user?.email ?? 'member'}.
        </p>
        <button
          className="danger-button"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          {isLoggingOut ? 'Signing out...' : 'Sign out'}
        </button>
        </div>
      </section>
    </main>
  )
}

export default AuthenticatedHomePage
