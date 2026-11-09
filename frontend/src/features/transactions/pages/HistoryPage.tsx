import { useEffect, useState, type ReactElement } from 'react'
import { usePurchase } from '../hooks/usePurchase'
import { getPendingPurchase, type PendingPurchase } from '../services/pendingPurchase'
import { transactionsService } from '../services/transactionsService'
import type { Transaction } from '../types'
import { TransactionDetails } from '../components/TransactionDetails'

const pageSize = 20

export function HistoryPage({ userId, navigate }: { userId: string; navigate: (path: string) => void }): ReactElement {
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const [pendingState] = useState<{ pending: PendingPurchase | null; error: boolean }>(() => {
    try { return { pending: getPendingPurchase(userId), error: false } }
    catch { return { pending: null, error: true } }
  })
  const { purchase, busy, message, result } = usePurchase(userId)

  useEffect(() => {
    let active = true
    transactionsService.history(page * pageSize, pageSize).then((value) => {
      if (active) { setItems(value); setError(false) }
    }).catch(() => { if (active) setError(true) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, result, attempt])

  const changePage = (next: number) => { setLoading(true); setItems([]); setPage(next) }
  return <section>
    <h1>My transactions</h1>
    {pendingState.error && <p className="error-message" role="alert">Pending purchase data could not be read. Do not start a new purchase until it can be recovered.</p>}
    {pendingState.pending && !result && <div className="detail-panel">
      <h2>Purchase awaiting resolution</h2>
      <p>Offer {pendingState.pending.offerId}. Check the history below or retry the same operation.</p>
      {pendingState.pending.transactionId && <button onClick={() => navigate(`/transactions/${encodeURIComponent(pendingState.pending!.transactionId!)}`)}>View pending transaction</button>}
      {message && <p className="error-message" role="alert">{message}</p>}
      <button disabled={busy} onClick={() => void purchase(pendingState.pending!.offerId, { retryOnly: true, expectedKey: pendingState.pending!.key })}>{busy ? 'Processing...' : 'Retry same operation'}</button>
    </div>}
    {result && <TransactionDetails transaction={result} userId={userId} />}
    {loading && <p className="state-message">Loading transactions...</p>}
    {error && <p className="error-message" role="alert">Transactions could not be loaded. Please try again.</p>}
    {error && <button onClick={() => { setError(false); setLoading(true); setAttempt((value) => value + 1) }}>Retry loading history</button>}
    {!loading && !error && items.length === 0 && <p className="state-message">No transactions on this page.</p>}
    {!loading && !error && items.map((item) => <div key={item.id}>
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
