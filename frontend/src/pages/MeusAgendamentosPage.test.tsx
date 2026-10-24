import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as agendamentosService from '@/services/agendamentosService'
import { MeusAgendamentosPage } from './MeusAgendamentosPage'

vi.mock('@/services/agendamentosService')

const agendamentosMock = [
  {
    id: 1,
    profissionalId: 10,
    profissionalNome: 'Ana Souza',
    servicoId: 1,
    servicoNome: 'Corte de Cabelo',
    inicio: '2026-11-02T09:00:00',
    fim: '2026-11-02T09:30:00',
    status: 'AGENDADO' as const,
    precoCobrado: 50,
  },
  {
    id: 2,
    profissionalId: 20,
    profissionalNome: 'Bruno Lima',
    servicoId: 2,
    servicoNome: 'Manicure',
    inicio: '2026-11-03T10:00:00',
    fim: '2026-11-03T10:40:00',
    status: 'CONCLUIDO' as const,
    precoCobrado: 35,
  },
]

describe('MeusAgendamentosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a loading state before the bookings arrive', () => {
    vi.mocked(agendamentosService.listarMeus).mockReturnValue(new Promise(() => {}))

    render(<MeusAgendamentosPage />)

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('shows an error message with a retry action when loading fails', async () => {
    vi.mocked(agendamentosService.listarMeus).mockRejectedValue(new Error('network error'))

    render(<MeusAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText(/não foi possível/i)).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument()
  })

  it('renders bookings and only shows Cancelar for AGENDADO ones', async () => {
    vi.mocked(agendamentosService.listarMeus).mockResolvedValue(agendamentosMock)

    render(<MeusAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    expect(screen.getByText('Manicure')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /cancelar/i })).toHaveLength(1)
  })

  it('cancels a booking and updates its status in place', async () => {
    vi.mocked(agendamentosService.listarMeus).mockResolvedValue(agendamentosMock)
    vi.mocked(agendamentosService.cancelar).mockResolvedValue({
      ...agendamentosMock[0],
      status: 'CANCELADO',
    })

    render(<MeusAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    await waitFor(() => {
      expect(agendamentosService.cancelar).toHaveBeenCalledWith(1)
    })
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /cancelar/i })).not.toBeInTheDocument()
    })
    expect(screen.getByText(/cancelado/i)).toBeInTheDocument()
  })

  it('shows the backend error message inline when cancelling fails', async () => {
    vi.mocked(agendamentosService.listarMeus).mockResolvedValue(agendamentosMock)
    vi.mocked(agendamentosService.cancelar).mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Cancelamento só é permitido até 2 horas antes do horário agendado' } },
    })

    render(<MeusAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    await waitFor(() => {
      expect(
        screen.getByText('Cancelamento só é permitido até 2 horas antes do horário agendado'),
      ).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeInTheDocument()
  })
})
