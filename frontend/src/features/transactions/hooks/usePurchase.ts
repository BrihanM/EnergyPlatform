import { useRef, useState } from 'react'
import { HttpError } from '../../../core/http/httpError'
import { clearPendingPurchase, getPendingPurchase, savePendingPurchase } from '../services/pendingPurchase'
import { transactionsService } from '../services/transactionsService'
import type { Transaction } from '../types'

const activeUsers = new Set<string>()

// The backend rejects these before claiming the idempotency key, so no transaction exists for the key.
const OFFER_UNAVAILABLE_MESSAGES = new Set(['Offer is no longer available', 'Offer is expired'])
const OFFER_UNAVAILABLE_NOTICE = 'The offer is no longer available, so no purchase was made. The pending purchase has been discarded.'
// Rejected before any transaction is created for the new key, so the local record can be cleared safely.
const definitiveRejectionNotice = (error: unknown): string | null => {
  if (!(error instanceof HttpError) || error.status !== 409) return null
  return OFFER_UNAVAILABLE_MESSAGES.has(error.message) ? OFFER_UNAVAILABLE_NOTICE : null
}

export function usePurchase(userId: string) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [result, setResult] = useState<Transaction | null>(null)
  const [discarded, setDiscarded] = useState(false)
  const [outcomeUnknown, setOutcomeUnknown] = useState(false)
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
    setDiscarded(false)
    setOutcomeUnknown(false)
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
        // Retrying the same offer reuses its idempotency key; a different offer gets a fresh key and never blocks.
        const pending = previous?.offerId === offerId ? previous : { offerId, key: crypto.randomUUID() }
        savePendingPurchase(userId, pending)
        attemptedKey.current = pending.key
        const transaction = await transactionsService.purchase(offerId, pending.key).catch((error: unknown) => {
          const notice = definitiveRejectionNotice(error)
          if (!notice) throw error
          clearPendingPurchase(userId)
          setDiscarded(true)
          setMessage(notice)
          return null
        })
        if (!transaction) return
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
      setOutcomeUnknown(true)
      setMessage(`${prefix}The purchase result is unknown. Check your history or retry this same operation; do not start a new purchase.`)
    } finally {
      inFlight.current = false
      activeUsers.delete(userId)
      setBusy(false)
    }
  }

  const reset = () => {
    attemptedKey.current = null
    setMessage(null)
    setDiscarded(false)
    setOutcomeUnknown(false)
  }

  return { purchase, reset, busy, message, result, discarded, outcomeUnknown }
}
