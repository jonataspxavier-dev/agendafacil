import { api } from './api'
import type { DiaSemana, Profissional } from './profissionaisService'

export interface JornadaRequest {
  diaSemana: DiaSemana
  horaInicio: string
  horaFim: string
}

export interface ProfissionalRequest {
  nome: string
  servicoIds: number[]
  jornada: JornadaRequest[]
}

export async function criar(request: ProfissionalRequest): Promise<Profissional> {
  const { data } = await api.post<Profissional>('/profissionais', request)
  return data
}

export async function atualizar(id: number, request: ProfissionalRequest): Promise<Profissional> {
  const { data } = await api.put<Profissional>(`/profissionais/${id}`, request)
  return data
}

export async function desativar(id: number): Promise<void> {
  await api.delete(`/profissionais/${id}`)
}
