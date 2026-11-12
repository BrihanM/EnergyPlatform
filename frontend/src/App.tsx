import './App.css'
import { LoginPage } from './features/auth/pages'
import { useEffect, useState } from 'react'
import { useAuth } from './core/auth'
import { CreateOfferPage, MarketplacePage, MyOffersPage, OfferDetailPage } from './features/offers'
import { HistoryPage } from './features/transactions/pages/HistoryPage'
import { TransactionPage } from './features/transactions/pages/TransactionPage'

function App() {
  const { isAuthenticated, user, clearSession, status } = useAuth()
  const [path, setPath] = useState(window.location.pathname)
  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])
  const navigate = (nextPath: string) => {
    window.history.pushState({}, '', nextPath)
    setPath(nextPath)
  }
  const canManage = isAuthenticated && user?.roles?.includes('NORMAL_USER')
  // Reserved offer sub-routes must not be captured as offer IDs.
  const detailMatch = path.match(/^\/offers\/(?!mine$|new$)([^/]+)$/)
  const transactionMatch = path.match(/^\/transactions\/([^/]+)$/)
  const canTransact = canManage && user?.id && !user.roles?.some((role) => role === 'ADMIN' || role === 'SECURITY_AUDITOR')
  const page = path === '/login' ? <LoginPage /> : detailMatch
    ? <OfferDetailPage key={detailMatch[1]} id={detailMatch[1]} navigate={navigate} />
    : path === '/transactions' || transactionMatch
      ? status === 'initializing' ? <p className="state-message">Restoring session...</p> : canTransact ? transactionMatch ? <TransactionPage key={transactionMatch[1]} id={transactionMatch[1]} userId={user.id!} navigate={navigate} /> : <HistoryPage key={user.id} userId={user.id!} navigate={navigate} /> : <p className="error-message">Sign in with an authorized NORMAL_USER account to view transactions.</p>
    : path === '/offers/new' && canManage ? <CreateOfferPage navigate={navigate} />
    : path === '/offers/mine' && canManage ? <MyOffersPage navigate={navigate} />
    : <MarketplacePage navigate={navigate} />
  const effectivePage = path === '/login' && isAuthenticated ? <MarketplacePage navigate={navigate} /> : page
  return <div className="app-shell">
    <header className="app-header"><button className="brand" onClick={() => navigate('/')}>Energy<span>Prosumer</span></button><nav><button onClick={() => navigate('/')}>Marketplace</button>{canManage && <><button onClick={() => navigate('/offers/mine')}>My offers</button><button onClick={() => navigate('/offers/new')}>Publish offer</button></>}{canTransact && <button onClick={() => navigate('/transactions')}>My transactions</button>}{isAuthenticated ? <button className="outline-button" onClick={() => { clearSession(); navigate('/login') }}>Sign out</button> : <button className="outline-button" onClick={() => navigate('/login')}>Sign in</button>}</nav></header>
    <main className="app-content">{effectivePage}</main>
  </div>
}

export default App