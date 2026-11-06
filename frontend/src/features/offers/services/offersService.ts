import { httpClient } from '../../../core/http/httpClient'
import type { EnergyOffer, OfferFilters, OfferPage, OfferPayload } from '../types/offer.types'

const pageParams = (page: number, size: number, filters: OfferFilters = {}) => ({
  page, size, sort: 'createdAt', ...filters,
})

export const offersService = {
  list: (page = 0, size = 20, filters?: OfferFilters) =>
    httpClient.get<OfferPage>('/api/v1/offers', { params: pageParams(page, size, filters) }),
  get: (id: string) => httpClient.get<EnergyOffer>(`/api/v1/offers/${id}`),
  mine: (page = 0, size = 20, status?: string) =>
    httpClient.get<OfferPage>('/api/v1/offers/me', { params: { page, size, sort: 'createdAt', status } }),
  create: (payload: OfferPayload) =>
    httpClient.post<EnergyOffer>('/api/v1/offers', { body: payload }),
  update: (id: string, payload: OfferPayload) =>
    httpClient.put<EnergyOffer>(`/api/v1/offers/${id}`, { body: payload }),
  cancel: (id: string) =>
    httpClient.delete<EnergyOffer>(`/api/v1/offers/${id}`),
}
