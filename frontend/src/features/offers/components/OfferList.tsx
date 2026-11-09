import type { ReactElement } from 'react'
import type { EnergyOffer } from '../types/offer.types'
import { OfferCard } from './OfferCard'
export function OfferList({ offers, onSelect, renderActions }: { offers: EnergyOffer[]; onSelect?: (offer: EnergyOffer) => void; renderActions?: (offer: EnergyOffer) => ReactElement }): ReactElement {
  return <div className="offer-grid">{offers.map((offer) => <OfferCard key={offer.id} offer={offer} onSelect={() => onSelect?.(offer)} actions={renderActions?.(offer)} />)}</div>
}
