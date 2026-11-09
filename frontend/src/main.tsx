import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import App from './App.tsx'
import { AuthProvider } from './core/auth'
import './index.css'

/**
 * Application entry point.
 * 
 * Renders the React application into the DOM with:
 * - StrictMode for additional development checks and warnings
 * - AuthProvider wrapping the entire app to provide authentication context
 * 
 * The root DOM element with id 'root' must exist in index.html.
 * 
 * @remarks
 * - AuthProvider is placed at the root level to ensure all components have access to authentication state
 * - StrictMode helps identify potential issues during development
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
