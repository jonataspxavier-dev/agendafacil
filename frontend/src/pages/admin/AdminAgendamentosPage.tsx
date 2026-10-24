import { useCallback, useEffect, useState } from 'react'
import { ErrorMessage } from '@/components/ErrorMessage'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import * as adminAgendamentosService from '@/services/adminAgendamentosService'
import type { Agendamento, StatusAgendamento } from '@/services/agendamentosService'
import { extractErrorMessage } from '@/services/api'

type Status = 'loading' | 'success' | 'error'

const TAMANHO_PAGINA = 10

const statusLabel: Record<StatusAgendamento, string> = {
  AGENDADO: 'Agendado',
  CANCELADO: 'Cancelado',
  CONCLUIDO: 'Concluído',
}

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

const formatarDataHora = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function AdminAgendamentosPage() {
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([])
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [status, setStatus] = useState<Status>('loading')

  const [filtroData, setFiltroData] = useState('')
  const [filtroStatus, setFiltroStatus] = useState<StatusAgendamento | ''>('')
  const [pagina, setPagina] = useState(0)

  const [acaoEmAndamentoId, setAcaoEmAndamentoId] = useState<number | null>(null)
  const [erroPorId, setErroPorId] = useState<Record<number, string>>({})

  const carregar = useCallback(async () => {
    setStatus('loading')
    try {
      const resposta = await adminAgendamentosService.listar({
        data: filtroData || undefined,
        status: filtroStatus || undefined,
        page: pagina,
        size: TAMANHO_PAGINA,
      })
      setAgendamentos(resposta.content)
      setTotalPaginas(resposta.totalPages)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }, [filtroData, filtroStatus, pagina])

  useEffect(() => {
    carregar()
  }, [carregar])

  function alterarFiltroData(valor: string) {
    setFiltroData(valor)
    setPagina(0)
  }

  function alterarFiltroStatus(valor: StatusAgendamento | '') {
    setFiltroStatus(valor)
    setPagina(0)
  }

  function limparErro(id: number) {
    setErroPorId((atual) => {
      const resto = { ...atual }
      delete resto[id]
      return resto
    })
  }

  async function concluir(id: number) {
    setAcaoEmAndamentoId(id)
    limparErro(id)
    try {
      const atualizado = await adminAgendamentosService.concluir(id)
      setAgendamentos((atual) => atual.map((item) => (item.id === id ? atualizado : item)))
    } catch (error) {
      setErroPorId((atual) => ({ ...atual, [id]: extractErrorMessage(error) }))
    } finally {
      setAcaoEmAndamentoId(null)
    }
  }

  async function cancelar(id: number) {
    setAcaoEmAndamentoId(id)
    limparErro(id)
    try {
      const atualizado = await adminAgendamentosService.cancelar(id)
      setAgendamentos((atual) => atual.map((item) => (item.id === id ? atualizado : item)))
    } catch (error) {
      setErroPorId((atual) => ({ ...atual, [id]: extractErrorMessage(error) }))
    } finally {
      setAcaoEmAndamentoId(null)
    }
  }

  if (status === 'loading') {
    return <LoadingSpinner />
  }

  if (status === 'error') {
    return <ErrorMessage message="Não foi possível carregar os agendamentos." onRetry={carregar} />
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Agendamentos</h1>

      <div className="mt-4 flex flex-wrap items-end gap-4 rounded-lg border border-slate-200 bg-white p-4">
        <div>
          <label htmlFor="filtroData" className="block text-sm font-medium text-slate-700">
            Data
          </label>
          <input
            id="filtroData"
            type="date"
            value={filtroData}
            onChange={(e) => alterarFiltroData(e.target.value)}
            className="mt-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label htmlFor="filtroStatus" className="block text-sm font-medium text-slate-700">
            Status
          </label>
          <select
            id="filtroStatus"
            value={filtroStatus}
            onChange={(e) => alterarFiltroStatus(e.target.value as StatusAgendamento | '')}
            className="mt-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">Todos</option>
            <option value="AGENDADO">Agendado</option>
            <option value="CANCELADO">Cancelado</option>
            <option value="CONCLUIDO">Concluído</option>
          </select>
        </div>
      </div>

      {agendamentos.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">Nenhum agendamento encontrado.</p>
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
                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    disabled={acaoEmAndamentoId === agendamento.id}
                    onClick={() => concluir(agendamento.id)}
                    className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-50"
                  >
                    Concluir
                  </button>
                  <button
                    type="button"
                    disabled={acaoEmAndamentoId === agendamento.id}
                    onClick={() => cancelar(agendamento.id)}
                    className="rounded-md border border-red-600 px-4 py-2 text-sm font-medium text-red-600 disabled:opacity-50"
                  >
                    Cancelar
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <div className="mt-4 flex items-center gap-2">
        <button
          type="button"
          disabled={pagina === 0}
          onClick={() => setPagina((atual) => atual - 1)}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-50"
        >
          Anterior
        </button>
        <span className="text-sm text-slate-600">
          Página {pagina + 1} de {Math.max(totalPaginas, 1)}
        </span>
        <button
          type="button"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => setPagina((atual) => atual + 1)}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-50"
        >
          Próxima
        </button>
      </div>
    </div>
  )
}
