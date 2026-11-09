import type { ReactElement } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '../../core/auth'
import { RouteLoading } from './RouteLoading'

/**
 * Protects private routes based on the global auth state.
 */
export function ProtectedRoute(): ReactElement {
  const { status, isAuthenticated } = useAuth()
  const location = useLocation()

  if (status === 'initializing') {
    return <RouteLoading />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}

export default ProtectedRoute
