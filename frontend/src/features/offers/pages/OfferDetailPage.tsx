import { useState, type ReactElement } from 'react'
import { useOffer } from '../hooks/useOffers'
import { OfferCard } from '../components/OfferCard'
import { useAuth } from '../../../core/auth'
import { PurchasePanel } from '../../transactions/components/PurchasePanel'
export function OfferDetailPage({ id, navigate }: { id: string; navigate: (path: string) => void }): ReactElement {
  const { offer, loading, error } = useOffer(id)
  const [resolved, setResolved] = useState(false)
  const { isAuthenticated, user } = useAuth()
  const canPurchase = isAuthenticated && user?.id && user.roles?.includes('NORMAL_USER') && !user.roles.some((role) => role === 'ADMIN' || role === 'SECURITY_AUDITOR')
  return <section><button className="link-button" onClick={() => navigate('/')}>← Back to marketplace</button>{loading && <p className="state-message">Loading offer...</p>}{Boolean(error) && <p className="error-message">This offer is unavailable or could not be loaded.</p>}{offer && <div className="detail-panel"><h1>Offer details</h1>{!resolved && <OfferCard offer={offer} />}{canPurchase && <PurchasePanel offer={offer} userId={user.id!} onComplete={() => setResolved(true)} />}</div>}</section>
}
