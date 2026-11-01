import type { ReactElement } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { LoginPage, RegisterPage, AuthenticatedHomePage } from '../../features/auth/pages'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'

export function AppRouter(): ReactElement {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/app" replace />} />

        <Route element={<PublicRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route path="/app" element={<AuthenticatedHomePage />} />
        </Route>

        <Route path="*" element={<Navigate to="/app" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
