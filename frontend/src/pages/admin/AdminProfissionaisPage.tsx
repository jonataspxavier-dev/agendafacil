import { zodResolver } from '@hookform/resolvers/zod'
import { useCallback, useEffect, useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErrorMessage } from '@/components/ErrorMessage'
import { LoadingSpinner } from '@/components/LoadingSpinner'
import { extractErrorMessage } from '@/services/api'
import * as profissionaisAdminService from '@/services/profissionaisAdminService'
import * as profissionaisService from '@/services/profissionaisService'
import type { DiaSemana, Profissional } from '@/services/profissionaisService'
import * as servicosService from '@/services/servicosService'
import type { Servico } from '@/services/servicosService'

const DIAS_SEMANA: DiaSemana[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

const diaSemanaLabel: Record<DiaSemana, string> = {
  MONDAY: 'Segunda-feira',
  TUESDAY: 'Terça-feira',
  WEDNESDAY: 'Quarta-feira',
  THURSDAY: 'Quinta-feira',
  FRIDAY: 'Sexta-feira',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
}

const jornadaRowSchema = z.object({
  diaSemana: z.enum(DIAS_SEMANA as [DiaSemana, ...DiaSemana[]]),
  horaInicio: z.string().min(1, 'Informe o horário de início'),
  horaFim: z.string().min(1, 'Informe o horário de término'),
})

const profissionalSchema = z.object({
  nome: z.string().min(1, 'Informe o nome'),
  servicoIds: z.array(z.coerce.number()),
  jornada: z.array(jornadaRowSchema),
})

type ProfissionalFormInput = z.input<typeof profissionalSchema>
type ProfissionalFormOutput = z.output<typeof profissionalSchema>

type Status = 'loading' | 'success' | 'error'

const valoresPadrao: ProfissionalFormInput = { nome: '', servicoIds: [], jornada: [] }

const paraHoraInput = (hora: string) => hora.slice(0, 5)

export function AdminProfissionaisPage() {
  const [profissionais, setProfissionais] = useState<Profissional[]>([])
  const [servicos, setServicos] = useState<Servico[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ProfissionalFormInput, unknown, ProfissionalFormOutput>({
    resolver: zodResolver(profissionalSchema),
    defaultValues: valoresPadrao,
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'jornada' })

  const carregar = useCallback(async () => {
    setStatus('loading')
    try {
      const [profissionaisData, servicosData] = await Promise.all([
        profissionaisService.listar(),
        servicosService.listar(),
      ])
      setProfissionais(profissionaisData)
      setServicos(servicosData)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    carregar()
  }, [carregar])

  function iniciarEdicao(profissional: Profissional) {
    setEditandoId(profissional.id)
    setErro(null)
    reset({
      nome: profissional.nome,
      servicoIds: profissional.servicoIds.map(String),
      jornada: profissional.jornada.map((j) => ({
        diaSemana: j.diaSemana,
        horaInicio: paraHoraInput(j.horaInicio),
        horaFim: paraHoraInput(j.horaFim),
      })),
    })
  }

  function cancelarEdicao() {
    setEditandoId(null)
    setErro(null)
    reset(valoresPadrao)
  }

  async function onSubmit(values: ProfissionalFormOutput) {
    setErro(null)
    try {
      if (editandoId === null) {
        await profissionaisAdminService.criar(values)
      } else {
        await profissionaisAdminService.atualizar(editandoId, values)
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
      await profissionaisAdminService.desativar(id)
      setProfissionais((atual) => atual.filter((profissional) => profissional.id !== id))
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
    return <ErrorMessage message="Não foi possível carregar os profissionais." onRetry={carregar} />
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Profissionais</h1>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="mt-4 space-y-4 rounded-lg border border-slate-200 bg-white p-4"
        noValidate
      >
        <h2 className="text-lg font-medium text-slate-900">
          {editandoId === null ? 'Novo profissional' : 'Editar profissional'}
        </h2>

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
          <span className="block text-sm font-medium text-slate-700">Serviços oferecidos</span>
          <div className="mt-1 space-y-1">
            {servicos.map((servico) => (
              <label key={servico.id} className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" value={servico.id} {...register('servicoIds')} />
                {servico.nome}
              </label>
            ))}
          </div>
        </div>

        <div>
          <span className="block text-sm font-medium text-slate-700">Jornada</span>
          <div className="mt-2 space-y-3">
            {fields.map((field, index) => (
              <fieldset key={field.id} className="rounded-md border border-slate-200 p-3">
                <legend className="sr-only">Horário {index + 1}</legend>
                <div className="flex flex-wrap items-end gap-2">
                  <div>
                    <label htmlFor={`jornada.${index}.diaSemana`} className="block text-xs font-medium text-slate-600">
                      Dia da semana
                    </label>
                    <select
                      id={`jornada.${index}.diaSemana`}
                      aria-label={`Dia da semana (horário ${index + 1})`}
                      {...register(`jornada.${index}.diaSemana`)}
                      className="mt-1 rounded-md border border-slate-300 px-2 py-1 text-sm"
                    >
                      {DIAS_SEMANA.map((dia) => (
                        <option key={dia} value={dia}>
                          {diaSemanaLabel[dia]}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor={`jornada.${index}.horaInicio`} className="block text-xs font-medium text-slate-600">
                      Início
                    </label>
                    <input
                      id={`jornada.${index}.horaInicio`}
                      type="time"
                      aria-label={`Início (horário ${index + 1})`}
                      {...register(`jornada.${index}.horaInicio`)}
                      className="mt-1 rounded-md border border-slate-300 px-2 py-1 text-sm"
                    />
                  </div>
                  <div>
                    <label htmlFor={`jornada.${index}.horaFim`} className="block text-xs font-medium text-slate-600">
                      Fim
                    </label>
                    <input
                      id={`jornada.${index}.horaFim`}
                      type="time"
                      aria-label={`Fim (horário ${index + 1})`}
                      {...register(`jornada.${index}.horaFim`)}
                      className="mt-1 rounded-md border border-slate-300 px-2 py-1 text-sm"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    aria-label={`Remover horário ${index + 1}`}
                    className="rounded-md border border-red-600 px-3 py-1 text-sm font-medium text-red-600"
                  >
                    Remover
                  </button>
                </div>
              </fieldset>
            ))}
          </div>
          <button
            type="button"
            onClick={() => append({ diaSemana: 'MONDAY', horaInicio: '', horaFim: '' })}
            className="mt-2 rounded-md border border-slate-300 px-3 py-1 text-sm font-medium text-slate-700"
          >
            Adicionar horário
          </button>
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
        {profissionais.map((profissional) => (
          <li key={profissional.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-lg font-medium text-slate-900">{profissional.nome}</h2>
            <p className="mt-1 text-sm text-slate-600">
              {profissional.servicoIds
                .map((id) => servicos.find((servico) => servico.id === id)?.nome)
                .filter(Boolean)
                .join(', ') || 'Nenhum serviço associado'}
            </p>
            <div className="mt-3 flex gap-2">
              <button
                type="button"
                onClick={() => iniciarEdicao(profissional)}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
              >
                Editar
              </button>
              <button
                type="button"
                onClick={() => desativar(profissional.id)}
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
