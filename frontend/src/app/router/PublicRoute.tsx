import type { ReactElement } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '../../core/auth'
import { RouteLoading } from './RouteLoading'

type RouteState = {
  from?: {
    pathname?: string
    search?: string
    hash?: string
  }
}

const resolveRedirectTarget = (state: unknown): string => {
  const maybeState = state as RouteState | null
  const from = maybeState?.from

  if (!from?.pathname) {
    return '/app'
  }

  return from.pathname + (from.search ?? '') + (from.hash ?? '')
}

/**
 * Redirects authenticated users away from auth pages.
 */
export function PublicRoute(): ReactElement {
  const { status, isAuthenticated } = useAuth()
  const location = useLocation()

  if (status === 'initializing') {
    return <RouteLoading />
  }

  if (isAuthenticated) {
    return <Navigate to={resolveRedirectTarget(location.state)} replace />
  }

  return <Outlet />
}

export default PublicRoute
