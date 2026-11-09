import { useState, type FormEvent, type ReactElement } from 'react'
import type { OfferPayload } from '../types/offer.types'

const initial = (offer?: Partial<OfferPayload>): OfferPayload => ({ energyAmountKwh: offer?.energyAmountKwh ?? '', pricePerKwh: offer?.pricePerKwh ?? '', expiresAt: offer?.expiresAt?.slice(0, 16) ?? '' })
export function OfferForm({ initialValue, submitLabel, onSubmit, disabled }: { initialValue?: Partial<OfferPayload>; submitLabel: string; onSubmit: (payload: OfferPayload) => Promise<void>; disabled?: boolean }): ReactElement {
  const [form, setForm] = useState(() => initial(initialValue))
  const [error, setError] = useState<string | null>(null)
  const update = (name: keyof OfferPayload, value: string) => setForm((current) => ({ ...current, [name]: value }))
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(null)
    if (!form.energyAmountKwh || Number(form.energyAmountKwh) <= 0 || !/^\d+(\.\d{1,3})?$/.test(form.energyAmountKwh)) return setError('Energy must be positive with up to 3 decimals.')
    if (!form.pricePerKwh || Number(form.pricePerKwh) <= 0 || !/^\d+(\.\d{1,2})?$/.test(form.pricePerKwh)) return setError('Price must be positive with up to 2 decimals.')
    const expiresAt = new Date(form.expiresAt)
    if (!form.expiresAt || Number.isNaN(expiresAt.getTime()) || expiresAt <= new Date()) return setError('Expiration must be a future date.')
    await onSubmit({ ...form, expiresAt: expiresAt.toISOString() })
  }
  return <form className="offer-form" onSubmit={(event) => void submit(event)}>
    <label>Energy (kWh)<input type="text" inputMode="decimal" value={form.energyAmountKwh} onChange={(event) => update('energyAmountKwh', event.target.value)} required /></label>
    <label>Price per kWh (COP)<input type="text" inputMode="decimal" value={form.pricePerKwh} onChange={(event) => update('pricePerKwh', event.target.value)} required /></label>
    <label>Expiration<input type="datetime-local" value={form.expiresAt} onChange={(event) => update('expiresAt', event.target.value)} required /></label>
    {error && <p className="error-message">{error}</p>}
    <button className="primary-button" disabled={disabled} type="submit">{disabled ? 'Saving...' : submitLabel}</button>
  </form>
}
