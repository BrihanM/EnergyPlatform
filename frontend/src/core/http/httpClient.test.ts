import { describe, expect, it, vi } from 'vitest'
import { buildUrl, httpClient, normalizeHttpError } from './httpClient'

describe('httpClient', () => {
  it('builds gateway URLs with query parameters', () => {
    expect(buildUrl('/api/v1/offers', { page: 1, size: 20 }))
      .toBe('http://localhost:8080/api/v1/offers?page=1&size=20')
  })

  it('normalizes safe backend error envelopes', () => {
    expect(normalizeHttpError({ code: 'OFFER_409', message: 'Offer is no longer editable' }, 409))
      .toMatchObject({ status: 409, code: 'OFFER_409', message: 'Offer is no longer editable' })
  })

  it('sends credentials and JSON payloads', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    try {
      await expect(httpClient.post('/api/v1/offers', { body: { energyAmountKwh: '1.000' } })).resolves.toEqual({ ok: true })
      const options = fetchMock.mock.calls[0][1] as { headers: Headers; credentials: string }
      expect(options.credentials).toBe('include')
      expect(options.headers.get('Content-Type')).toBe('application/json')
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
