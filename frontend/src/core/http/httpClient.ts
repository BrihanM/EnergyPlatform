import { getAccessToken } from '../auth/tokenStore';
import { environment } from '../config/environment';
import { HttpError } from './httpError';
import type {
  HttpErrorResponse,
  HttpRequestOptions,
  QueryParamCollection,
} from './types';

const appendQueryParam = (
  searchParams: URLSearchParams,
  key: string,
  value: QueryParamCollection,
): void => {
  if (value === undefined || value === null) {
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((entry) => {
      if (entry !== undefined && entry !== null) {
        searchParams.append(key, String(entry));
      }
    });
    return;
  }

  searchParams.append(key, String(value));
};

export const buildUrl = (
  path: string,
  params?: Record<string, QueryParamCollection>,
): string => {
  const normalizedBaseUrl = environment.apiBaseUrl.replace(/\/+$/, '');
  const targetUrl =
    path.startsWith('http://') || path.startsWith('https://')
      ? path
      : normalizedBaseUrl + (path.startsWith('/') ? path : '/' + path);

  const url = new URL(targetUrl);

  if (!params) {
    return url.toString();
  }

  Object.entries(params).forEach(([key, value]) => {
    appendQueryParam(url.searchParams, key, value);
  });

  return url.toString();
};

const parseResponseBody = async <TResponse>(response: Response): Promise<TResponse | undefined> => {
  const text = await response.text();

  if (!text) {
    return undefined;
  }

  try {
    return JSON.parse(text) as TResponse;
  } catch {
    return text as unknown as TResponse;
  }
};

const extractErrorMessage = (payload: unknown): string | undefined => {
  if (typeof payload === 'string') {
    return payload;
  }

  if (payload && typeof payload === 'object') {
    const record = payload as Record<string, unknown>;
    const directMessage = typeof record.message === 'string' ? record.message : undefined;

    if (directMessage) {
      return directMessage;
    }

    const details = typeof record.details === 'string' ? record.details : undefined;

    if (details) {
      return details;
    }

    const nestedError = record.error;

    if (nestedError && typeof nestedError === 'object') {
      const errorRecord = nestedError as Record<string, unknown>;
      const nestedMessage = typeof errorRecord.message === 'string' ? errorRecord.message : undefined;

      if (nestedMessage) {
        return nestedMessage;
      }
    }
  }

  return undefined;
};

export const normalizeHttpError = (payload: unknown, status: number): HttpError => {
  const record = typeof payload === 'object' && payload !== null ? (payload as Record<string, unknown>) : {};
  const code = typeof record.code === 'string' ? record.code : undefined;
  const correlationId = typeof record.correlationId === 'string' ? record.correlationId : undefined;

  return new HttpError({
    status,
    code,
    message: extractErrorMessage(payload) ?? ('Request failed with status ' + status),
    correlationId,
  });
};

const request = async <TResponse>(
  path: string,
  options: HttpRequestOptions = {},
): Promise<TResponse> => {
  const {
    method = 'GET',
    headers,
    params,
    body,
    signal,
    credentials = 'include',
  } = options;

  const requestHeaders = new Headers(headers ?? {});
  const requestUrl = buildUrl(path, params);
  const hasBody = body !== undefined && body !== null && !['GET', 'HEAD'].includes(method);
  const requestBody =
    hasBody && typeof body !== 'string'
      ? body instanceof FormData ||
          body instanceof URLSearchParams ||
          body instanceof Blob ||
          body instanceof ArrayBuffer
        ? body
        : JSON.stringify(body)
      : body;

  if (
    requestBody !== undefined &&
    requestBody !== null &&
    typeof requestBody !== 'string' &&
    !(requestBody instanceof FormData) &&
    !(requestBody instanceof URLSearchParams) &&
    !(requestBody instanceof Blob) &&
    !(requestBody instanceof ArrayBuffer) &&
    !requestHeaders.has('Content-Type')
  ) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  const accessToken = getAccessToken();

  if (accessToken && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', 'Bearer ' + accessToken);
  }

  const response = await fetch(requestUrl, {
    method,
    headers: requestHeaders,
    body: requestBody as BodyInit | null | undefined,
    credentials,
    signal,
  });

  if (response.status === 204 || response.status === 205) {
    return undefined as TResponse;
  }

  const payload = await parseResponseBody<TResponse>(response);

  if (!response.ok) {
    throw normalizeHttpError(payload as HttpErrorResponse | unknown, response.status);
  }

  return payload as TResponse;
};

export const httpClient = {
  request,
  get: <TResponse>(path: string, options: Omit<HttpRequestOptions, 'method' | 'body'> = {}) =>
    request<TResponse>(path, { ...options, method: 'GET' }),
  post: <TResponse>(path: string, options: HttpRequestOptions = {}) =>
    request<TResponse>(path, { ...options, method: 'POST' }),
  put: <TResponse>(path: string, options: HttpRequestOptions = {}) =>
    request<TResponse>(path, { ...options, method: 'PUT' }),
  patch: <TResponse>(path: string, options: HttpRequestOptions = {}) =>
    request<TResponse>(path, { ...options, method: 'PATCH' }),
  delete: <TResponse>(path: string, options: Omit<HttpRequestOptions, 'method' | 'body'> = {}) =>
    request<TResponse>(path, { ...options, method: 'DELETE' }),
};
