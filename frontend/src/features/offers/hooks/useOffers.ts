/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { offersService } from '../services/offersService'
import type { EnergyOffer, OfferFilters, OfferPage } from '../types/offer.types'

export function useOffers(filters: OfferFilters) {
  const [result, setResult] = useState<OfferPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  const reload = useCallback(async () => {
    setLoading(true); setError(null)
    try { setResult(await offersService.list(page, 20, filters)) } catch (caught) { setError(caught) } finally { setLoading(false) }
  }, [filters, page])

  useEffect(() => { void reload() }, [reload])
  return { result, page, setPage, loading, error, reload }
}

export function useMyOffers(status?: string) {
  const [result, setResult] = useState<OfferPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const reload = useCallback(async () => {
    setLoading(true); setError(null)
    try { setResult(await offersService.mine(0, 100, status)) } catch (caught) { setError(caught) } finally { setLoading(false) }
  }, [status])
  useEffect(() => { void reload() }, [reload])
  return { result, loading, error, reload }
}

export function useOffer(id: string) {
  const [offer, setOffer] = useState<EnergyOffer | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  useEffect(() => {
    let active = true
    setLoading(true)
    offersService.get(id).then((value) => { if (active) setOffer(value) })
      .catch((caught) => { if (active) setError(caught) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])
  return { offer, loading, error }
}
