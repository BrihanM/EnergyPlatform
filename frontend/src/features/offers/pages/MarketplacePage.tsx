import { useState, type ReactElement } from 'react'
import { useOffers } from '../hooks/useOffers'
import type { OfferFilters } from '../types/offer.types'
import { OfferFilters as Filters } from '../components/OfferFilters'
import { OfferList } from '../components/OfferList'
export function MarketplacePage({ navigate }: { navigate: (path: string) => void }): ReactElement {
  const [filters, setFilters] = useState<OfferFilters>({})
  const { result, page, setPage, loading, error } = useOffers(filters)
  return <section><div className="page-heading"><div><p className="eyebrow">Energy market</p><h1>Marketplace</h1><p className="muted">Browse active, available surplus energy offers.</p></div></div><Filters value={filters} onChange={(value) => { setFilters(value); setPage(0) }} />{loading && <p className="state-message">Loading offers...</p>}{Boolean(error) && <p className="error-message">Unable to load offers. Please try again.</p>}{!loading && !error && result?.content.length === 0 && <p className="state-message">No offers match the selected filters.</p>}{result && <OfferList offers={result.content} onSelect={(offer) => navigate(`/offers/${offer.id}`)} />}<div className="pagination"><button disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Previous</button><span>Page {page + 1} of {Math.max(result?.totalPages ?? 1, 1)}</span><button disabled={loading || !result || page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button></div></section>
}
