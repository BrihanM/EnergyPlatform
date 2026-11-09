export interface PendingPurchase {
  offerId: string
  key: string
  transactionId?: string
}

const storageKey = (userId: string) => `energy-pending-purchase:${userId}`

export function getPendingPurchase(userId: string): PendingPurchase | null {
  const raw = localStorage.getItem(storageKey(userId))
  if (raw === null) return null
  const value: unknown = JSON.parse(raw)
  if (!value || typeof value !== 'object' ||
      !('offerId' in value) || typeof value.offerId !== 'string' ||
      !('key' in value) || typeof value.key !== 'string' ||
      ('transactionId' in value && typeof value.transactionId !== 'string') ||
      !value.offerId || !/^[A-Za-z0-9._:-]{1,100}$/.test(value.key)) {
    throw new Error('Pending purchase data is invalid')
  }
  const transactionId = 'transactionId' in value && typeof value.transactionId === 'string' ? value.transactionId : undefined
  return { offerId: value.offerId, key: value.key, ...(transactionId ? { transactionId } : {}) }
}

export function savePendingPurchase(userId: string, pending: PendingPurchase): void {
  localStorage.setItem(storageKey(userId), JSON.stringify(pending))
}

export function clearPendingPurchase(userId: string): void {
  localStorage.removeItem(storageKey(userId))
}
