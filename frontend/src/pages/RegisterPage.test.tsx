import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '@/contexts/AuthContext'
import { RegisterPage } from './RegisterPage'

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

function renderRegisterPage(authValue: AuthContextValue) {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={['/cadastro']}>
        <Routes>
          <Route path="/cadastro" element={<RegisterPage />} />
          <Route path="/servicos" element={<div>Página de serviços</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('RegisterPage', () => {
  it('shows validation errors and never calls register when the form is invalid', async () => {
    const register = vi.fn()
    renderRegisterPage(authStub({ register }))

    fireEvent.click(screen.getByRole('button', { name: /cadastrar/i }))

    await waitFor(() => {
      expect(screen.getByText(/informe o nome/i)).toBeInTheDocument()
    })
    expect(register).not.toHaveBeenCalled()
  })

  it('rejects a password shorter than 8 characters', async () => {
    const register = vi.fn()
    renderRegisterPage(authStub({ register }))

    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: 'Cliente Teste' } })
    fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: 'cliente@example.com' } })
    fireEvent.change(screen.getByLabelText(/senha/i), { target: { value: '123' } })
    fireEvent.click(screen.getByRole('button', { name: /cadastrar/i }))

    await waitFor(() => {
      expect(screen.getByText(/pelo menos 8 caracteres/i)).toBeInTheDocument()
    })
    expect(register).not.toHaveBeenCalled()
  })

  it('calls register with the form values and redirects to /servicos on success', async () => {
    const register = vi.fn().mockResolvedValue(undefined)
    renderRegisterPage(authStub({ register }))

    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: 'Cliente Teste' } })
    fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: 'cliente@example.com' } })
    fireEvent.change(screen.getByLabelText(/senha/i), { target: { value: 'senha123' } })
    fireEvent.click(screen.getByRole('button', { name: /cadastrar/i }))

    await waitFor(() => {
      expect(register).toHaveBeenCalledWith('Cliente Teste', 'cliente@example.com', 'senha123')
    })
    await waitFor(() => {
      expect(screen.getByText('Página de serviços')).toBeInTheDocument()
    })
  })

  it('shows the backend error message when register fails (e.g. duplicate email)', async () => {
    const register = vi.fn().mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'E-mail já cadastrado' } },
    })
    renderRegisterPage(authStub({ register }))

    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: 'Cliente Teste' } })
    fireEvent.change(screen.getByLabelText(/e-mail/i), { target: { value: 'cliente@example.com' } })
    fireEvent.change(screen.getByLabelText(/senha/i), { target: { value: 'senha123' } })
    fireEvent.click(screen.getByRole('button', { name: /cadastrar/i }))

    await waitFor(() => {
      expect(screen.getByText('E-mail já cadastrado')).toBeInTheDocument()
    })
  })
})
