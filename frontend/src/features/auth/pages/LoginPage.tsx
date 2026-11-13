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
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-card-body">
        <p className="auth-kicker">
          Access your account
        </p>
        <h1>Sign in</h1>
        <p className="auth-description">
          Use your username or email and password to continue.
        </p>
        <LoginForm />

        {logoutNotice ? (
          <div className="auth-feedback" role="status">
            {logoutNotice}
          </div>
        ) : null}

        <p className="auth-switch">
          Need an account?{' '}
          <Link to="/register">
            Create one
          </Link>
        </p>
        </div>
      </section>
    </main>
  )
}

export default LoginPage
