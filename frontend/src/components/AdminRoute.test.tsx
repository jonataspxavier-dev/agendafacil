import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AuthContext, type AuthContextValue } from '@/contexts/AuthContext'
import { AdminRoute } from './AdminRoute'

const noop = async () => {}
const noopLogin = async () => ({ id: 1, nome: 'Teste', email: 'teste@example.com', perfil: 'CLIENTE' as const })

function renderAdminRoute(value: AuthContextValue) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/admin/protegida']}>
        <Routes>
          <Route path="/login" element={<div>Tela de login</div>} />
          <Route path="/servicos" element={<div>Tela de serviços</div>} />
          <Route
            path="/admin/protegida"
            element={
              <AdminRoute>
                <div>Conteúdo admin</div>
              </AdminRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('AdminRoute', () => {
  it('redirects to /login when the user is not authenticated', () => {
    renderAdminRoute({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.getByText('Tela de login')).toBeInTheDocument()
  })

  it('redirects to /servicos when authenticated as a non-admin', () => {
    renderAdminRoute({
      user: { id: 1, nome: 'Cliente Teste', email: 'cliente@example.com', perfil: 'CLIENTE' },
      isAuthenticated: true,
      isLoading: false,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.getByText('Tela de serviços')).toBeInTheDocument()
  })

  it('renders its children when authenticated as ADMIN', () => {
    renderAdminRoute({
      user: { id: 1, nome: 'Admin Teste', email: 'admin@example.com', perfil: 'ADMIN' },
      isAuthenticated: true,
      isLoading: false,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.getByText('Conteúdo admin')).toBeInTheDocument()
  })

  it('renders nothing while the auth state is still loading', () => {
    renderAdminRoute({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      login: noopLogin,
      register: noop,
      logout: noop,
    })

    expect(screen.queryByText('Conteúdo admin')).not.toBeInTheDocument()
    expect(screen.queryByText('Tela de login')).not.toBeInTheDocument()
    expect(screen.queryByText('Tela de serviços')).not.toBeInTheDocument()
  })
})
