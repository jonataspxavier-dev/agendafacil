import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as agendamentosService from '@/services/agendamentosService'
import * as disponibilidadeService from '@/services/disponibilidadeService'
import * as profissionaisService from '@/services/profissionaisService'
import * as servicosService from '@/services/servicosService'
import { AgendarPage } from './AgendarPage'

vi.mock('@/services/servicosService')
vi.mock('@/services/profissionaisService')
vi.mock('@/services/disponibilidadeService')
vi.mock('@/services/agendamentosService')

const servicosMock = [
  { id: 1, nome: 'Corte de Cabelo', descricao: 'desc', duracaoMin: 30, preco: 50, ativo: true },
  { id: 2, nome: 'Manicure', descricao: 'desc', duracaoMin: 40, preco: 35, ativo: true },
]

const profissionaisMock = [
  { id: 10, nome: 'Ana Souza', ativo: true, servicoIds: [1], jornada: [] },
  { id: 20, nome: 'Bruno Lima', ativo: true, servicoIds: [2], jornada: [] },
]

const slotsMock = [
  { inicio: '2026-11-02T09:00:00', fim: '2026-11-02T09:30:00' },
  { inicio: '2026-11-02T09:30:00', fim: '2026-11-02T10:00:00' },
]

const avancarButton = () => screen.getByRole('button', { name: /avançar/i })
const servicoButton = () => screen.getByRole('button', { name: /corte de cabelo/i })
const profissionalButton = () => screen.getByRole('button', { name: /ana souza/i })
const slotButton = () => screen.getByRole('button', { name: '09:00' })

function renderAgendarPage(routeState?: { servicoId?: number }) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/agendar', state: routeState }]}>
      <Routes>
        <Route path="/agendar" element={<AgendarPage />} />
        <Route path="/meus-agendamentos" element={<div>Página meus agendamentos</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function avancarAteHorario() {
  await waitFor(() => {
    expect(servicoButton()).toBeInTheDocument()
  })
  fireEvent.click(servicoButton())
  fireEvent.click(avancarButton())

  await waitFor(() => {
    expect(profissionalButton()).toBeInTheDocument()
  })
  fireEvent.click(profissionalButton())
  fireEvent.click(avancarButton())

  fireEvent.change(screen.getByLabelText(/data/i), { target: { value: '2026-11-02' } })
  fireEvent.click(avancarButton())

  await waitFor(() => {
    expect(slotButton()).toBeInTheDocument()
  })
}

describe('AgendarPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(servicosService.listar).mockResolvedValue(servicosMock)
    vi.mocked(profissionaisService.listar).mockResolvedValue(profissionaisMock)
    vi.mocked(disponibilidadeService.buscar).mockResolvedValue({
      profissionalId: 10,
      servicoId: 1,
      data: '2026-11-02',
      slots: slotsMock,
    })
  })

  it('pre-selects the service from route state and lets Avançar proceed immediately', async () => {
    renderAgendarPage({ servicoId: 1 })

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    expect(avancarButton()).toBeEnabled()
  })

  it('disables Avançar on step 1 until a service is chosen', async () => {
    renderAgendarPage()

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    expect(avancarButton()).toBeDisabled()

    fireEvent.click(servicoButton())
    expect(avancarButton()).toBeEnabled()
  })

  it('only lists professionals that offer the selected service', async () => {
    renderAgendarPage({ servicoId: 1 })

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(profissionalButton()).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: /bruno lima/i })).not.toBeInTheDocument()
  })

  it('disables Avançar on step 2 until a professional is chosen', async () => {
    renderAgendarPage({ servicoId: 1 })

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(profissionalButton()).toBeInTheDocument()
    })
    expect(avancarButton()).toBeDisabled()

    fireEvent.click(profissionalButton())
    expect(avancarButton()).toBeEnabled()
  })

  it('disables Avançar on step 3 until a date is chosen', async () => {
    renderAgendarPage({ servicoId: 1 })

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(profissionalButton()).toBeInTheDocument()
    })
    fireEvent.click(profissionalButton())
    fireEvent.click(avancarButton())

    expect(screen.getByLabelText(/data/i)).toBeInTheDocument()
    expect(avancarButton()).toBeDisabled()

    fireEvent.change(screen.getByLabelText(/data/i), { target: { value: '2026-11-02' } })
    expect(avancarButton()).toBeEnabled()
  })

  it('disables Avançar on step 4 until a slot is chosen', async () => {
    renderAgendarPage({ servicoId: 1 })

    await waitFor(() => {
      expect(servicoButton()).toBeInTheDocument()
    })
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(profissionalButton()).toBeInTheDocument()
    })
    fireEvent.click(profissionalButton())
    fireEvent.click(avancarButton())

    fireEvent.change(screen.getByLabelText(/data/i), { target: { value: '2026-11-02' } })
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(slotButton()).toBeInTheDocument()
    })
    expect(avancarButton()).toBeDisabled()

    fireEvent.click(slotButton())
    expect(avancarButton()).toBeEnabled()
  })

  it('submits exactly the selected ids/time on confirm and redirects to /meus-agendamentos', async () => {
    const criar = vi.fn().mockResolvedValue({ id: 99 })
    vi.mocked(agendamentosService.criar).mockImplementation(criar)

    renderAgendarPage({ servicoId: 1 })
    await avancarAteHorario()

    fireEvent.click(slotButton())
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /confirmar agendamento/i })).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /confirmar agendamento/i }))

    await waitFor(() => {
      expect(criar).toHaveBeenCalledWith(10, 1, '2026-11-02T09:00:00')
    })
    await waitFor(() => {
      expect(screen.getByText('Página meus agendamentos')).toBeInTheDocument()
    })
  })

  it('shows the backend error message inline when the booking fails', async () => {
    vi.mocked(agendamentosService.criar).mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Horário indisponível para este profissional' } },
    })

    renderAgendarPage({ servicoId: 1 })
    await avancarAteHorario()

    fireEvent.click(slotButton())
    fireEvent.click(avancarButton())

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /confirmar agendamento/i })).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /confirmar agendamento/i }))

    await waitFor(() => {
      expect(screen.getByText('Horário indisponível para este profissional')).toBeInTheDocument()
    })
  })
})
