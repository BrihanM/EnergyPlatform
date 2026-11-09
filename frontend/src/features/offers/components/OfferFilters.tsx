import type { ReactElement } from 'react'
import type { OfferFilters } from '../types/offer.types'
export function OfferFilters({ value, onChange }: { value: OfferFilters; onChange: (value: OfferFilters) => void }): ReactElement {
  const field = (name: keyof OfferFilters, label: string) => <label>{label}<input type="number" min="0" step="any" value={value[name] ?? ''} onChange={(event) => onChange({ ...value, [name]: event.target.value || undefined })} /></label>
  return <div className="filters">{field('minEnergy', 'Min energy')}{field('maxEnergy', 'Max energy')}{field('minPrice', 'Min price')}{field('maxPrice', 'Max price')}</div>
}
