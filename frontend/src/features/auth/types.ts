export interface LoginFormValues {
  identifier: string
  password: string
}

export interface RegisterFormValues {
  firstName: string
  lastName: string
  username: string
  email: string
  password: string
  confirmPassword: string
}

export type LoginStatus = 'idle' | 'loading' | 'success' | 'error'
export type RegisterStatus = 'idle' | 'loading' | 'success' | 'error'
