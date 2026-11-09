import type { ReactElement } from 'react'
import type { Transaction } from '../types'

const labels = {
  CONFIRMED: 'Purchase confirmed',
  REJECTED: 'Rejected by simulated verification',
  PENDING_VERIFICATION: 'Verification in progress',
  CANCELLED: 'Cancelled',
}

export function TransactionDetails({ transaction, userId }: { transaction: Transaction; userId: string }): ReactElement {
  const role = transaction.buyerId === userId ? 'Buyer' : transaction.sellerId === userId ? 'Seller' : 'Unavailable'
  return <article className="detail-panel">
    <h2>{labels[transaction.status]}</h2>
    <dl className="offer-meta">
      <div><dt>Transaction</dt><dd>{transaction.id}</dd></div>
      <div><dt>Offer</dt><dd>{transaction.offerId}</dd></div>
      <div><dt>Your role</dt><dd>{role}</dd></div>
      <div><dt>Energy</dt><dd>{transaction.energyAmountKwh} kWh</dd></div>
      <div><dt>Price per kWh</dt><dd>{transaction.pricePerKwh} {transaction.currency}</dd></div>
      <div><dt>Total</dt><dd>{transaction.totalAmount} {transaction.currency}</dd></div>
      <div><dt>Created</dt><dd>{new Date(transaction.createdAt).toLocaleString()}</dd></div>
      {transaction.completedAt && <div><dt>Completed</dt><dd>{new Date(transaction.completedAt).toLocaleString()}</dd></div>}
      {transaction.verificationResult && <div><dt>Verification</dt><dd>{transaction.verificationResult}</dd></div>}
      {transaction.verifiedAt && <div><dt>Verified</dt><dd>{new Date(transaction.verifiedAt).toLocaleString()}</dd></div>}
      {transaction.simulationReference && <div><dt>Simulation reference</dt><dd>{transaction.simulationReference}</dd></div>}
    </dl>
  </article>
}
