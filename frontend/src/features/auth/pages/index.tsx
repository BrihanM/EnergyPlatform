<<<<<<< Updated upstream
﻿import type { ReactElement } from 'react'

import LoginPageComponent from './LoginPage'
import RegisterPageComponent from './RegisterPage'
import AuthenticatedHomePageComponent from './AuthenticatedHomePage'
=======
﻿/**
 * Authentication page component that displays a login and registration form.
 * Manages local form state, authentication state, and handles user login/registration/logout flows.
 * 
 * Features:
 * - Toggle between login and registration modes
 * - Form validation and submission handling
 * - Real-time error and status message display
 * - Automatic token storage on successful login
 * - Graceful logout with server synchronization
 * 
 * @returns {ReactElement} The rendered authentication page with forms and user interface.
 * 
 * @example
 * // Usage in App component
 * import { LoginPage } from './features/auth/pages'
 * 
 * export function App() {
 *   return <LoginPage />
 * }
 */
import { useState, type ChangeEvent, type FormEvent, type ReactElement } from 'react'

import { useAuth } from '../../../core/auth'
import { authService, type LoginRequest, type RegisterRequest } from '../services/auth.service'

type AuthMode = 'login' | 'register';

const emptyLoginForm: LoginRequest = {
  identifier: '',
  password: '',
};

const emptyRegisterForm: RegisterRequest = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
};

/**
 * Extracts a human-readable error message from an error object.
 * Handles various error formats including HttpError with message property.
 * Falls back to a generic message if the error format is not recognized.
 * 
 * @param {unknown} error - The error object to extract a message from.
 * @returns {string} A human-readable error message.
 */
const resolveErrorMessage = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const maybeHttpError = error as { message?: unknown };

    if (typeof maybeHttpError.message === 'string' && maybeHttpError.message.trim()) {
      return maybeHttpError.message;
    }
  }

  return 'Unable to complete the request. Please try again.';
};
>>>>>>> Stashed changes

export function LoginPage(): ReactElement {
  return <LoginPageComponent />
}

export function RegisterPage(): ReactElement {
  return <RegisterPageComponent />
}

export function AuthenticatedHomePage(): ReactElement {
  return <AuthenticatedHomePageComponent />
}

export default LoginPage
