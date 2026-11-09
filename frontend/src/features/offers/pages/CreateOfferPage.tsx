import { useState, type ReactElement } from 'react'
import { useAuth } from '../../../core/auth'
import { offersService } from '../services/offersService'
import { OfferForm } from '../components/OfferForm'
export function CreateOfferPage({ navigate }: { navigate: (path: string) => void }): ReactElement {
  const { isAuthenticated } = useAuth(); const [busy, setBusy] = useState(false); const [message, setMessage] = useState<string | null>(null)
  if (!isAuthenticated) return <p className="error-message">Sign in with a NORMAL_USER account to publish offers.</p>
  return <section className="narrow-page"><p className="eyebrow">Energy market</p><h1>Publish an offer</h1><p className="muted">Your identity is taken from the authenticated session.</p>{message && <p className="state-message">{message}</p>}<OfferForm submitLabel="Publish offer" disabled={busy} onSubmit={async (payload) => { setBusy(true); setMessage(null); try { const offer = await offersService.create(payload); setMessage('Offer published successfully.'); navigate(`/offers/${offer.id}`) } catch (error) { setMessage(error instanceof Error ? error.message : 'The offer could not be published.') } finally { setBusy(false) } }} /></section>
}
