export type OfferStatus = 'ACTIVE' | 'RESERVED' | 'SOLD' | 'CANCELLED' | 'EXPIRED'

export interface EnergyOffer {
  id: string
  sellerId: string
  energyAmountKwh: string
  pricePerKwh: string
  status: OfferStatus
  expiresAt: string
  createdAt: string
  updatedAt: string
}

export interface OfferPage {
  content: EnergyOffer[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface OfferPayload {
  energyAmountKwh: string
  pricePerKwh: string
  expiresAt: string
}

export interface OfferFilters {
  minEnergy?: string
  maxEnergy?: string
  minPrice?: string
  maxPrice?: string
  expiresBefore?: string
}
