import { api } from './api'
import { cancelar as cancelarAgendamento } from './agendamentosService'
import type { Agendamento, StatusAgendamento } from './agendamentosService'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ListarParams {
  data?: string
  status?: StatusAgendamento
  page: number
  size: number
}

export async function listar(params: ListarParams): Promise<PageResponse<Agendamento>> {
  const { data } = await api.get<PageResponse<Agendamento>>('/admin/agendamentos', {
    params: {
      data: params.data,
      status: params.status,
      page: params.page,
      size: params.size,
    },
  })
  return data
}

export async function concluir(id: number): Promise<Agendamento> {
  const { data } = await api.patch<Agendamento>(`/agendamentos/${id}/concluir`)
  return data
}

export const cancelar = cancelarAgendamento
