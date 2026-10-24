import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ErrorMessage } from '@/components/ErrorMessage'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import * as servicosService from '@/services/servicosService'
import type { Servico } from '@/services/servicosService'

type Status = 'loading' | 'success' | 'error'

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

export function ServicosPage() {
  const [servicos, setServicos] = useState<Servico[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const navigate = useNavigate()

  const carregar = useCallback(async () => {
    setStatus('loading')
    try {
      const data = await servicosService.listar()
      setServicos(data)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    carregar()
  }, [carregar])

  function agendar(servicoId: number) {
    navigate('/agendar', { state: { servicoId } })
  }

  if (status === 'loading') {
    return <LoadingSpinner label="Carregando serviços..." />
  }

  if (status === 'error') {
    return <ErrorMessage message="Não foi possível carregar os serviços." onRetry={carregar} />
  }

  return (
    <div className="mx-auto max-w-3xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Serviços</h1>
      <ul className="mt-4 space-y-4">
        {servicos.map((servico) => (
          <li key={servico.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-lg font-medium text-slate-900">{servico.nome}</h2>
            <p className="mt-1 text-sm text-slate-500">{servico.descricao}</p>
            <p className="mt-2 text-sm text-slate-600">
              {servico.duracaoMin} min · {formatarPreco(servico.preco)}
            </p>
            <button
              type="button"
              onClick={() => agendar(servico.id)}
              className="mt-3 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white"
            >
              Agendar
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
