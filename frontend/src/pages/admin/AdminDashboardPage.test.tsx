import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as dashboardService from '@/services/dashboardService'
import { AdminDashboardPage } from './AdminDashboardPage'

vi.mock('@/services/dashboardService')

describe('AdminDashboardPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a loading state before the dashboard arrives', () => {
    vi.mocked(dashboardService.obter).mockReturnValue(new Promise(() => {}))

    render(<AdminDashboardPage />)

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('renders the three figures once loaded', async () => {
    vi.mocked(dashboardService.obter).mockResolvedValue({
      agendamentosHoje: 4,
      faturamentoMes: 350.5,
      topServicos: [
        { servicoId: 1, nome: 'Corte de Cabelo', quantidade: 3 },
        { servicoId: 2, nome: 'Manicure', quantidade: 1 },
      ],
    })

    render(<AdminDashboardPage />)

    await waitFor(() => {
      expect(screen.getByText('4')).toBeInTheDocument()
    })
    expect(screen.getByText('R$ 350,50')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Corte de Cabelo' })).toHaveAttribute('aria-valuenow', '3')
    expect(screen.getByRole('progressbar', { name: 'Manicure' })).toHaveAttribute('aria-valuenow', '1')
  })

  it('shows an error message with a retry action when loading fails', async () => {
    vi.mocked(dashboardService.obter).mockRejectedValue(new Error('network error'))

    render(<AdminDashboardPage />)

    await waitFor(() => {
      expect(screen.getByText(/não foi possível/i)).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument()
  })
})
