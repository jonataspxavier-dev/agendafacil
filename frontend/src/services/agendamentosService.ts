import { api } from './api'

export type StatusAgendamento = 'AGENDADO' | 'CANCELADO' | 'CONCLUIDO'

export interface Agendamento {
  id: number
  profissionalId: number
  profissionalNome: string
  servicoId: number
  servicoNome: string
  inicio: string
  fim: string
  status: StatusAgendamento
  precoCobrado: number
}

export async function criar(profissionalId: number, servicoId: number, inicio: string): Promise<Agendamento> {
  const { data } = await api.post<Agendamento>('/agendamentos', { profissionalId, servicoId, inicio })
  return data
}

export async function listarMeus(): Promise<Agendamento[]> {
  const { data } = await api.get<Agendamento[]>('/agendamentos/meus')
  return data
}

export async function cancelar(id: number): Promise<Agendamento> {
  const { data } = await api.patch<Agendamento>(`/agendamentos/${id}/cancelar`)
  return data
}
