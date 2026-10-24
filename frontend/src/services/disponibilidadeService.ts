import { api } from './api'

export interface Slot {
  inicio: string
  fim: string
}

export interface Disponibilidade {
  profissionalId: number
  servicoId: number
  data: string
  slots: Slot[]
}

export async function buscar(profissionalId: number, servicoId: number, data: string): Promise<Disponibilidade> {
  const { data: resposta } = await api.get<Disponibilidade>('/disponibilidade', {
    params: { profissionalId, servicoId, data },
  })
  return resposta
}
