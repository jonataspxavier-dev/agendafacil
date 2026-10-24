import { useCallback, useEffect, useState } from 'react'
import { BarChart } from '@/components/BarChart'
import { ErrorMessage } from '@/components/ErrorMessage'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import * as dashboardService from '@/services/dashboardService'
import type { Dashboard } from '@/services/dashboardService'

type Status = 'loading' | 'success' | 'error'

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

export function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [status, setStatus] = useState<Status>('loading')

  const carregar = useCallback(async () => {
    setStatus('loading')
    try {
      const data = await dashboardService.obter()
      setDashboard(data)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (status === 'loading') {
    return <LoadingSpinner />
  }

  if (status === 'error' || !dashboard) {
    return <ErrorMessage message="Não foi possível carregar o dashboard." onRetry={carregar} />
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Dashboard</h1>
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-sm text-slate-500">Agendamentos hoje</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">{dashboard.agendamentosHoje}</p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-sm text-slate-500">Faturamento do mês</p>
          <p className="mt-1 text-2xl font-semibold text-slate-900">{formatarPreco(dashboard.faturamentoMes)}</p>
        </div>
      </div>
      <div className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-lg font-medium text-slate-900">Serviços mais vendidos</h2>
        <div className="mt-3">
          <BarChart
            data={dashboard.topServicos.map((servico) => ({ label: servico.nome, value: servico.quantidade }))}
          />
        </div>
      </div>
    </div>
  )
}
