export interface LoginFormValues {
  identifier: string
  password: string
}

export type LoginStatus = 'idle' | 'loading' | 'success' | 'error'
