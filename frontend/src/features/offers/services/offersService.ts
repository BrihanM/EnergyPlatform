import { httpClient } from '../../../core/http/httpClient'
import type { EnergyOffer, OfferFilters, OfferPage, OfferPayload } from '../types/offer.types'

const pageParams = (page: number, size: number, filters: OfferFilters = {}) => ({
  page, size, sort: 'createdAt', ...filters,
})

type ApiOffer = Omit<EnergyOffer, 'energyAmountKwh' | 'pricePerKwh'> & {
  energyAmountKwh: number | string
  pricePerKwh: number | string
}
const normalize = (offer: ApiOffer): EnergyOffer => ({
  ...offer, energyAmountKwh: String(offer.energyAmountKwh), pricePerKwh: String(offer.pricePerKwh),
})
const normalizePage = (page: Omit<OfferPage, 'content'> & { content: ApiOffer[] }): OfferPage => ({
  ...page, content: page.content.map(normalize),
})

export const offersService = {
  list: (page = 0, size = 20, filters?: OfferFilters) =>
    httpClient.get<Omit<OfferPage, 'content'> & { content: ApiOffer[] }>('/api/v1/offers', { params: pageParams(page, size, filters) }).then(normalizePage),
  get: (id: string) => httpClient.get<ApiOffer>(`/api/v1/offers/${id}`).then(normalize),
  mine: (page = 0, size = 20, status?: string) =>
    httpClient.get<Omit<OfferPage, 'content'> & { content: ApiOffer[] }>('/api/v1/offers/me', { params: { page, size, sort: 'createdAt', status } }).then(normalizePage),
  create: (payload: OfferPayload) =>
    httpClient.post<ApiOffer>('/api/v1/offers', { body: payload }).then(normalize),
  update: (id: string, payload: OfferPayload) =>
    httpClient.put<ApiOffer>(`/api/v1/offers/${id}`, { body: payload }).then(normalize),
  cancel: (id: string) =>
    httpClient.delete<ApiOffer>(`/api/v1/offers/${id}`).then(normalize),
}
