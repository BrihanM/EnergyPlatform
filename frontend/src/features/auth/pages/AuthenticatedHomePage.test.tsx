import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { AuthContext } from '../../../core/auth/authContextInstance'
import type { AuthContextValue } from '../../../core/auth/types'
import { authService } from '../services/auth.service'
import { AuthenticatedHomePage } from './AuthenticatedHomePage'

const createAuthValue = (overrides: Partial<AuthContextValue>): AuthContextValue => ({
  status: 'authenticated',
  isAuthenticated: true,
  accessToken: 'token-1',
  user: { username: 'demo-user', email: 'demo@example.com' },
  setSession: () => undefined,
  clearSession: () => undefined,
  updateAccessToken: () => undefined,
  ...overrides,
})

describe('AuthenticatedHomePage', () => {
  it('logs out once, clears session, and navigates to login', async () => {
    const logoutSpy = vi.spyOn(authService, 'logout').mockResolvedValue(undefined)
    const clearSession = vi.fn()

    render(
      <AuthContext.Provider value={createAuthValue({ clearSession })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route path="/app" element={<AuthenticatedHomePage />} />
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    await waitFor(() => {
      expect(logoutSpy).toHaveBeenCalledTimes(1)
      expect(clearSession).toHaveBeenCalledTimes(1)
      expect(screen.getByText('Login page')).toBeTruthy()
    })
  })

  it('prevents multiple concurrent logout requests', async () => {
    let resolveLogout: (() => void) | undefined
    const logoutSpy = vi.spyOn(authService, 'logout').mockReturnValue(
      new Promise<void>((resolve) => {
        resolveLogout = resolve
      }),
    )

    render(
      <AuthContext.Provider value={createAuthValue({ clearSession: vi.fn() })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route path="/app" element={<AuthenticatedHomePage />} />
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    fireEvent.click(screen.getByRole('button', { name: 'Signing out...' }))

    expect(logoutSpy).toHaveBeenCalledTimes(1)
    resolveLogout?.()
  })

  it('clears local session and redirects even when backend logout fails', async () => {
    vi.spyOn(authService, 'logout').mockRejectedValue(new Error('network failed'))
    const clearSession = vi.fn()

    render(
      <AuthContext.Provider value={createAuthValue({ clearSession })}>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route path="/app" element={<AuthenticatedHomePage />} />
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    await waitFor(() => {
      expect(clearSession).toHaveBeenCalledTimes(1)
      expect(screen.getByText('Login page')).toBeTruthy()
    })
  })
})
