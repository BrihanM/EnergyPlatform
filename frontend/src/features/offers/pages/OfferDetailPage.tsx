import type { ReactElement } from 'react'
import { useOffer } from '../hooks/useOffers'
import { OfferCard } from '../components/OfferCard'
export function OfferDetailPage({ id, navigate }: { id: string; navigate: (path: string) => void }): ReactElement {
  const { offer, loading, error } = useOffer(id)
  return <section><button className="link-button" onClick={() => navigate('/')}>← Back to marketplace</button>{loading && <p className="state-message">Loading offer...</p>}{Boolean(error) && <p className="error-message">This offer is unavailable or could not be loaded.</p>}{offer && <div className="detail-panel"><h1>Offer details</h1><OfferCard offer={offer} /></div>}</section>
}
