export type TransactionStatus = 'PENDING_VERIFICATION' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED'

export interface Transaction {
  id: string
  offerId: string
  sellerId: string
  buyerId: string
  energyAmountKwh: number
  pricePerKwh: number
  totalAmount: number
  currency: string
  status: TransactionStatus
  createdAt: string
  completedAt: string | null
  verificationResult: 'ACCEPTED' | 'REJECTED' | null
  verificationReason: string | null
  simulationReference: string | null
  verifiedAt: string | null
}
