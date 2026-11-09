import { afterEach, expect, it, vi } from 'vitest'
import { offersService } from './offersService'

afterEach(() => vi.unstubAllGlobals())

it('normalizes numeric backend offer amounts into the existing frontend contract', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
    id: 'offer', sellerId: 'seller', energyAmountKwh: 1.25, pricePerKwh: 42.5,
    status: 'ACTIVE', expiresAt: '2027-01-01T00:00:00Z',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
  }), { status: 200 })))
  const offer = await offersService.get('offer')
  expect(offer.energyAmountKwh).toBe('1.25')
  expect(offer.pricePerKwh).toBe('42.5')
})
