import { createContext, useEffect, useState, type ReactNode } from 'react'
import { clearTokens, getAccessToken, getRefreshToken, setOnAuthFailure, storeTokens } from '@/services/api'
import * as authService from '@/services/authService'
import type { Usuario } from '@/services/authService'

export interface AuthContextValue {
  user: Usuario | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, senha: string) => Promise<void>
  register: (nome: string, email: string, senha: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    setOnAuthFailure(() => setUser(null))
  }, [])

  useEffect(() => {
    let active = true

    async function hydrate() {
      if (!getAccessToken()) {
        setIsLoading(false)
        return
      }
      try {
        const usuario = await authService.me()
        if (active) setUser(usuario)
      } catch {
        clearTokens()
        if (active) setUser(null)
      } finally {
        if (active) setIsLoading(false)
      }
    }

    hydrate()
    return () => {
      active = false
    }
  }, [])

  async function login(email: string, senha: string) {
    const tokens = await authService.login(email, senha)
    storeTokens(tokens)
    const usuario = await authService.me()
    setUser(usuario)
  }

  async function register(nome: string, email: string, senha: string) {
    await authService.registrar(nome, email, senha)
    await login(email, senha)
  }

  async function logout() {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken) {
        await authService.logout(refreshToken)
      }
    } finally {
      clearTokens()
      setUser(null)
    }
  }

  const value: AuthContextValue = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
