export type HttpMethod =
  | 'GET'
  | 'POST'
  | 'PUT'
  | 'PATCH'
  | 'DELETE'
  | 'HEAD'
  | 'OPTIONS';

export type QueryParamValue = string | number | boolean | null | undefined;
export type QueryParamCollection = QueryParamValue | QueryParamValue[];

export interface HttpRequestOptions<TBody = unknown> {
  method?: HttpMethod;
  headers?: HeadersInit;
  params?: Record<string, QueryParamCollection>;
  body?: TBody;
  credentials?: RequestCredentials;
  signal?: AbortSignal;
  requiresAuth?: boolean;
  skipAuthRefresh?: boolean;
  retryAfterRefreshAttempted?: boolean;
}

export interface HttpErrorResponse {
  code?: string;
  message?: string;
  correlationId?: string;
  details?: string;
}
