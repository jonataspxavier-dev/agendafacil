import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as servicosService from '@/services/servicosService'
import { ServicosPage } from './ServicosPage'

vi.mock('@/services/servicosService')

const servicosMock = [
  { id: 1, nome: 'Corte de Cabelo', descricao: 'Corte feminino ou masculino', duracaoMin: 30, preco: 50, ativo: true },
  { id: 2, nome: 'Manicure', descricao: 'Cuidado das unhas', duracaoMin: 40, preco: 35, ativo: true },
]

describe('ServicosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a loading state before the services arrive', () => {
    vi.mocked(servicosService.listar).mockReturnValue(new Promise(() => {}))

    render(
      <MemoryRouter>
        <ServicosPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('renders the service list once loaded', async () => {
    vi.mocked(servicosService.listar).mockResolvedValue(servicosMock)

    render(
      <MemoryRouter>
        <ServicosPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    expect(screen.getByText('Manicure')).toBeInTheDocument()
  })

  it('shows an error message with a retry action when loading fails', async () => {
    vi.mocked(servicosService.listar).mockRejectedValue(new Error('network error'))

    render(
      <MemoryRouter>
        <ServicosPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText(/não foi possível/i)).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument()
  })
})
