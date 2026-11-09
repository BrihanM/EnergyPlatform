import type { ReactElement } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { RegisterForm } from '../components/RegisterForm'

export function RegisterPage(): ReactElement {
  const navigate = useNavigate()

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
          width: 'min(540px, 100%)',
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
          Create your account
        </p>
        <h1 style={{ margin: '0.8rem 0 0.5rem', fontSize: '2rem' }}>Create account</h1>
        <p style={{ margin: '0 0 1.5rem', color: '#475569', lineHeight: 1.6 }}>
          Set up your profile to access the platform and continue with your account.
        </p>
        <RegisterForm onSwitchToLogin={() => navigate('/login', { replace: true })} />

        <p style={{ marginTop: '1.25rem', textAlign: 'center', color: '#475569' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: '#2563eb', fontWeight: 700 }}>
            Sign in
          </Link>
        </p>
      </section>
    </main>
  )
}

export default RegisterPage
