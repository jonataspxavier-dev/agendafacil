import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as adminAgendamentosService from '@/services/adminAgendamentosService'
import { AdminAgendamentosPage } from './AdminAgendamentosPage'

vi.mock('@/services/adminAgendamentosService')

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

const paginaMock = { content: agendamentosMock, page: 0, size: 10, totalElements: 2, totalPages: 1 }

describe('AdminAgendamentosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(adminAgendamentosService.listar).mockResolvedValue(paginaMock)
  })

  it('shows a loading state before the bookings arrive', () => {
    vi.mocked(adminAgendamentosService.listar).mockReturnValue(new Promise(() => {}))

    render(<AdminAgendamentosPage />)

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('shows an error message with a retry action when loading fails', async () => {
    vi.mocked(adminAgendamentosService.listar).mockRejectedValue(new Error('network error'))

    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText(/não foi possível/i)).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument()
  })

  it('renders the booking list once loaded', async () => {
    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    expect(screen.getByText('Manicure')).toBeInTheDocument()
  })

  it('only shows Concluir/Cancelar on AGENDADO bookings', async () => {
    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    expect(screen.getAllByRole('button', { name: /concluir/i })).toHaveLength(1)
    expect(screen.getAllByRole('button', { name: /cancelar/i })).toHaveLength(1)
  })

  it('changing the data filter resets to page 0 and calls listar with the new params', async () => {
    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/^data/i), { target: { value: '2026-11-02' } })

    await waitFor(() => {
      expect(adminAgendamentosService.listar).toHaveBeenLastCalledWith({
        data: '2026-11-02',
        status: undefined,
        page: 0,
        size: 10,
      })
    })
  })

  it('changing the status filter calls listar with the new params', async () => {
    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/status/i), { target: { value: 'CANCELADO' } })

    await waitFor(() => {
      expect(adminAgendamentosService.listar).toHaveBeenLastCalledWith({
        data: undefined,
        status: 'CANCELADO',
        page: 0,
        size: 10,
      })
    })
  })

  it('paginates: Próxima advances the page, Anterior is disabled on the first page', async () => {
    vi.mocked(adminAgendamentosService.listar).mockResolvedValue({
      ...paginaMock,
      totalPages: 2,
    })

    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    expect(screen.getByRole('button', { name: /anterior/i })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: /próxima/i }))

    await waitFor(() => {
      expect(adminAgendamentosService.listar).toHaveBeenLastCalledWith({
        data: undefined,
        status: undefined,
        page: 1,
        size: 10,
      })
    })
  })

  it('concludes an AGENDADO booking and updates it in place', async () => {
    vi.mocked(adminAgendamentosService.concluir).mockResolvedValue({ ...agendamentosMock[0], status: 'CONCLUIDO' })

    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /concluir/i }))

    await waitFor(() => {
      expect(adminAgendamentosService.concluir).toHaveBeenCalledWith(1)
    })
    await waitFor(() => {
      expect(screen.queryAllByRole('button', { name: /^concluir$/i })).toHaveLength(0)
    })
  })

  it('shows the backend error inline when cancelling fails', async () => {
    vi.mocked(adminAgendamentosService.cancelar).mockRejectedValue({
      isAxiosError: true,
      response: { data: { message: 'Cancelamento só é permitido até 2 horas antes do horário agendado' } },
    })

    render(<AdminAgendamentosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    await waitFor(() => {
      expect(
        screen.getByText('Cancelamento só é permitido até 2 horas antes do horário agendado'),
      ).toBeInTheDocument()
    })
  })
})
