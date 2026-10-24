import { api } from './api'

export type DiaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface Jornada {
  diaSemana: DiaSemana
  horaInicio: string
  horaFim: string
}

export interface Profissional {
  id: number
  nome: string
  ativo: boolean
  servicoIds: number[]
  jornada: Jornada[]
}

export async function listar(): Promise<Profissional[]> {
  const { data } = await api.get<Profissional[]>('/profissionais')
  return data
}
