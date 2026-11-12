import { httpClient } from '../../../core/http/httpClient'
import type { Transaction } from '../types'

const statuses = new Set(['PENDING_VERIFICATION', 'CONFIRMED', 'REJECTED', 'CANCELLED'])

function parseTransaction(value: unknown): Transaction {
  if (!value || typeof value !== 'object') throw new Error('Invalid transaction response')
  const record = value as Record<string, unknown>
  const stringFields = ['id', 'offerId', 'sellerId', 'buyerId', 'currency', 'status', 'createdAt']
  const numberFields = ['energyAmountKwh', 'pricePerKwh', 'totalAmount']
  const nullableFields = ['completedAt', 'verificationResult', 'verificationReason', 'simulationReference', 'verifiedAt']
  if (stringFields.some((field) => typeof record[field] !== 'string' || record[field] === '') ||
      numberFields.some((field) => typeof record[field] !== 'number' || !Number.isFinite(record[field])) ||
      nullableFields.some((field) => record[field] !== null && typeof record[field] !== 'string') ||
      typeof record.createdAt !== 'string' || Number.isNaN(Date.parse(record.createdAt)) ||
      (typeof record.completedAt === 'string' && Number.isNaN(Date.parse(record.completedAt))) ||
      (typeof record.verifiedAt === 'string' && Number.isNaN(Date.parse(record.verifiedAt))) ||
      !statuses.has(String(record.status)) ||
      (record.verificationResult !== null && record.verificationResult !== 'ACCEPTED' && record.verificationResult !== 'REJECTED')) {
    throw new Error('Invalid transaction response')
  }
  return value as Transaction
}

export const transactionsService = {
  purchase: async (offerId: string, key: string): Promise<Transaction> => {
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 30_000)
    try {
      return parseTransaction(await httpClient.post<unknown>('/api/v1/transactions', {
        body: { offerId }, headers: { 'Idempotency-Key': key }, requiresAuth: true, signal: controller.signal,
      }))
    } finally {
      clearTimeout(timeout)
    }
  },
  history: async (offset: number, size: number): Promise<Transaction[]> => {
    const response = await httpClient.get<unknown>('/api/v1/transactions/me', {
      params: { offset, size }, requiresAuth: true,
    })
    if (!Array.isArray(response)) throw new Error('Invalid transaction list')
    return response.map(parseTransaction)
  },
  complete: async (id: string): Promise<Transaction> =>
    parseTransaction(await httpClient.post<unknown>(`/api/v1/transactions/${encodeURIComponent(id)}/complete`, { requiresAuth: true })),
  detail: async (id: string): Promise<Transaction> =>
    parseTransaction(await httpClient.get<unknown>(`/api/v1/transactions/${encodeURIComponent(id)}`, { requiresAuth: true })),
}
