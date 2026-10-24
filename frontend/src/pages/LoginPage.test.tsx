import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '@/contexts/AuthContext'
import { LoginPage } from './LoginPage'

function authStub(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  }
}

function renderLoginPage(authValue: AuthContextValue) {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/servicos" element={<div>Página de serviços</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('LoginPage', () => {
  it('shows a validation error and never calls login when the form is invalid', async () => {
    const login = vi.fn()
    renderLoginPage(authStub({ login }))

    fireEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => {
      expect(screen.getByText(/e-mail inválido/i)).toBeInTheDocument()
    })
    expect(login).not.toHaveBeenCalled()
  })

  it('calls login with the form values and redirects to /servicos on success', async () => {
    const login = vi.fn().mockResolvedValue(undefined)
    renderLoginPage(authStub({ login }))

    fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: 'cliente@example.com' } })
    fireEvent.change(screen.getByLabelText(/senha/i), { target: { value: 'senha123' } })
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith('cliente@example.com', 'senha123')
    })
    await waitFor(() => {
      expect(screen.getByText('Página de serviços')).toBeInTheDocument()
    })
  })

  it('shows the backend error message when login fails', async () => {
    const login = vi.fn().mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Credenciais inválidas' } },
    })
    renderLoginPage(authStub({ login }))

    fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: 'cliente@example.com' } })
    fireEvent.change(screen.getByLabelText(/senha/i), { target: { value: 'senha-errada' } })
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => {
      expect(screen.getByText('Credenciais inválidas')).toBeInTheDocument()
    })
  })
})
