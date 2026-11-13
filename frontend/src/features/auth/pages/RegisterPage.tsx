import type { ReactElement } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { RegisterForm } from '../components/RegisterForm'

export function RegisterPage(): ReactElement {
  const navigate = useNavigate()

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-card-body">
        <p className="auth-kicker">
          Create your account
        </p>
        <h1>Create account</h1>
        <p className="auth-description">
          Set up your profile to access the platform and continue with your account.
        </p>
        <RegisterForm onSwitchToLogin={() => navigate('/login', { replace: true })} />

        <p className="auth-switch">
          Already have an account?{' '}
          <Link to="/login">
            Sign in
          </Link>
        </p>
        </div>
      </section>
    </main>
  )
}

export default RegisterPage
