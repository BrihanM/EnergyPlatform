import type { ReactElement } from 'react'

export function RouteLoading(): ReactElement {
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
      <p style={{ margin: 0, color: '#475569', fontWeight: 600 }}>Loading application...</p>
    </main>
  )
}

export default RouteLoading
