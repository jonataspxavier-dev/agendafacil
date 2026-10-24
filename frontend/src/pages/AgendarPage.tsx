import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import * as agendamentosService from '@/services/agendamentosService'
import { extractErrorMessage } from '@/services/api'
import * as disponibilidadeService from '@/services/disponibilidadeService'
import type { Slot } from '@/services/disponibilidadeService'
import * as profissionaisService from '@/services/profissionaisService'
import type { Profissional } from '@/services/profissionaisService'
import * as servicosService from '@/services/servicosService'
import type { Servico } from '@/services/servicosService'

type Step = 1 | 2 | 3 | 4 | 5
type PageStatus = 'loading' | 'success' | 'error'
type SlotsStatus = 'idle' | 'loading' | 'success' | 'error'

const formatarPreco = (preco: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(preco)

const formatarHorario = (iso: string) =>
  new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

const formatarData = (data: string) => new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')

function hojeISO(): string {
  const hoje = new Date()
  const ano = hoje.getFullYear()
  const mes = String(hoje.getMonth() + 1).padStart(2, '0')
  const dia = String(hoje.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

const botaoBase = 'w-full rounded-md border px-4 py-2 text-left text-sm'
const botaoSelecionado = 'border-slate-900 bg-slate-100'
const botaoNaoSelecionado = 'border-slate-300'

export function AgendarPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const [status, setStatus] = useState<PageStatus>('loading')
  const [servicos, setServicos] = useState<Servico[]>([])
  const [profissionais, setProfissionais] = useState<Profissional[]>([])

  const [step, setStep] = useState<Step>(1)
  const [servicoId, setServicoId] = useState<number | undefined>(
    (location.state as { servicoId?: number } | null)?.servicoId,
  )
  const [profissionalId, setProfissionalId] = useState<number | undefined>()
  const [data, setData] = useState('')
  const [inicio, setInicio] = useState('')

  const [slots, setSlots] = useState<Slot[]>([])
  const [slotsStatus, setSlotsStatus] = useState<SlotsStatus>('idle')

  const [confirmando, setConfirmando] = useState(false)
  const [erroConfirmacao, setErroConfirmacao] = useState<string | null>(null)

  useEffect(() => {
    carregarListas()
  }, [])

  async function carregarListas() {
    setStatus('loading')
    try {
      const [servicosData, profissionaisData] = await Promise.all([
        servicosService.listar(),
        profissionaisService.listar(),
      ])
      setServicos(servicosData)
      setProfissionais(profissionaisData)
      setStatus('success')
    } catch {
      setStatus('error')
    }
  }

  const profissionaisDoServico = useMemo(
    () => profissionais.filter((p) => servicoId !== undefined && p.servicoIds.includes(servicoId)),
    [profissionais, servicoId],
  )

  const servicoSelecionado = servicos.find((s) => s.id === servicoId)
  const profissionalSelecionado = profissionais.find((p) => p.id === profissionalId)

  useEffect(() => {
    if (step !== 4 || profissionalId === undefined || servicoId === undefined || !data) {
      return
    }
    let active = true
    setSlotsStatus('loading')
    disponibilidadeService
      .buscar(profissionalId, servicoId, data)
      .then((resposta) => {
        if (active) {
          setSlots(resposta.slots)
          setSlotsStatus('success')
        }
      })
      .catch(() => {
        if (active) setSlotsStatus('error')
      })
    return () => {
      active = false
    }
  }, [step, profissionalId, servicoId, data])

  function selecionarServico(id: number) {
    setServicoId(id)
    setProfissionalId(undefined)
    setData('')
    setInicio('')
  }

  function selecionarProfissional(id: number) {
    setProfissionalId(id)
    setInicio('')
  }

  function alterarData(novaData: string) {
    setData(novaData)
    setInicio('')
  }

  async function confirmar() {
    if (profissionalId === undefined || servicoId === undefined || !inicio) {
      return
    }
    setConfirmando(true)
    setErroConfirmacao(null)
    try {
      await agendamentosService.criar(profissionalId, servicoId, inicio)
      navigate('/meus-agendamentos')
    } catch (error) {
      setErroConfirmacao(extractErrorMessage(error))
    } finally {
      setConfirmando(false)
    }
  }

  if (status === 'loading') {
    return <p className="p-6 text-slate-600">Carregando...</p>
  }

  if (status === 'error') {
    return (
      <div className="p-6">
        <p className="text-red-600">Não foi possível carregar os dados do agendamento.</p>
        <button
          type="button"
          onClick={carregarListas}
          className="mt-2 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white"
        >
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Agendar</h1>

      {step === 1 && (
        <section className="mt-4">
          <h2 className="text-lg font-medium text-slate-900">1. Escolha o serviço</h2>
          <ul className="mt-2 space-y-2">
            {servicos.map((servico) => (
              <li key={servico.id}>
                <button
                  type="button"
                  onClick={() => selecionarServico(servico.id)}
                  aria-pressed={servico.id === servicoId}
                  className={`${botaoBase} ${servico.id === servicoId ? botaoSelecionado : botaoNaoSelecionado}`}
                >
                  {servico.nome} · {formatarPreco(servico.preco)}
                </button>
              </li>
            ))}
          </ul>
          <div className="mt-4">
            <button
              type="button"
              disabled={servicoId === undefined}
              onClick={() => setStep(2)}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Avançar
            </button>
          </div>
        </section>
      )}

      {step === 2 && (
        <section className="mt-4">
          <h2 className="text-lg font-medium text-slate-900">2. Escolha o profissional</h2>
          {profissionaisDoServico.length === 0 ? (
            <p className="mt-2 text-sm text-slate-600">Nenhum profissional disponível para este serviço.</p>
          ) : (
            <ul className="mt-2 space-y-2">
              {profissionaisDoServico.map((profissional) => (
                <li key={profissional.id}>
                  <button
                    type="button"
                    onClick={() => selecionarProfissional(profissional.id)}
                    aria-pressed={profissional.id === profissionalId}
                    className={`${botaoBase} ${profissional.id === profissionalId ? botaoSelecionado : botaoNaoSelecionado}`}
                  >
                    {profissional.nome}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Voltar
            </button>
            <button
              type="button"
              disabled={profissionalId === undefined}
              onClick={() => setStep(3)}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Avançar
            </button>
          </div>
        </section>
      )}

      {step === 3 && (
        <section className="mt-4">
          <h2 className="text-lg font-medium text-slate-900">3. Escolha a data</h2>
          <label htmlFor="data" className="mt-2 block text-sm font-medium text-slate-700">
            Data
          </label>
          <input
            id="data"
            type="date"
            min={hojeISO()}
            value={data}
            onChange={(e) => alterarData(e.target.value)}
            className="mt-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={() => setStep(2)}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Voltar
            </button>
            <button
              type="button"
              disabled={!data}
              onClick={() => setStep(4)}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Avançar
            </button>
          </div>
        </section>
      )}

      {step === 4 && (
        <section className="mt-4">
          <h2 className="text-lg font-medium text-slate-900">4. Escolha o horário</h2>
          {slotsStatus === 'loading' && <p className="mt-2 text-sm text-slate-600">Carregando horários...</p>}
          {slotsStatus === 'error' && <p className="mt-2 text-sm text-red-600">Não foi possível carregar os horários.</p>}
          {slotsStatus === 'success' && slots.length === 0 && (
            <p className="mt-2 text-sm text-slate-600">Nenhum horário disponível para esta data.</p>
          )}
          {slotsStatus === 'success' && slots.length > 0 && (
            <ul className="mt-2 grid grid-cols-3 gap-2">
              {slots.map((slot) => (
                <li key={slot.inicio}>
                  <button
                    type="button"
                    onClick={() => setInicio(slot.inicio)}
                    aria-pressed={slot.inicio === inicio}
                    className={`rounded-md border px-3 py-2 text-sm ${
                      slot.inicio === inicio ? botaoSelecionado : botaoNaoSelecionado
                    }`}
                  >
                    {formatarHorario(slot.inicio)}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={() => setStep(3)}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Voltar
            </button>
            <button
              type="button"
              disabled={!inicio}
              onClick={() => setStep(5)}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              Avançar
            </button>
          </div>
        </section>
      )}

      {step === 5 && (
        <section className="mt-4">
          <h2 className="text-lg font-medium text-slate-900">5. Confirmar</h2>
          <dl className="mt-2 space-y-1 text-sm text-slate-700">
            <div>
              <dt className="inline font-medium">Serviço: </dt>
              <dd className="inline">{servicoSelecionado?.nome}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Profissional: </dt>
              <dd className="inline">{profissionalSelecionado?.nome}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Data: </dt>
              <dd className="inline">
                {formatarData(data)} às {formatarHorario(inicio)}
              </dd>
            </div>
            <div>
              <dt className="inline font-medium">Preço: </dt>
              <dd className="inline">{servicoSelecionado ? formatarPreco(servicoSelecionado.preco) : ''}</dd>
            </div>
          </dl>
          {erroConfirmacao && <p className="mt-2 text-sm text-red-600">{erroConfirmacao}</p>}
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={() => setStep(4)}
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
            >
              Voltar
            </button>
            <button
              type="button"
              disabled={confirmando}
              onClick={confirmar}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              {confirmando ? 'Confirmando...' : 'Confirmar agendamento'}
            </button>
          </div>
        </section>
      )}
    </div>
  )
}
