import { useState, type ChangeEvent, type FormEvent, type ReactElement } from 'react';

import { useAuth } from '../../../core/auth';
import { authService, type LoginRequest, type RegisterRequest } from '../services/auth.service';

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

const resolveErrorMessage = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const maybeHttpError = error as { message?: unknown };

    if (typeof maybeHttpError.message === 'string' && maybeHttpError.message.trim()) {
      return maybeHttpError.message;
    }
  }

  return 'Unable to complete the request. Please try again.';
};

export function LoginPage(): ReactElement {
  const { isAuthenticated, user, setSession, clearSession } = useAuth();
  const [mode, setMode] = useState<AuthMode>('login');
  const [loginForm, setLoginForm] = useState<LoginRequest>(emptyLoginForm);
  const [registerForm, setRegisterForm] = useState<RegisterRequest>(emptyRegisterForm);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLoginChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setLoginForm((current) => ({ ...current, [name]: value }));
  };

  const handleRegisterChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setRegisterForm((current) => ({ ...current, [name]: value }));
  };

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      const response = await authService.login(loginForm);

      setSession({
        accessToken: response.accessToken,
        user: {
          id: response.user.id,
          email: response.user.email,
          username: response.user.username,
          roles: response.roles,
        },
      });

      setStatusMessage('Welcome back, ' + (response.user.username || response.user.email) + '.');
      setLoginForm(emptyLoginForm);
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      await authService.register(registerForm);
      setRegisterForm(emptyRegisterForm);
      setMode('login');
      setStatusMessage('Registration successful. You can now sign in.');
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = async () => {
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      await authService.logout();
    } catch {
      // The session still clears locally even if the server response fails.
    } finally {
      clearSession();
      setStatusMessage('You have been signed out.');
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-card-heading">
          <p className="auth-kicker">
            Authentication
          </p>
          <div className="auth-mode-switch">
            {(['login', 'register'] as AuthMode[]).map((authMode) => (
              <button
                key={authMode}
                className={mode === authMode ? 'is-active' : undefined}
                type="button"
                onClick={() => {
                  setMode(authMode);
                  setErrorMessage(null);
                  setStatusMessage(null);
                }}
              >
                {authMode === 'login' ? 'Login' : 'Register'}
              </button>
            ))}
          </div>
        </div>

        <div className="auth-card-body">
          {isAuthenticated ? (
            <div>
              <h2>Signed in</h2>
              <p className="muted">
                Welcome back, {user?.username ?? user?.email ?? 'member'}.
              </p>
              <button
                type="button"
                onClick={handleLogout}
                className="danger-button"
              >
                Sign out
              </button>
            </div>
          ) : mode === 'login' ? (
            <form className="auth-form" onSubmit={handleLoginSubmit}>
              <div>
                <label htmlFor="identifier">
                  Username or email
                </label>
                <input
                  id="identifier"
                  name="identifier"
                  type="text"
                  value={loginForm.identifier}
                  onChange={handleLoginChange}
                  required
                  placeholder="jdoe or john@example.com"
                />
              </div>

              <div>
                <label htmlFor="password">
                  Password
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={loginForm.password}
                  onChange={handleLoginChange}
                  required
                  placeholder="Enter your password"
                />
              </div>

              <button className="primary-button" type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Signing in...' : 'Sign in'}
              </button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={handleRegisterSubmit}>
              <div className="auth-name-fields">
                <div>
                  <label htmlFor="firstName">
                    First name
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    value={registerForm.firstName}
                    onChange={handleRegisterChange}
                    required
                  />
                </div>
                <div>
                  <label htmlFor="lastName">
                    Last name
                  </label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    value={registerForm.lastName}
                    onChange={handleRegisterChange}
                    required
                  />
                </div>
              </div>

              <div>
                <label htmlFor="username">
                  Username
                </label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  value={registerForm.username}
                  onChange={handleRegisterChange}
                  required
                />
              </div>

              <div>
                <label htmlFor="email">
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={registerForm.email}
                  onChange={handleRegisterChange}
                  required
                />
              </div>

              <div>
                <label htmlFor="register-password">
                  Password
                </label>
                <input
                  id="register-password"
                  name="password"
                  type="password"
                  value={registerForm.password}
                  onChange={handleRegisterChange}
                  required
                  minLength={12}
                />
              </div>

              <button className="primary-button" type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creating account...' : 'Create account'}
              </button>
            </form>
          )}

          {(statusMessage || errorMessage) && (
            <div
              className={`auth-feedback${errorMessage ? ' auth-feedback-error' : ''}`}
            >
              {errorMessage ?? statusMessage}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

export default LoginPage;
