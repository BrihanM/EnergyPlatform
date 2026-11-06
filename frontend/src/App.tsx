import './App.css'
import { LoginPage } from './features/auth/pages'
import { useEffect, useState } from 'react'
import { useAuth } from './core/auth'
import { CreateOfferPage, MarketplacePage, MyOffersPage, OfferDetailPage } from './features/offers'

function App() {
  const { isAuthenticated, user, clearSession } = useAuth()
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
  const detailMatch = path.match(/^\/offers\/([^/]+)$/)
  const page = path === '/login' ? <LoginPage /> : detailMatch
    ? <OfferDetailPage id={detailMatch[1]} navigate={navigate} />
    : path === '/offers/new' && canManage ? <CreateOfferPage navigate={navigate} />
    : path === '/offers/mine' && canManage ? <MyOffersPage navigate={navigate} />
    : <MarketplacePage navigate={navigate} />
  const effectivePage = path === '/login' && isAuthenticated ? <MarketplacePage navigate={navigate} /> : page
  return <div className="app-shell">
    <header className="app-header"><button className="brand" onClick={() => navigate('/')}>Energy<span>Prosumer</span></button><nav><button onClick={() => navigate('/')}>Marketplace</button>{canManage && <><button onClick={() => navigate('/offers/mine')}>My offers</button><button onClick={() => navigate('/offers/new')}>Publish offer</button></>}{isAuthenticated ? <button className="outline-button" onClick={() => { clearSession(); navigate('/login') }}>Sign out</button> : <button className="outline-button" onClick={() => navigate('/login')}>Sign in</button>}</nav></header>
    <main className="app-content">{effectivePage}</main>
  </div>
}

export default App