import { api } from './api'

export interface Profissional {
  id: number
  nome: string
  ativo: boolean
  servicoIds: number[]
}

export async function listar(): Promise<Profissional[]> {
  const { data } = await api.get<Profissional[]>('/profissionais')
  return data
}
