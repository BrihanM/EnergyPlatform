import type { ReactElement } from 'react'
import type { EnergyOffer } from '../types/offer.types'
import { OfferStatusBadge } from './OfferStatusBadge'
import { multiply } from '../utils/decimal'

const date = (value: string) => new Date(value).toLocaleString()
const money = (value: string) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 0,
}).format(BigInt(value.split('.')[0] || '0'))
export function OfferCard({ offer, onSelect, actions }: { offer: EnergyOffer; onSelect?: () => void; actions?: ReactElement }): ReactElement {
  return <article className="offer-card">
    <div className="offer-card-header"><OfferStatusBadge status={offer.status} /><button className="link-button" onClick={onSelect}>View details</button></div>
    <h3>{offer.energyAmountKwh} kWh</h3>
    <p className="offer-price">{money(offer.pricePerKwh)} / kWh</p>
    <p className="muted">Indicative total: {money(multiply(offer.energyAmountKwh, offer.pricePerKwh))}</p>
    <dl className="offer-meta"><div><dt>Published</dt><dd>{date(offer.createdAt)}</dd></div><div><dt>Expires</dt><dd>{date(offer.expiresAt)}</dd></div></dl>
    {actions}
  </article>
}
