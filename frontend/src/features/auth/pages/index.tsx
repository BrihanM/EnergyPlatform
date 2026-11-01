import type { ReactElement } from 'react';

export function LoginPage(): ReactElement {
  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: '#f5f7fb',
        color: '#0f172a',
        fontFamily: 'Inter, system-ui, sans-serif',
      }}
    >
      <section
        style={{
          width: 'min(420px, 90vw)',
          padding: '2rem',
          borderRadius: '16px',
          background: '#ffffff',
          boxShadow: '0 18px 40px rgba(15, 23, 42, 0.08)',
        }}
      >
        <p style={{ margin: 0, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
          Authentication
        </p>
        <h1 style={{ marginTop: '0.75rem', marginBottom: 0 }}>Module ready for API integration</h1>
        <p style={{ marginTop: '1rem', color: '#334155', lineHeight: 1.6 }}>
          The frontend infrastructure is prepared to call the API Gateway, while the auth UI remains
          intentionally deferred to a later module implementation.
        </p>
      </section>
    </main>
  );
}

export default LoginPage;
