/**
 * Centralizes access to Vite environment variables for the frontend.
 */
const requiredEnvironmentVariables = ['VITE_API_BASE_URL'] as const;

type RequiredEnvironmentVariable = (typeof requiredEnvironmentVariables)[number];

const readEnvironmentVariable = (name: RequiredEnvironmentVariable): string => {
  const value = import.meta.env[name];

  return typeof value === 'string' ? value.trim() : '';
};

const apiBaseUrl = readEnvironmentVariable('VITE_API_BASE_URL').replace(/\/+$/, '');
const missingEnvironmentVariables = requiredEnvironmentVariables.filter(
  (variable) => !readEnvironmentVariable(variable),
);

if (missingEnvironmentVariables.length > 0) {
  console.warn(
    `[environment] Missing Vite config values: ${missingEnvironmentVariables.join(', ')}. Falling back to http://localhost:8080.`,
  );
}

export const environment = {
  apiBaseUrl: apiBaseUrl || 'http://localhost:8080',
  appName: import.meta.env.VITE_APP_NAME ?? 'Mi App Frontend',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
} as const;

export const validateEnvironment = (): void => {
  if (!environment.apiBaseUrl) {
    throw new Error('VITE_API_BASE_URL is required to reach the API Gateway.');
  }
};

validateEnvironment();
