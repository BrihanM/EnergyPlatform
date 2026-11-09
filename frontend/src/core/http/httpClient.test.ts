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
      const response = await httpClient.post<{ success: boolean }>('/api/v1/auth/login', {
        body: {
          email: 'user@example.com',
          password: 'secret',
        },
      });

      expect(response).toEqual({ success: true });
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/auth/login'),
        expect.objectContaining({
          method: 'POST',
          credentials: 'include',
          headers: expect.any(Headers),
        }),
      );
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
    expect(response).toEqual({ success: true });
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/auth/login'),
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        headers: expect.any(Headers),
      }),
    );

    const fetchOptions = fetchMock.mock.calls[0][1] as { headers: Headers };
    expect(fetchOptions.headers.get('Content-Type')).toBe('application/json');
  });

  it('retries a protected request once after a successful refresh', async () => {
    let refreshCalls = 0;
    registerSessionHandlers({
      refreshSession: async () => {
        refreshCalls += 1;
        return { accessToken: 'new-token' };
      },
      clearSession: vi.fn(),
    });

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }));

    vi.stubGlobal('fetch', fetchMock);

    const response = await httpClient.get<{ ok: boolean }>('/api/v1/protected/resource', {
      requiresAuth: true,
    });

    expect(response).toEqual({ ok: true });
    expect(refreshCalls).toBe(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('does not auto-refresh for public requests', async () => {
    const refreshSession = vi.fn().mockResolvedValue({ accessToken: 'new-token' });
    registerSessionHandlers({
      refreshSession,
      clearSession: vi.fn(),
    });

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 })));

    await expect(
      httpClient.get('/api/v1/public/resource'),
    ).rejects.toBeInstanceOf(HttpError);

    expect(refreshSession).not.toHaveBeenCalled();
  });

  it('does not retry indefinitely when a protected request keeps returning 401', async () => {
    registerSessionHandlers({
      refreshSession: vi.fn().mockResolvedValue({ accessToken: 'new-token' }),
      clearSession: vi.fn(),
    });

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }));

    vi.stubGlobal('fetch', fetchMock);

    await expect(
      httpClient.get('/api/v1/protected/resource', { requiresAuth: true }),
    ).rejects.toBeInstanceOf(HttpError);

    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('uses a single refresh request for concurrent 401 protected calls', async () => {
    const releaseRefresh = Promise.resolve({ accessToken: 'new-token' });
    const refreshSession = vi.fn().mockImplementation(() => releaseRefresh);
    registerSessionHandlers({
      refreshSession,
      clearSession: vi.fn(),
    });

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 2 }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 3 }), { status: 200 }));

    vi.stubGlobal('fetch', fetchMock);

    const [first, second, third] = await Promise.all([
      httpClient.get<{ id: number }>('/api/v1/protected/a', { requiresAuth: true }),
      httpClient.get<{ id: number }>('/api/v1/protected/b', { requiresAuth: true }),
      httpClient.get<{ id: number }>('/api/v1/protected/c', { requiresAuth: true }),
    ]);

    expect(first.id).toBe(1);
    expect(second.id).toBe(2);
    expect(third.id).toBe(3);
    expect(refreshSession).toHaveBeenCalledTimes(1);
  });

  it('clears session for concurrent requests when refresh reports an invalid session', async () => {
    const clearSession = vi.fn();
    registerSessionHandlers({
      refreshSession: vi.fn().mockRejectedValue(new HttpError({ status: 401, message: 'Refresh expired' })),
      clearSession,
    });

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }));

    vi.stubGlobal('fetch', fetchMock);

    const [firstError, secondError] = await Promise.all([
      httpClient.get('/api/v1/protected/one', { requiresAuth: true }).catch((error) => error),
      httpClient.get('/api/v1/protected/two', { requiresAuth: true }).catch((error) => error),
    ]);

    expect(firstError).toBeInstanceOf(HttpError);
    expect(secondError).toBeInstanceOf(HttpError);
    expect(clearSession).toHaveBeenCalledTimes(1);
  });
});
