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
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background: 'linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%)',
        padding: '2rem 1rem',
      }}
    >
      <section
        style={{
          width: 'min(480px, 100%)',
          background: '#ffffff',
          borderRadius: '20px',
          boxShadow: '0 24px 60px rgba(15, 23, 42, 0.08)',
          border: '1px solid #e2e8f0',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            padding: '1.5rem 1.5rem 0.75rem',
            borderBottom: '1px solid #e2e8f0',
            background: '#f8fafc',
          }}
        >
          <p style={{ margin: 0, color: '#475569', letterSpacing: '0.08em', textTransform: 'uppercase', fontSize: '0.72rem' }}>
            Authentication
          </p>
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
            {(['login', 'register'] as AuthMode[]).map((authMode) => (
              <button
                key={authMode}
                type="button"
                onClick={() => {
                  setMode(authMode);
                  setErrorMessage(null);
                  setStatusMessage(null);
                }}
                style={{
                  flex: 1,
                  border: 'none',
                  borderRadius: '999px',
                  padding: '0.7rem 1rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  background: mode === authMode ? '#2563eb' : '#e2e8f0',
                  color: mode === authMode ? '#ffffff' : '#1e293b',
                  transition: 'all 0.2s ease',
                }}
              >
                {authMode === 'login' ? 'Login' : 'Register'}
              </button>
            ))}
          </div>
        </div>

        <div style={{ padding: '1.5rem' }}>
          {isAuthenticated ? (
            <div>
              <h2 style={{ margin: '0 0 0.5rem', fontSize: '1.5rem' }}>Signed in</h2>
              <p style={{ margin: '0 0 1.25rem', color: '#475569' }}>
                Welcome back, {user?.username ?? user?.email ?? 'member'}.
              </p>
              <button
                type="button"
                onClick={handleLogout}
                style={{
                  width: '100%',
                  border: 'none',
                  borderRadius: '10px',
                  padding: '0.8rem 1rem',
                  background: '#dc2626',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Sign out
              </button>
            </div>
          ) : mode === 'login' ? (
            <form onSubmit={handleLoginSubmit} style={{ display: 'grid', gap: '1rem' }}>
              <div>
                <label htmlFor="identifier" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
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
                  style={inputStyles}
                />
              </div>

              <div>
                <label htmlFor="password" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
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
                  style={inputStyles}
                />
              </div>

              <button type="submit" disabled={isSubmitting} style={{ ...primaryButtonStyle, opacity: isSubmitting ? 0.7 : 1 }}>
                {isSubmitting ? 'Signing in...' : 'Sign in'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegisterSubmit} style={{ display: 'grid', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div>
                  <label htmlFor="firstName" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
                    First name
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    value={registerForm.firstName}
                    onChange={handleRegisterChange}
                    required
                    style={inputStyles}
                  />
                </div>
                <div>
                  <label htmlFor="lastName" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
                    Last name
                  </label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    value={registerForm.lastName}
                    onChange={handleRegisterChange}
                    required
                    style={inputStyles}
                  />
                </div>
              </div>

              <div>
                <label htmlFor="username" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
                  Username
                </label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  value={registerForm.username}
                  onChange={handleRegisterChange}
                  required
                  style={inputStyles}
                />
              </div>

              <div>
                <label htmlFor="email" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={registerForm.email}
                  onChange={handleRegisterChange}
                  required
                  style={inputStyles}
                />
              </div>

              <div>
                <label htmlFor="register-password" style={{ display: 'block', marginBottom: '0.35rem', fontWeight: 600 }}>
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
                  style={inputStyles}
                />
              </div>

              <button type="submit" disabled={isSubmitting} style={{ ...primaryButtonStyle, opacity: isSubmitting ? 0.7 : 1 }}>
                {isSubmitting ? 'Creating account...' : 'Create account'}
              </button>
            </form>
          )}

          {(statusMessage || errorMessage) && (
            <div
              style={{
                marginTop: '1rem',
                padding: '0.85rem 1rem',
                borderRadius: '10px',
                background: errorMessage ? '#fef2f2' : '#eff6ff',
                color: errorMessage ? '#b91c1c' : '#1d4ed8',
                border: '1px solid ' + (errorMessage ? '#fecaca' : '#bfdbfe'),
              }}
            >
              {errorMessage ?? statusMessage}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

const inputStyles: React.CSSProperties = {
  width: '100%',
  border: '1px solid #cbd5e1',
  borderRadius: '10px',
  padding: '0.8rem 0.9rem',
  fontSize: '1rem',
  outline: 'none',
  transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
};

const primaryButtonStyle: React.CSSProperties = {
  border: 'none',
  borderRadius: '10px',
  padding: '0.8rem 1rem',
  background: '#2563eb',
  color: '#ffffff',
  fontWeight: 700,
  cursor: 'pointer',
};

export default LoginPage;
