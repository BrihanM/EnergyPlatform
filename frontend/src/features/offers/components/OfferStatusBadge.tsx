import type { ReactElement } from 'react'
import type { OfferStatus } from '../types/offer.types'

export function OfferStatusBadge({ status }: { status: OfferStatus }): ReactElement {
  return <span className={`offer-status offer-status-${status.toLowerCase()}`}>{status}</span>
}
