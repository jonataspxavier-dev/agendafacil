import { api, type AuthTokens, refreshTokens } from './api'

export type Perfil = 'ADMIN' | 'CLIENTE'

export interface Usuario {
  id: number
  nome: string
  email: string
  perfil: Perfil
}

export async function registrar(nome: string, email: string, senha: string): Promise<Usuario> {
  const { data } = await api.post<Usuario>('/auth/register', { nome, email, senha })
  return data
}

export async function login(email: string, senha: string): Promise<AuthTokens> {
  const { data } = await api.post<AuthTokens>('/auth/login', { email, senha })
  return data
}

export async function me(): Promise<Usuario> {
  const { data } = await api.get<Usuario>('/auth/me')
  return data
}

export async function logout(refreshToken: string): Promise<void> {
  await api.post('/auth/logout', { refreshToken })
}

export const refresh = refreshTokens
