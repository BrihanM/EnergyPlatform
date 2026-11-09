import { getAccessToken } from '../auth/tokenStore';
import { refreshSessionSingleFlight } from '../auth/sessionRuntime';
import { environment } from '../config/environment';
import { HttpError } from './httpError';
import type {
  HttpErrorResponse,
  HttpRequestOptions,
  QueryParamCollection,
} from './types';

/**
 * Appends a query parameter to the URL search parameters.
 * Handles arrays by appending multiple values for the same key.
 * Skips null and undefined values.
 * 
 * @param {URLSearchParams} searchParams - The search parameters object to modify.
 * @param {string} key - The parameter key.
 * @param {QueryParamCollection} value - The parameter value(s).
 * @returns {void}
 */
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

/**
 * Constructs a complete URL with base URL, path, and query parameters.
 * Automatically handles URL normalization and parameter serialization.
 * 
 * @template TResponse - The expected response type.
 * @param {string} path - The API endpoint path or full URL.
 * @param {Record<string, QueryParamCollection>} [params] - Query parameters to append to the URL.
 * @returns {string} The complete, properly formatted URL.
 * @example
 * buildUrl('/users', { id: 123, status: 'active' })
 * // Returns: 'http://localhost:8080/users?id=123&status=active'
 */
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

/**
 * Parses the response body as JSON or plain text.
 * Returns undefined for empty responses.
 * 
 * @template TResponse - The expected response type.
 * @param {Response} response - The fetch Response object.
 * @returns {Promise<TResponse | undefined>} The parsed response body.
 */
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

/**
 * Extracts an error message from various payload formats.
 * Handles plain strings, objects with message/details fields, and nested error objects.
 * 
 * @param {unknown} payload - The error payload from the server.
 * @returns {string | undefined} The extracted error message or undefined.
 */
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

/**
 * Normalizes an error payload into a structured HttpError object.
 * Extracts status, code, message, and correlation ID from the error response.
 * 
 * @param {unknown} payload - The error response payload.
 * @param {number} status - The HTTP status code.
 * @returns {HttpError} A normalized HttpError instance.
 */
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

/**
 * Executes an HTTP request with automatic auth header injection and error handling.
 * Supports various body types and automatically sets appropriate Content-Type headers.
 * 
 * @template TResponse - The expected response type.
 * @param {string} path - The API endpoint path or full URL.
 * @param {HttpRequestOptions} [options] - Request configuration options.
 * @returns {Promise<TResponse>} The parsed response data.
 * @throws {HttpError} If the request fails with a non-2xx status.
 * @example
 * const data = await request<User>('/api/users/1');
 * const created = await request<User>('/api/users', { method: 'POST', body: { name: 'John' } });
 */
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
    requiresAuth = false,
    skipAuthRefresh = false,
    retryAfterRefreshAttempted = false,
  } = options;

  const requestHeaders = new Headers(headers ?? {});
  const requestUrl = buildUrl(path, params);
  const hasBody = body !== undefined && body !== null && !['GET', 'HEAD'].includes(method);
  const isBinaryLikeBody =
    body instanceof FormData ||
    body instanceof URLSearchParams ||
    body instanceof Blob ||
    body instanceof ArrayBuffer;
  const requestBody =
    hasBody && typeof body !== 'string'
      ? isBinaryLikeBody
        ? body
        : JSON.stringify(body)
      : body;

  if (
    requestBody !== undefined &&
    requestBody !== null &&
    !isBinaryLikeBody &&
    typeof body !== 'string' &&
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
    if (response.status === 401 && requiresAuth && !skipAuthRefresh) {
      if (!retryAfterRefreshAttempted) {
        const refreshed = await refreshSessionSingleFlight('unauthorized');

        if (refreshed) {
          return request<TResponse>(path, {
            ...options,
            retryAfterRefreshAttempted: true,
          });
        }
      }
    }

    throw normalizeHttpError(payload as HttpErrorResponse | unknown, response.status);
  }

  return payload as TResponse;
};

/**
 * Main HTTP client interface providing methods for all common HTTP operations.
 * Automatically injects authentication tokens, handles errors, and manages headers.
 * 
 * @typedef {Object} httpClient
 * @property {Function} request - Generic request method accepting any HTTP method.
 * @property {Function} get - Performs a GET request.
 * @property {Function} post - Performs a POST request.
 * @property {Function} put - Performs a PUT request.
 * @property {Function} patch - Performs a PATCH request.
 * @property {Function} delete - Performs a DELETE request.
 * 
 * @example
 * // GET request
 * const user = await httpClient.get<User>('/api/users/1');
 * 
 * // POST request with body
 * const created = await httpClient.post<User>('/api/users', { body: { name: 'John' } });
 * 
 * // Request with query parameters
 * const users = await httpClient.get<User[]>('/api/users', { params: { page: 1, limit: 10 } });
 */
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
