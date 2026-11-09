import { act, cleanup, fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpError } from '../../core/http/httpError'
import { PurchasePanel } from './components/PurchasePanel'
import { usePurchase } from './hooks/usePurchase'
import { HistoryPage } from './pages/HistoryPage'
import { TransactionPage } from './pages/TransactionPage'
import { clearPendingPurchase, getPendingPurchase, savePendingPurchase } from './services/pendingPurchase'
import { transactionsService } from './services/transactionsService'
import type { Transaction } from './types'
import type { EnergyOffer } from '../offers/types/offer.types'

const transaction: Transaction = {
  id: 'transaction-1', offerId: 'offer-1', sellerId: 'seller', buyerId: 'buyer',
  energyAmountKwh: 2, pricePerKwh: 3, totalAmount: 6, currency: 'COP',
  status: 'CONFIRMED', createdAt: '2026-01-01T00:00:00Z', completedAt: null,
  verificationResult: null, verificationReason: null, simulationReference: null, verifiedAt: null,
}
const offer: EnergyOffer = {
  id: 'offer-1', sellerId: 'seller', energyAmountKwh: '2', pricePerKwh: '3',
  status: 'ACTIVE', expiresAt: '2099-01-01T00:00:00Z',
  createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
}

beforeEach(() => {
  localStorage.clear()
  Object.defineProperty(navigator, 'locks', {
    configurable: true,
    value: { request: async (_name: string, _options: unknown, callback: (lock: object) => unknown) => callback({}) },
  })
})
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); localStorage.clear(); Reflect.deleteProperty(navigator, 'locks') })

