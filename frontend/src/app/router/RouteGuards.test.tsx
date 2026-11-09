import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { AuthContext } from '../../core/auth/authContextInstance'
import type { AuthContextValue } from '../../core/auth/types'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'

const buildAuthValue = (overrides: Partial<AuthContextValue>): AuthContextValue => ({
  status: 'unauthenticated',
  isAuthenticated: false,
  accessToken: null,
  user: null,
  setSession: () => undefined,
  clearSession: () => undefined,
  updateAccessToken: () => undefined,
  ...overrides,
})

describe('ProtectedRoute', () => {
  it('renders loading while auth is initializing', () => {
    render(
      <AuthContext.Provider value={buildAuthValue({ status: 'initializing' })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/app" element={<div>Protected content</div>} />
            </Route>
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByText('Loading application...')).toBeTruthy()
  })

  it('redirects unauthenticated users to login', () => {
    render(
      <AuthContext.Provider value={buildAuthValue({ status: 'unauthenticated' })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/app" element={<div>Protected content</div>} />
            </Route>
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByText('Login page')).toBeTruthy()
  })

  it('renders protected content for authenticated users', () => {
    render(
      <AuthContext.Provider value={buildAuthValue({ status: 'authenticated', isAuthenticated: true, accessToken: 'token' })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/app" element={<div>Protected content</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByText('Protected content')).toBeTruthy()
  })
})

describe('PublicRoute', () => {
  it('renders public content for unauthenticated users', () => {
    render(
      <AuthContext.Provider value={buildAuthValue({ status: 'unauthenticated' })}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicRoute />}>
              <Route path="/login" element={<div>Login page</div>} />
            </Route>
            <Route path="/app" element={<div>Protected app</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByText('Login page')).toBeTruthy()
  })

  it('redirects authenticated users to protected area', () => {
    render(
      <AuthContext.Provider value={buildAuthValue({ status: 'authenticated', isAuthenticated: true, accessToken: 'token' })}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicRoute />}>
              <Route path="/login" element={<div>Login page</div>} />
            </Route>
            <Route path="/app" element={<div>Protected app</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(screen.getByText('Protected app')).toBeTruthy()
  })
})
