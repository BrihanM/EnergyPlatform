/**
 * Test suite for AuthContext and AuthProvider components.
 * 
 * Tests the authentication provider's ability to:
 * - Initialize with unauthenticated state
 * - Transition to authenticated state when a session is set
 * - Store and retrieve access tokens
 * - Store and retrieve user information
 * - Return to unauthenticated state when session is cleared
 * 
 * Also validates that useAuth throws appropriate errors when used outside the provider.
 */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthProvider } from './AuthContext'
import { clearAccessToken } from './tokenStore'
import { useAuth } from './useAuth'

describe('AuthProvider', () => {
  beforeEach(() => {
    clearAccessToken()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ message: 'Invalid refresh session' }), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
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

  it('restores the session from refresh cookie on startup when refresh succeeds', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            authenticated: true,
            accessToken: 'restored-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
            roles: ['NORMAL_USER'],
            user: {
              id: 'user-2',
              firstName: 'Jane',
              lastName: 'Doe',
              username: 'jane.doe',
              email: 'jane@example.com',
            },
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    const Probe = () => {
      const { status, isAuthenticated, accessToken, user } = useAuth()

      return (
        <>
          <span data-testid="status">{status}</span>
          <span data-testid="authenticated">{String(isAuthenticated)}</span>
          <span data-testid="token">{accessToken ?? 'none'}</span>
          <span data-testid="user">{user?.email ?? 'no-user'}</span>
          <span data-testid="roles">{user?.roles?.join(',') ?? 'none'}</span>
        </>
      )
    }

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('authenticated')
      expect(screen.getByTestId('token').textContent).toBe('restored-token')
      expect(screen.getByTestId('user').textContent).toBe('jane@example.com')
      expect(screen.getByTestId('roles').textContent).toBe('NORMAL_USER')
    })
  })

  it('ends unauthenticated on startup when refresh returns invalid session', async () => {
    const Probe = () => {
      const { status, isAuthenticated } = useAuth()

      return (
        <>
          <span data-testid="status">{status}</span>
          <span data-testid="authenticated">{String(isAuthenticated)}</span>
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
      expect(screen.getByTestId('authenticated').textContent).toBe('false')
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
