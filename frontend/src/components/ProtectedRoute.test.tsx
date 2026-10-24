import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext, type AuthContextValue } from '@/contexts/AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

const noop = async () => {}
const noopLogin = async () => ({ id: 1, nome: 'Teste', email: 'teste@example.com', perfil: 'CLIENTE' as const })

function renderProtected(value: AuthContextValue) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/protegida']}>
        <Routes>
          <Route path="/login" element={<div>Tela de login</div>} />
          <Route
            path="/protegida"
            element={
              <ProtectedRoute>
                <div>Conteúdo protegido</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects to /login when the user is not authenticated', () => {
    renderProtected({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.getByText('Tela de login')).toBeInTheDocument()
  })

  it('renders its children when the user is authenticated', () => {
    renderProtected({
      user: { id: 1, nome: 'Cliente Teste', email: 'cliente@example.com', perfil: 'CLIENTE' },
      isAuthenticated: true,
      isLoading: false,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.getByText('Conteúdo protegido')).toBeInTheDocument()
  })

  it('renders nothing while the auth state is still loading', () => {
    renderProtected({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument()
    expect(screen.queryByText('Tela de login')).not.toBeInTheDocument()
  })
})
