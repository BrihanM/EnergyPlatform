import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { HttpError } from '../../../core/http/httpError'
import { authService } from '../services/auth.service'
import { RegisterForm } from './RegisterForm'

const validValues = {
  firstName: 'Ana',
  lastName: 'García',
  username: 'ana.garcia',
  email: 'ana@example.com',
  password: 'Password123!',
  confirmPassword: 'Password123!',
}

describe('RegisterForm', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the required registration fields', () => {
    render(<RegisterForm />)

    expect(screen.getByLabelText('First name')).toBeTruthy()
    expect(screen.getByLabelText('Last name')).toBeTruthy()
    expect(screen.getByLabelText('Username')).toBeTruthy()
    expect(screen.getByLabelText('Email')).toBeTruthy()
    expect(screen.getByLabelText('Password')).toBeTruthy()
    expect(screen.getByLabelText('Confirm password')).toBeTruthy()
  })

  it('shows validation errors when required fields are missing', async () => {
    render(<RegisterForm />)

    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(screen.getByText('First name is required.')).toBeTruthy()
    expect(screen.getByText('Last name is required.')).toBeTruthy()
    expect(screen.getByText('Username is required.')).toBeTruthy()
    expect(screen.getByText('Email is required.')).toBeTruthy()
    expect(screen.getByText('Password is required.')).toBeTruthy()
    expect(screen.getByText('Please confirm your password.')).toBeTruthy()
  })

  it('validates that the password confirmation matches', async () => {
    render(<RegisterForm />)

    fireEvent.change(screen.getByLabelText('First name'), { target: { value: validValues.firstName } })
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: validValues.lastName } })
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: validValues.username } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: validValues.email } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: validValues.password } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'DifferentPassword123!' } })

    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(screen.getByText('Passwords do not match.')).toBeTruthy()
  })

  it('shows a loading state while the registration request is in flight', async () => {
    const registerPromise = new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          id: 'user-1',
          firstName: validValues.firstName,
          lastName: validValues.lastName,
          username: validValues.username,
          email: validValues.email,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        })
      }, 50)
    })

    vi.spyOn(authService, 'register').mockReturnValue(registerPromise as Promise<never>)

    render(<RegisterForm />)

    fireEvent.change(screen.getByLabelText('First name'), { target: { value: validValues.firstName } })
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: validValues.lastName } })
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: validValues.username } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: validValues.email } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: validValues.password } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: validValues.confirmPassword } })

    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(screen.getByRole('button', { name: /creating account/i })).toBeTruthy()
  })

  it('shows success feedback after a successful registration', async () => {
    const registerSpy = vi.spyOn(authService, 'register').mockResolvedValue({
      id: 'user-1',
      firstName: validValues.firstName,
      lastName: validValues.lastName,
      username: validValues.username,
      email: validValues.email,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    })

    render(<RegisterForm />)

    fireEvent.change(screen.getByLabelText('First name'), { target: { value: validValues.firstName } })
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: validValues.lastName } })
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: validValues.username } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: validValues.email } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: validValues.password } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: validValues.confirmPassword } })

    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('status')).toBeTruthy()
    expect(registerSpy).toHaveBeenCalledWith({
      firstName: validValues.firstName,
      lastName: validValues.lastName,
      username: validValues.username,
      email: validValues.email,
      password: validValues.password,
    })
  })

  it('shows backend errors for duplicate registrations', async () => {
    vi.spyOn(authService, 'register').mockRejectedValue(
      new HttpError({
        status: 409,
        message: 'Email is already registered.',
      }),
    )

    render(<RegisterForm />)

    fireEvent.change(screen.getByLabelText('First name'), { target: { value: validValues.firstName } })
    fireEvent.change(screen.getByLabelText('Last name'), { target: { value: validValues.lastName } })
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: validValues.username } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: validValues.email } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: validValues.password } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: validValues.confirmPassword } })

    fireEvent.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByText('Email is already registered.')).toBeTruthy()
  })
})
