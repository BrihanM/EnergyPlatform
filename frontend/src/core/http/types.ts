/**
 * HTTP method types supported by the HTTP client.
 * 
 * @typedef {string} HttpMethod
 * @type {'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS'}
 */
export type HttpMethod =
  | 'GET'
  | 'POST'
  | 'PUT'
  | 'PATCH'
  | 'DELETE'
  | 'HEAD'
  | 'OPTIONS'

/**
 * Represents a single query parameter value.
 * Can be a primitive value or null/undefined to omit the parameter.
 * 
 * @typedef {*} QueryParamValue
 */
export type QueryParamValue = string | number | boolean | null | undefined

/**
 * Represents a collection of query parameter values.
 * Can be a single value or an array of values to set multiple parameters with the same key.
 * 
 * @typedef {*} QueryParamCollection
 */
export type QueryParamCollection = QueryParamValue | QueryParamValue[]

/**
 * Configuration options for HTTP requests.
 * 
 * @template TBody - The type of the request body.
 */
export interface HttpRequestOptions<TBody = unknown> {
  /** The HTTP method for the request. Defaults to 'GET'. */
  method?: HttpMethod
  /** Custom headers to include in the request. */
  headers?: HeadersInit
  /** Query parameters to append to the URL. */
  params?: Record<string, QueryParamCollection>
  /** The request body payload. */
  body?: TBody
  /** Whether to include credentials (cookies) with the request. Defaults to 'include'. */
  credentials?: RequestCredentials
  /** AbortSignal to cancel the request. */
  signal?: AbortSignal
  requiresAuth?: boolean
  skipAuthRefresh?: boolean
  retryAfterRefreshAttempted?: boolean
}

/**
 * Represents an error response from the HTTP server.
 * Contains error details such as code, message, and correlation ID for debugging.
 */
export interface HttpErrorResponse {
  /** Application-specific error code. */
  code?: string
  /** Human-readable error message. */
  message?: string
  /** Correlation ID for tracking the error across logs. */
  correlationId?: string
  /** Additional error details or context. */
  details?: string
}
