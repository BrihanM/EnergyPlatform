import { useRef, useState } from 'react'
import { HttpError } from '../../../core/http/httpError'
import { clearPendingPurchase, getPendingPurchase, savePendingPurchase } from '../services/pendingPurchase'
import { transactionsService } from '../services/transactionsService'
import type { Transaction } from '../types'

const activeUsers = new Set<string>()

export function usePurchase(userId: string) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [result, setResult] = useState<Transaction | null>(null)
  const inFlight = useRef(false)
  const attemptedKey = useRef<string | null>(null)

  const purchase = async (offerId: string, options?: { retryOnly?: boolean; expectedKey?: string }) => {
    if (inFlight.current) return
    if (activeUsers.has(userId)) {
      setMessage('Another purchase is being processed. Check your transaction history before retrying.')
      return
    }
    inFlight.current = true
    activeUsers.add(userId)
    setBusy(true)
    setMessage(null)
    try {
      if (!navigator.locks) {
        setMessage('This browser cannot safely coordinate purchases across tabs. No request was sent.')
        return
      }
      return await navigator.locks.request(`energy-purchase:${userId}`, { ifAvailable: true }, async (lock) => {
        if (!lock) {
          setMessage('Another purchase is being processed in this browser. Check your transaction history before retrying.')
          return
        }
        const previous = getPendingPurchase(userId)
        const expectedKey = options?.expectedKey ?? attemptedKey.current
        if (options?.retryOnly && (!previous || (expectedKey && previous.key !== expectedKey))) {
          setMessage('This purchase is no longer pending or has changed. Refresh your transaction history before taking further action.')
          return
        }
        if (previous && previous.offerId !== offerId) {
          setMessage('Resolve your pending purchase before starting another one. Go to transaction history to retry it.')
          return
        }
        const pending = previous ?? { offerId, key: crypto.randomUUID() }
        savePendingPurchase(userId, pending)
        attemptedKey.current = pending.key
        const transaction = await transactionsService.purchase(offerId, pending.key)
        if (transaction.offerId !== offerId || transaction.buyerId !== userId) {
          throw new Error('Unexpected transaction response')
        }
        if (transaction.status === 'PENDING_VERIFICATION') {
          savePendingPurchase(userId, { ...pending, transactionId: transaction.id })
        } else {
          clearPendingPurchase(userId)
        }
        setResult(transaction)
        return transaction
      })
    } catch (error) {
      const prefix = error instanceof HttpError && error.status === 403
        ? 'You are not authorized to complete this request. '
        : error instanceof HttpError && error.status === 400
          ? 'The request could not be validated. '
          : ''
      setMessage(`${prefix}The purchase result is unknown. Check your history or retry this same operation; do not start a new purchase.`)
    } finally {
      inFlight.current = false
      activeUsers.delete(userId)
      setBusy(false)
    }
  }
  return { purchase, busy, message, result }
}
