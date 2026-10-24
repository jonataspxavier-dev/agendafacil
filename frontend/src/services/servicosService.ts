import { api } from './api'

export interface Servico {
  id: number
  nome: string
  descricao: string
  duracaoMin: number
  preco: number
  ativo: boolean
}

export async function listar(): Promise<Servico[]> {
  const { data } = await api.get<Servico[]>('/servicos')
  return data
}
