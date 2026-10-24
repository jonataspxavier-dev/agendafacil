import { api } from './api'

export interface TopServico {
  servicoId: number
  nome: string
  quantidade: number
}

export interface Dashboard {
  agendamentosHoje: number
  faturamentoMes: number
  topServicos: TopServico[]
}

export async function obter(): Promise<Dashboard> {
  const { data } = await api.get<Dashboard>('/admin/dashboard')
  return data
}
