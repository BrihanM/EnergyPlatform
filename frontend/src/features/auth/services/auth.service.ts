import { httpClient } from '../../../core/http/httpClient';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  displayName: string;
  email: string;
  password: string;
}

export interface AuthSuccessResponse {
  message?: string;
  user?: {
    id: string;
    email: string;
    displayName?: string;
  };
}

export interface GatewayHealthResponse {
  status: string;
  timestamp?: string;
}

export const authService = {
  health: () => httpClient.get<GatewayHealthResponse>('/api/v1/integration/health'),
  login: (payload: LoginPayload) =>
    httpClient.post<AuthSuccessResponse>('/api/v1/auth/login', { body: payload }),
  register: (payload: RegisterPayload) =>
    httpClient.post<AuthSuccessResponse>('/api/v1/auth/register', { body: payload }),
  refresh: () =>
    httpClient.post<{ message?: string }>('/api/v1/auth/refresh', {
      credentials: 'include',
    }),
  logout: () =>
    httpClient.post<{ message?: string }>('/api/v1/auth/logout', {
      credentials: 'include',
    }),
};
