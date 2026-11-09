import { useEffect, useState, type ReactElement } from 'react'
import { transactionsService } from '../services/transactionsService'
import type { Transaction } from '../types'
import { TransactionDetails } from '../components/TransactionDetails'

export function TransactionPage({ id, userId, navigate }: { id: string; userId: string; navigate: (path: string) => void }): ReactElement {
  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  useEffect(() => {
    let active = true
    transactionsService.detail(id).then((value) => { if (active) setTransaction(value) })
      .catch(() => { if (active) setError(true) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])
  return <section>
    <button className="link-button" onClick={() => navigate('/transactions')}>Back to transactions</button>
    <h1>Transaction details</h1>
    {loading && <p className="state-message">Loading transaction...</p>}
    {error && <p className="error-message" role="alert">This transaction is unavailable or you do not have access.</p>}
    {transaction && <TransactionDetails transaction={transaction} userId={userId} />}
  </section>
}
