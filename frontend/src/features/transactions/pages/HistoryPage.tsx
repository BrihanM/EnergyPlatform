import { useEffect, useState, type ReactElement } from 'react'
import { usePurchase } from '../hooks/usePurchase'
import { clearPendingPurchase, getPendingPurchase, type PendingPurchase } from '../services/pendingPurchase'
import { transactionsService } from '../services/transactionsService'
import type { Transaction } from '../types'
import { TransactionDetails } from '../components/TransactionDetails'

const pageSize = 20

const readPendingState = (userId: string): { pending: PendingPurchase | null; error: boolean } => {
  try { return { pending: getPendingPurchase(userId), error: false } }
  catch { return { pending: null, error: true } }
}

export function HistoryPage({ userId, navigate }: { userId: string; navigate: (path: string) => void }): ReactElement {
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const [revision, setRevision] = useState(0)
  const [completingId, setCompletingId] = useState<string | null>(null)
  const [completeError, setCompleteError] = useState<string | null>(null)
  const { pending: pendingPurchase, error: pendingError } = readPendingState(userId)
  const { purchase, reset, busy, message, result } = usePurchase(userId)

  useEffect(() => {
    let active = true
    transactionsService.history(page * pageSize, pageSize).then((value) => {
      if (active) { setItems(value); setError(false) }
    }).catch(() => { if (active) setError(true) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, result, attempt, revision])

  const changePage = (next: number) => { setLoading(true); setItems([]); setPage(next) }

  const discardPending = () => {
    clearPendingPurchase(userId)
    reset()
    setRevision((value) => value + 1)
  }

  const completeTransaction = async (transaction: Transaction) => {
    setCompletingId(transaction.id)
    setCompleteError(null)
    try {
      await transactionsService.complete(transaction.id)
      if (readPendingState(userId).pending?.transactionId === transaction.id) clearPendingPurchase(userId)
      setRevision((value) => value + 1)
    } catch {
      setCompleteError('The transaction could not be completed. Try again.')
    } finally {
      setCompletingId(null)
    }
  }

  return <section>
    <h1>My transactions</h1>
    {pendingError && <p className="error-message" role="alert">Pending purchase data could not be read. Do not start a new purchase until it can be recovered.</p>}
    {pendingPurchase && !result && <div className="detail-panel">
      <h2>Purchase awaiting resolution</h2>
      <p>Offer {pendingPurchase.offerId}. Check the history below, retry the same operation, or discard it if no transaction exists for it.</p>
      {pendingPurchase.transactionId && <button onClick={() => navigate(`/transactions/${encodeURIComponent(pendingPurchase.transactionId!)}`)}>View pending transaction</button>}
      <button disabled={busy} onClick={() => void purchase(pendingPurchase.offerId, { retryOnly: true, expectedKey: pendingPurchase.key })}>{busy ? 'Processing...' : 'Retry same operation'}</button>
      <button disabled={busy} onClick={discardPending}>Discard pending purchase</button>
    </div>}
    {message && <p className="error-message" role="alert">{message}</p>}
    {result && <TransactionDetails transaction={result} userId={userId} />}
    {loading && <p className="state-message">Loading transactions...</p>}
    {error && <p className="error-message" role="alert">Transactions could not be loaded. Please try again.</p>}
    {error && <button onClick={() => { setError(false); setLoading(true); setAttempt((value) => value + 1) }}>Retry loading history</button>}
    {completeError && <p className="error-message" role="alert">{completeError}</p>}
    {!loading && !error && items.length === 0 && <p className="state-message">No transactions on this page.</p>}
    {!loading && !error && items.map((item) => <div key={item.id}>
      {item.status === 'PENDING_VERIFICATION' && <>
        <p className="state-message" role="status">In progress</p>
        <button disabled={completingId !== null} onClick={() => void completeTransaction(item)}>{completingId === item.id ? 'Completing...' : 'Complete transaction'}</button>
      </>}
      <TransactionDetails transaction={item} userId={userId} />
      <button onClick={() => navigate(`/transactions/${item.id}`)}>View transaction details</button>
    </div>)}
    <div className="pagination">
      <button disabled={page === 0 || loading} onClick={() => changePage(page - 1)}>Previous</button>
      <span>Page {page + 1}</span>
      <button disabled={loading || error || items.length < pageSize} onClick={() => changePage(page + 1)}>Next</button>
    </div>
  </section>
}
