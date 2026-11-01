import { httpClient } from '../../../core/http/httpClient';

/**
 * Credentials required to log in a user.
 */
export interface LoginRequest {
  /** Username, email, or other unique identifier. */
  identifier: string
  /** The user's password. */
  password: string
}

/**
 * Information required to create a new user account.
 */
export interface RegisterRequest {
  /** The user's first name. */
  firstName: string
  /** The user's last name. */
  lastName: string
  /** The desired username. */
  username: string
  /** The user's email address. */
  email: string
  /** The user's password. */
  password: string
}

/**
 * Summary information about an authenticated user.
 */
export interface AuthUserSummary {
  /** The unique user identifier. */
  id: string
  /** The user's first name. */
  firstName: string
  /** The user's last name. */
  lastName: string
  /** The user's username. */
  username: string
  /** The user's email address. */
  email: string
  /** ISO 8601 timestamp when the user was created. */
  createdAt?: string
}

/**
 * Response returned upon successful login.
 * Contains authentication token and user information.
 */
export interface LoginResponse {
  /** Whether the user was successfully authenticated. */
  authenticated: boolean
  /** JWT access token for authenticating future requests. */
  accessToken: string
  /** The type of token (usually 'Bearer'). */
  tokenType: string
  /** Token expiration time in seconds. */
  expiresIn: number
  /** Summary information about the authenticated user. */
  user: AuthUserSummary
}

/**
 * Response returned upon successful user registration.
 * Contains the created user's information.
 */
export interface RegisterResponse {
  /** The unique identifier of the newly created user. */
  id: string
  /** The user's first name. */
  firstName: string
  /** The user's last name. */
  lastName: string
  /** The user's username. */
  username: string
  /** The user's email address. */
  email: string
  /** The status of the newly created account. */
  status: string
  /** ISO 8601 timestamp when the user was created. */
  createdAt: string
}

/**
 * Authentication service for managing user login, registration, and session refresh.
 * Uses the HTTP client to communicate with the backend authentication API.
 */
export const authService = {
  /**
   * Authenticates a user with their credentials.
   * 
   * @param {LoginRequest} payload - The login credentials.
   * @returns {Promise<LoginResponse>} The authentication response with token and user info.
   */
  login: (payload: LoginRequest) =>
    httpClient.post<LoginResponse>('/api/v1/auth/login', { body: payload }),
  
  /**
   * Creates a new user account.
   * 
   * @param {RegisterRequest} payload - The new user's information.
   * @returns {Promise<RegisterResponse>} The registration response with created user info.
   */
  register: (payload: RegisterRequest) =>
    httpClient.post<RegisterResponse>('/api/v1/auth/register', { body: payload }),
  
  /**
   * Refreshes the current access token using stored credentials.
   * Requires cookies to be enabled for authentication.
   * 
   * @returns {Promise<LoginResponse>} The new authentication response with refreshed token.
   */
  refresh: () =>
    httpClient.post<LoginResponse>('/api/v1/auth/refresh', {
      credentials: 'include',
    }),
  
  /**
   * Logs out the current user and clears the session.
   * 
   * @returns {Promise<void>}
   */
  logout: () =>
    httpClient.post<void>('/api/v1/auth/logout', {
      credentials: 'include',
    }),
};
