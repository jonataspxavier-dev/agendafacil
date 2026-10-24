import { zodResolver } from '@hookform/resolvers/zod'
import { useCallback, useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErrorMessage } from '@/components/ErrorMessage'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import { extractErrorMessage } from '@/services/api'
import * as servicosAdminService from '@/services/servicosAdminService'
import * as servicosService from '@/services/servicosService'
import type { Servico } from '@/services/servicosService'

const servicoSchema = z.object({
  nome: z.string().min(1, 'Informe o nome'),
  descricao: z.string().optional(),
  duracaoMin: z.coerce.number().int('Duração deve ser um número inteiro').positive('Duração deve ser maior que zero'),
  preco: z.coerce.number().positive('Preço deve ser maior que zero'),
})

type ServicoFormInput = z.input<typeof servicoSchema>
type ServicoFormOutput = z.output<typeof servicoSchema>

type Status = 'loading' | 'success' | 'error'

const valoresPadrao: ServicoFormInput = { nome: '', descricao: '', duracaoMin: 30, preco: 0 }

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

export function AdminServicosPage() {
  const [servicos, setServicos] = useState<Servico[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ServicoFormInput, unknown, ServicoFormOutput>({
    resolver: zodResolver(servicoSchema),
    defaultValues: valoresPadrao,
  })

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

  function iniciarEdicao(servico: Servico) {
    setEditandoId(servico.id)
    setErro(null)
    reset({
      nome: servico.nome,
      descricao: servico.descricao,
      duracaoMin: servico.duracaoMin,
      preco: servico.preco,
    })
  }

  function cancelarEdicao() {
    setEditandoId(null)
    setErro(null)
    reset(valoresPadrao)
  }

  async function onSubmit(values: ServicoFormOutput) {
    setErro(null)
    try {
      if (editandoId === null) {
        await servicosAdminService.criar(values)
      } else {
        await servicosAdminService.atualizar(editandoId, values)
      }
      cancelarEdicao()
      await carregar()
    } catch (error) {
      setErro(extractErrorMessage(error))
    }
  }

  async function desativar(id: number) {
    setErro(null)
    try {
      await servicosAdminService.desativar(id)
      setServicos((atual) => atual.filter((servico) => servico.id !== id))
      if (editandoId === id) {
        cancelarEdicao()
      }
    } catch (error) {
      setErro(extractErrorMessage(error))
    }
  }

  if (status === 'loading') {
    return <LoadingSpinner />
  }

  if (status === 'error') {
    return <ErrorMessage message="Não foi possível carregar os serviços." onRetry={carregar} />
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Serviços</h1>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="mt-4 space-y-4 rounded-lg border border-slate-200 bg-white p-4"
        noValidate
      >
        <h2 className="text-lg font-medium text-slate-900">{editandoId === null ? 'Novo serviço' : 'Editar serviço'}</h2>

        <div>
          <label htmlFor="nome" className="block text-sm font-medium text-slate-700">
            Nome
          </label>
          <input
            id="nome"
            type="text"
            {...register('nome')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          {errors.nome && <p className="mt-1 text-sm text-red-600">{errors.nome.message}</p>}
        </div>

        <div>
          <label htmlFor="descricao" className="block text-sm font-medium text-slate-700">
            Descrição
          </label>
          <input
            id="descricao"
            type="text"
            {...register('descricao')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label htmlFor="duracaoMin" className="block text-sm font-medium text-slate-700">
            Duração (min)
          </label>
          <input
            id="duracaoMin"
            type="number"
            {...register('duracaoMin')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          {errors.duracaoMin && <p className="mt-1 text-sm text-red-600">{errors.duracaoMin.message}</p>}
        </div>

        <div>
          <label htmlFor="preco" className="block text-sm font-medium text-slate-700">
            Preço
          </label>
          <input
            id="preco"
            type="number"
            step="0.01"
            {...register('preco')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          {errors.preco && <p className="mt-1 text-sm text-red-600">{errors.preco.message}</p>}
        </div>

        {erro && <p className="text-sm text-red-600">{erro}</p>}

        <div className="flex gap-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            {editandoId === null ? 'Criar' : 'Salvar'}
          </button>
          {editandoId !== null && (
            <button
              type="button"
              onClick={cancelarEdicao}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Cancelar
            </button>
          )}
        </div>
      </form>

      <ul className="mt-6 space-y-4">
        {servicos.map((servico) => (
          <li key={servico.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-lg font-medium text-slate-900">{servico.nome}</h2>
            <p className="mt-1 text-sm text-slate-500">{servico.descricao}</p>
            <p className="mt-2 text-sm text-slate-600">
              {servico.duracaoMin} min · {formatarPreco(servico.preco)}
            </p>
            <div className="mt-3 flex gap-2">
              <button
                type="button"
                onClick={() => iniciarEdicao(servico)}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
              >
                Editar
              </button>
              <button
                type="button"
                onClick={() => desativar(servico.id)}
                className="rounded-md border border-red-600 px-4 py-2 text-sm font-medium text-red-600"
              >
                Desativar
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
