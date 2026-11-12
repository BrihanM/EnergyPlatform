import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { useAuth } from './core/auth'
import { transactionsService } from './features/transactions/services/transactionsService'
import { offersService } from './features/offers/services/offersService'

vi.mock('./core/auth', () => ({ useAuth: vi.fn() }))

const session = (roles: string[]) => ({
  status: 'authenticated' as const, isAuthenticated: true, accessToken: 'token',
  user: { id: 'buyer', roles }, setSession: vi.fn(), clearSession: vi.fn(), updateAccessToken: vi.fn(),
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  window.history.replaceState({}, '', '/')
})

describe('transaction routes', () => {
  it('loads history on direct navigation', async () => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER']))
    const history = vi.spyOn(transactionsService, 'history').mockResolvedValue([])
    window.history.replaceState({}, '', '/transactions')
    render(<App />)
    expect(await screen.findByText(/No transactions/)).toBeTruthy()
    expect(history).toHaveBeenCalledWith(0, 20)
  })

  it.each(['ADMIN', 'SECURITY_AUDITOR'])('does not expose history to %s', (role) => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER', role]))
    const history = vi.spyOn(transactionsService, 'history')
    window.history.replaceState({}, '', '/transactions')
    render(<App />)
    expect(screen.getByText(/authorized NORMAL_USER/i)).toBeTruthy()
    expect(history).not.toHaveBeenCalled()
  })

  it('loads detail by direct URL after refresh', async () => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER']))
    const detail = vi.spyOn(transactionsService, 'detail').mockRejectedValue(new Error('not found'))
    window.history.replaceState({}, '', '/transactions/example')
    render(<App />)
    expect((await screen.findByRole('alert')).textContent).toMatch(/unavailable/i)
    expect(detail).toHaveBeenCalledWith('example')
  })

  it.each(['ADMIN', 'SECURITY_AUDITOR'])('does not expose purchase to %s', async (role) => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER', role]))
    vi.spyOn(offersService, 'get').mockResolvedValue({
      id: 'offer', sellerId: 'seller', energyAmountKwh: '1', pricePerKwh: '2', status: 'ACTIVE',
      expiresAt: '2099-01-01T00:00:00Z', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    })
    window.history.replaceState({}, '', '/offers/offer')
    render(<App />)
    expect(await screen.findByText('Offer details')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Buy offer' })).toBeNull()
  })
})

describe('offer sub-routes', () => {
  it('opens My offers instead of treating "mine" as an offer id', async () => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER']))
    const mine = vi.spyOn(offersService, 'mine').mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 } as never)
    const detail = vi.spyOn(offersService, 'get')
    window.history.replaceState({}, '', '/offers/mine')
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'My offers' })).toBeTruthy()
    expect(mine).toHaveBeenCalled()
    expect(detail).not.toHaveBeenCalled()
  })

  it('opens Publish offer instead of treating "new" as an offer id', () => {
    vi.mocked(useAuth).mockReturnValue(session(['NORMAL_USER']))
    const detail = vi.spyOn(offersService, 'get')
    window.history.replaceState({}, '', '/offers/new')
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Publish an offer' })).toBeTruthy()
    expect(detail).not.toHaveBeenCalled()
  })
})
