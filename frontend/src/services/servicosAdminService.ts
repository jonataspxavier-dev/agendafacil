import { api } from './api'
import type { Servico } from './servicosService'

export interface ServicoRequest {
  nome: string
  descricao?: string
  duracaoMin: number
  preco: number
}

export async function criar(request: ServicoRequest): Promise<Servico> {
  const { data } = await api.post<Servico>('/servicos', request)
  return data
}

export async function atualizar(id: number, request: ServicoRequest): Promise<Servico> {
  const { data } = await api.put<Servico>(`/servicos/${id}`, request)
  return data
}

export async function desativar(id: number): Promise<void> {
  await api.delete(`/servicos/${id}`)
}
