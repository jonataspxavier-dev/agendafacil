import { useCallback, useEffect, useState } from 'react'
import * as agendamentosService from '@/services/agendamentosService'
import type { Agendamento } from '@/services/agendamentosService'
import { extractErrorMessage } from '@/services/api'

type Status = 'loading' | 'success' | 'error'

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

const formatarDataHora = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

const statusLabel: Record<Agendamento['status'], string> = {
  AGENDADO: 'Agendado',
  CANCELADO: 'Cancelado',
  CONCLUIDO: 'Concluído',
}

export function MeusAgendamentosPage() {
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [cancelandoId, setCancelandoId] = useState<number | null>(null)
  const [erroPorId, setErroPorId] = useState<Record<number, string>>({})

  const carregar = useCallback(async () => {
    setStatus('loading')
    try {
      const data = await agendamentosService.listarMeus()
      setAgendamentos(data)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    carregar()
  }, [carregar])

  async function cancelar(id: number) {
    setCancelandoId(id)
    setErroPorId((atual) => {
      const resto = { ...atual }
      delete resto[id]
      return resto
    })
    try {
      const atualizado = await agendamentosService.cancelar(id)
      setAgendamentos((atual) => atual.map((item) => (item.id === id ? atualizado : item)))
    } catch (error) {
      setErroPorId((atual) => ({ ...atual, [id]: extractErrorMessage(error) }))
    } finally {
      setCancelandoId(null)
    }
  }

  if (status === 'loading') {
    return <p className="p-6 text-slate-600">Carregando...</p>
  }

  if (status === 'error') {
    return (
      <div className="p-6">
        <p className="text-red-600">Não foi possível carregar seus agendamentos.</p>
        <button
          type="button"
          onClick={carregar}
          className="mt-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Meus agendamentos</h1>
      {agendamentos.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">Você ainda não tem agendamentos.</p>
      ) : (
        <ul className="mt-4 space-y-4">
          {agendamentos.map((agendamento) => (
            <li key={agendamento.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <h2 className="text-lg font-medium text-slate-900">{agendamento.servicoNome}</h2>
              <p className="mt-1 text-sm text-slate-500">{agendamento.profissionalNome}</p>
              <p className="mt-1 text-sm text-slate-600">{formatarDataHora(agendamento.inicio)}</p>
              <p className="mt-1 text-sm text-slate-600">
                {statusLabel[agendamento.status]} · {formatarPreco(agendamento.precoCobrado)}
              </p>
              {erroPorId[agendamento.id] && <p className="mt-2 text-sm text-red-600">{erroPorId[agendamento.id]}</p>}
              {agendamento.status === 'AGENDADO' && (
                <button
                  type="button"
                  disabled={cancelandoId === agendamento.id}
                  onClick={() => cancelar(agendamento.id)}
                  className="mt-3 rounded-md border border-red-600 px-4 py-2 text-sm font-medium text-red-600 disabled:opacity-50"
                >
                  {cancelandoId === agendamento.id ? 'Cancelando...' : 'Cancelar'}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
