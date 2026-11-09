import { httpClient } from '../../../core/http/httpClient';

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
}

export interface AuthUserSummary {
  id: string;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  createdAt?: string;
}

export interface LoginResponse {
  authenticated: boolean;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUserSummary;
  roles: string[];
}

export interface RegisterResponse {
  id: string;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  status: string;
  createdAt: string;
}

export const authService = {
  login: (payload: LoginRequest) =>
    httpClient.post<LoginResponse>('/api/v1/auth/login', { body: payload }),
  register: (payload: RegisterRequest) =>
    httpClient.post<RegisterResponse>('/api/v1/auth/register', { body: payload }),
  refresh: () =>
    httpClient.post<LoginResponse>('/api/v1/auth/refresh', {
      credentials: 'include',
    }),
  logout: () =>
    httpClient.post<void>('/api/v1/auth/logout', {
      credentials: 'include',
    }),
};
