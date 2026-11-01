import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'

import { AuthProvider } from './AuthContext'
import { clearAccessToken } from './tokenStore'
import { useAuth } from './useAuth'

describe('AuthProvider', () => {
  beforeEach(() => {
    clearAccessToken()
  })

  it('starts unauthenticated and becomes authenticated after a session is set', async () => {
    const Probe = () => {
      const { status, isAuthenticated, accessToken, user, setSession, clearSession } = useAuth()

      return (
        <>
          <span data-testid="status">{status}</span>
          <span data-testid="authenticated">{String(isAuthenticated)}</span>
          <span data-testid="token">{accessToken ?? 'none'}</span>
          <span data-testid="user">{user?.email ?? 'no-user'}</span>
          <button
            type="button"
            onClick={() =>
              setSession({
                accessToken: 'token-123',
                user: {
                  id: 'user-1',
                  email: 'user@example.com',
                  username: 'demo-user',
                },
              })
            }
          >
            login
          </button>
          <button type="button" onClick={clearSession}>
            logout
          </button>
        </>
      )
    }

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('unauthenticated')
    })

    fireEvent.click(screen.getByRole('button', { name: 'login' }))

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('authenticated')
      expect(screen.getByTestId('token').textContent).toBe('token-123')
      expect(screen.getByTestId('user').textContent).toBe('user@example.com')
    })

    fireEvent.click(screen.getByRole('button', { name: 'logout' }))

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('unauthenticated')
      expect(screen.getByTestId('token').textContent).toBe('none')
    })
  })
})

describe('useAuth', () => {
  it('throws a clear error when used outside the provider', () => {
    const Probe = () => {
      useAuth()
      return null
    }

    expect(() => render(<Probe />)).toThrow('useAuth must be used within an AuthProvider.')
  })
})
