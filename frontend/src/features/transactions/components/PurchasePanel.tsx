import { useState, type ReactElement } from 'react'
import type { EnergyOffer } from '../../offers/types/offer.types'
import { multiply } from '../../offers/utils/decimal'
import { usePurchase } from '../hooks/usePurchase'
import { TransactionDetails } from './TransactionDetails'

export function PurchasePanel({ offer, userId, onComplete }: { offer: EnergyOffer; userId: string; onComplete?: () => void }): ReactElement {
  const [confirming, setConfirming] = useState(false)
  const [expired, setExpired] = useState(false)
  const { purchase, busy, message, result } = usePurchase(userId)
  if (result) return <TransactionDetails transaction={result} userId={userId} />
  if (offer.sellerId === userId || offer.status !== 'ACTIVE') return <></>
  return <div>
    {expired && <p className="error-message">This offer has expired.</p>}
    {!confirming && <button className="primary-button" disabled={expired} onClick={() => {
      if (new Date(offer.expiresAt).getTime() <= Date.now()) { setExpired(true); return }
      setConfirming(true)
    }}>Buy offer</button>}
    {confirming && <div className="detail-panel" role="dialog" aria-label="Confirm purchase">
      <h2>Confirm purchase</h2>
      <p>{offer.energyAmountKwh} kWh at {offer.pricePerKwh} COP per kWh. Indicative total: {multiply(offer.energyAmountKwh, offer.pricePerKwh)} COP.</p>
      <p>This is an academic simulation. No payment or real energy transfer occurs.</p>
      {message && <p className="error-message" role="alert">{message}</p>}
      <button className="primary-button" disabled={busy} onClick={() => {
        if (!message && new Date(offer.expiresAt).getTime() <= Date.now()) { setExpired(true); setConfirming(false); return }
        void purchase(offer.id, { retryOnly: Boolean(message) }).then((value) => { if (value) onComplete?.() })
      }}>{busy ? 'Processing...' : message ? 'Retry same operation' : 'Confirm purchase'}</button>
      {!message && <button disabled={busy} onClick={() => setConfirming(false)}>Cancel</button>}
    </div>}
  </div>
}
