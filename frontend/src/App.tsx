import './App.css'
import { AppRouter } from './app/router/AppRouter'

/**
 * Root application component.
 * Renders the main page of the application.
 * Note: The AuthProvider wrapping is handled in main.tsx.
 * 
 * @returns {JSX.Element} The rendered application content.
 */
function App() {
  return <AppRouter />
}

export default App