describe('purchase', () => {
  it('confirms with the simulated purchase information', async () => {
    const request = vi.spyOn(transactionsService, 'purchase').mockResolvedValue(transaction)
    render(<PurchasePanel offer={offer} userId="buyer" />)
    fireEvent.click(screen.getByRole('button', { name: 'Buy offer' }))
    expect(screen.getByText(/academic simulation/i)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Confirm purchase' }))
    expect(await screen.findByText('Purchase confirmed')).toBeTruthy()
    expect(request).toHaveBeenCalledTimes(1)
    expect(getPendingPurchase('buyer')).toBeNull()
  })

  it('displays simulator rejection distinctly', async () => {
    vi.spyOn(transactionsService, 'purchase').mockResolvedValue({ ...transaction, status: 'REJECTED' })
    render(<PurchasePanel offer={offer} userId="buyer" />)
    fireEvent.click(screen.getByRole('button', { name: 'Buy offer' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirm purchase' }))
    expect(await screen.findByText('Rejected by simulated verification')).toBeTruthy()
  })

  it('retains the key while verification is pending', async () => {
    vi.spyOn(transactionsService, 'purchase').mockResolvedValue({ ...transaction, status: 'PENDING_VERIFICATION' })
    const { result } = renderHook(() => usePurchase('buyer'))
    await act(async () => { await result.current.purchase('offer-1') })
    expect(getPendingPurchase('buyer')).toMatchObject({ transactionId: transaction.id })
  })

  it('hides purchase for own and unavailable offers', () => {
    const { rerender } = render(<PurchasePanel offer={{ ...offer, sellerId: 'buyer' }} userId="buyer" />)
    expect(screen.queryByRole('button', { name: 'Buy offer' })).toBeNull()
    rerender(<PurchasePanel offer={{ ...offer, status: 'SOLD' }} userId="buyer" />)
    expect(screen.queryByRole('button', { name: 'Buy offer' })).toBeNull()
  })

  it.each([new Error('network'), new HttpError({ status: 400 }), new HttpError({ status: 403 })])(
    'retains the same idempotency key after uncertain or rejected requests',
    async (error) => {
      const request = vi.spyOn(transactionsService, 'purchase').mockRejectedValueOnce(error).mockResolvedValueOnce(transaction)
      const { result } = renderHook(() => usePurchase('buyer'))
      await act(async () => { await result.current.purchase('offer-1') })
      expect(result.current.message).toMatch(/result is unknown/i)
      const key = getPendingPurchase('buyer')?.key
      expect(key).toBeTruthy()
      await act(async () => { await result.current.purchase('offer-1') })
      expect(request.mock.calls[0][1]).toBe(key)
      expect(request.mock.calls[1][1]).toBe(key)
      expect(getPendingPurchase('buyer')).toBeNull()
    },
  )

  it('blocks duplicate in-flight requests and makes a new key for a new intention', async () => {
    let resolve!: (value: Transaction) => void
    const request = vi.spyOn(transactionsService, 'purchase')
      .mockImplementationOnce(() => new Promise((done) => { resolve = done }))
      .mockResolvedValueOnce(transaction)
    const { result } = renderHook(() => usePurchase('buyer'))
    let first!: Promise<Transaction | undefined>
    act(() => { first = result.current.purchase('offer-1'); void result.current.purchase('offer-1') })
    expect(request).toHaveBeenCalledTimes(1)
    await act(async () => { resolve(transaction); await first })
    await act(async () => { await result.current.purchase('offer-1') })
    expect(request.mock.calls[0][1]).not.toBe(request.mock.calls[1][1])
  })

  it('does not send a request if cross-tab locking is unavailable', async () => {
    Reflect.deleteProperty(navigator, 'locks')
    const request = vi.spyOn(transactionsService, 'purchase')
    const { result } = renderHook(() => usePurchase('buyer'))
    await act(async () => { await result.current.purchase('offer-1') })
    expect(request).not.toHaveBeenCalled()
    expect(result.current.message).toMatch(/cannot safely coordinate/i)
  })

  it('does not start a different intention while the previous outcome is unknown', async () => {
    const request = vi.spyOn(transactionsService, 'purchase').mockRejectedValue(new Error('offline'))
    const { result } = renderHook(() => usePurchase('buyer'))
    await act(async () => { await result.current.purchase('offer-1') })
    await act(async () => { await result.current.purchase('offer-2') })
    expect(request).toHaveBeenCalledTimes(1)
    expect(result.current.message).toMatch(/resolve your pending purchase/i)
  })

  it('recovers the same key after the purchase component is remounted', async () => {
    const request = vi.spyOn(transactionsService, 'purchase').mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(transaction)
    const first = renderHook(() => usePurchase('buyer'))
    await act(async () => { await first.result.current.purchase('offer-1') })
    const key = getPendingPurchase('buyer')?.key
    first.unmount()
    const second = renderHook(() => usePurchase('buyer'))
    await act(async () => { await second.result.current.purchase('offer-1') })
    expect(request.mock.calls[1][1]).toBe(key)
  })

  it('does not retry a resolved or replaced intention from a stale tab', async () => {
    const request = vi.spyOn(transactionsService, 'purchase')
    savePendingPurchase('buyer', { offerId: 'offer-1', key: 'original-key' })
    vi.spyOn(transactionsService, 'history').mockResolvedValue([])
    render(<HistoryPage userId="buyer" navigate={vi.fn()} />)
    await screen.findByRole('button', { name: 'Retry same operation' })

    clearPendingPurchase('buyer')
    fireEvent.click(screen.getByRole('button', { name: 'Retry same operation' }))
    expect(await screen.findByText(/no longer pending or has changed/i)).toBeTruthy()
    expect(request).not.toHaveBeenCalled()

    savePendingPurchase('buyer', { offerId: 'offer-1', key: 'replacement-key' })
    fireEvent.click(screen.getByRole('button', { name: 'Retry same operation' }))
    await waitFor(() => expect(request).not.toHaveBeenCalled())
    expect(getPendingPurchase('buyer')?.key).toBe('replacement-key')
  })

  it('does not retry from a stale purchase panel after another tab resolves it', async () => {
    const request = vi.spyOn(transactionsService, 'purchase').mockRejectedValueOnce(new Error('offline'))
    render(<PurchasePanel offer={offer} userId="buyer" />)
    fireEvent.click(screen.getByRole('button', { name: 'Buy offer' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirm purchase' }))
    await screen.findByText(/purchase result is unknown/i)
    clearPendingPurchase('buyer')
    fireEvent.click(screen.getByRole('button', { name: 'Retry same operation' }))
    expect(await screen.findByText(/no longer pending or has changed/i)).toBeTruthy()
    expect(request).toHaveBeenCalledTimes(1)
  })

  it('does not send a request while another tab owns the purchase lock', async () => {
    Object.defineProperty(navigator, 'locks', {
      configurable: true,
      value: { request: async (_name: string, _options: unknown, callback: (lock: null) => unknown) => callback(null) },
    })
    const request = vi.spyOn(transactionsService, 'purchase')
    const { result } = renderHook(() => usePurchase('buyer'))
    await act(async () => { await result.current.purchase('offer-1') })
    expect(request).not.toHaveBeenCalled()
    expect(result.current.message).toMatch(/another purchase/i)
  })
})

describe('history and detail', () => {
  it('shows buyer and seller roles and supports pagination', async () => {
    const history = vi.spyOn(transactionsService, 'history')
      .mockResolvedValueOnce(Array.from({ length: 20 }, (_, index) => ({ ...transaction, id: `id-${index}` })))
      .mockResolvedValueOnce([{ ...transaction, id: 'sale', buyerId: 'other', sellerId: 'buyer' }])
    render(<HistoryPage userId="buyer" navigate={vi.fn()} />)
    expect((await screen.findAllByText('Buyer')).length).toBe(20)
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(await screen.findByText('Seller')).toBeTruthy()
    expect(history).toHaveBeenLastCalledWith(20, 20)
  })

  it('shows an empty history and a server error', async () => {
    const history = vi.spyOn(transactionsService, 'history').mockResolvedValue([])
    const { unmount } = render(<HistoryPage userId="buyer" navigate={vi.fn()} />)
    expect(await screen.findByText(/No transactions/)).toBeTruthy()
    unmount()
    history.mockRejectedValue(new Error('server'))
    render(<HistoryPage userId="buyer" navigate={vi.fn()} />)
    expect((await screen.findByRole('alert')).textContent).toMatch(/could not be loaded/i)
  })

  it.each([403, 404, 500])('shows a safe detail error for %s', async (status) => {
    vi.spyOn(transactionsService, 'detail').mockRejectedValue(new HttpError({ status }))
    render(<TransactionPage id="id" userId="buyer" navigate={vi.fn()} />)
    expect((await screen.findByRole('alert')).textContent).toMatch(/unavailable or you do not have access/i)
  })

  it('loads transaction details', async () => {
    vi.spyOn(transactionsService, 'detail').mockResolvedValue(transaction)
    render(<TransactionPage id="id" userId="buyer" navigate={vi.fn()} />)
    expect(await screen.findByText('transaction-1')).toBeTruthy()
  })
})

describe('transaction HTTP contract', () => {
  it('sends the idempotency header and validates the response', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(transaction), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    await transactionsService.purchase('offer-1', 'intent-1')
    const options = fetchMock.mock.calls[0][1] as { headers: Headers; body: string }
    expect(options.headers.get('Idempotency-Key')).toBe('intent-1')
    expect(JSON.parse(options.body)).toEqual({ offerId: 'offer-1' })
  })

  it('rejects incomplete responses rather than displaying success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: '1' }), { status: 200 })))
    await expect(transactionsService.detail('1')).rejects.toThrow('Invalid transaction response')
  })

  it('uses backend offset pagination', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    await transactionsService.history(20, 20)
    await waitFor(() => expect(String(fetchMock.mock.calls[0][0])).toContain('offset=20&size=20'))
  })

  it('aborts an unanswered purchase after 30 seconds', async () => {
    vi.useFakeTimers()
    try {
      vi.stubGlobal('fetch', vi.fn((_url: string, options: { signal: AbortSignal }) =>
        new Promise((_resolve, reject) => {
          options.signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
        })))
      const request = transactionsService.purchase('offer-1', 'intent-1')
      const assertion = expect(request).rejects.toMatchObject({ name: 'AbortError' })
      await vi.advanceTimersByTimeAsync(30_000)
      await assertion
    } finally {
      vi.useRealTimers()
    }
  })
})
