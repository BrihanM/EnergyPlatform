import type { ReactElement } from 'react'

import LoginPageComponent from './LoginPage'
import RegisterPageComponent from './RegisterPage'
import AuthenticatedHomePageComponent from './AuthenticatedHomePage'

export function LoginPage(): ReactElement {
  return <LoginPageComponent />
}

export function RegisterPage(): ReactElement {
  return <RegisterPageComponent />
}

export function AuthenticatedHomePage(): ReactElement {
  return <AuthenticatedHomePageComponent />
}

export default LoginPage
