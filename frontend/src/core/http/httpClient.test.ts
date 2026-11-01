import { describe, expect, it, vi } from 'vitest';

import { buildUrl, httpClient, normalizeHttpError } from './httpClient';

describe('buildUrl', () => {
  it('builds a gateway URL and appends query parameters', () => {
    expect(
      buildUrl('/api/v1/auth/login', { page: 1, include: 'profile' }),
    ).toBe('http://localhost:8080/api/v1/auth/login?page=1&include=profile');
  });
});

describe('normalizeHttpError', () => {
  it('normalizes backend error payloads', () => {
    const error = normalizeHttpError(
      {
        code: 'AUTH_401',
        message: 'Invalid credentials',
        correlationId: 'corr-123',
      },
      401,
    );

    expect(error).toMatchObject({
      status: 401,
      code: 'AUTH_401',
      message: 'Invalid credentials',
      correlationId: 'corr-123',
    });
  });
});

describe('httpClient.request', () => {
  it('sends credentials and JSON payloads to the API Gateway', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    vi.stubGlobal('fetch', fetchMock);

    try {
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
      vi.unstubAllGlobals();
    }
  });
});
