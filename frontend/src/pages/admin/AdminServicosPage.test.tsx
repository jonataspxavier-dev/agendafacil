import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as servicosAdminService from '@/services/servicosAdminService'
import * as servicosService from '@/services/servicosService'
import { AdminServicosPage } from './AdminServicosPage'

vi.mock('@/services/servicosService')
vi.mock('@/services/servicosAdminService')

const servicosMock = [
  { id: 1, nome: 'Corte de Cabelo', descricao: 'Corte feminino ou masculino', duracaoMin: 30, preco: 50, ativo: true },
  { id: 2, nome: 'Manicure', descricao: 'Cuidado das unhas', duracaoMin: 40, preco: 35, ativo: true },
]

describe('AdminServicosPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(servicosService.listar).mockResolvedValue(servicosMock)
  })

  it('shows a loading state before the services arrive', () => {
    vi.mocked(servicosService.listar).mockReturnValue(new Promise(() => {}))

    render(<AdminServicosPage />)

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('renders the service list once loaded', async () => {
    render(<AdminServicosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    expect(screen.getByText('Manicure')).toBeInTheDocument()
  })

  it('shows validation errors and never calls criar when the form is invalid', async () => {
    render(<AdminServicosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /^criar$/i }))

    await waitFor(() => {
      expect(screen.getByText(/informe o nome/i)).toBeInTheDocument()
    })
    expect(servicosAdminService.criar).not.toHaveBeenCalled()
  })

  it('creates a new service with the right payload and reloads the list', async () => {
    vi.mocked(servicosAdminService.criar).mockResolvedValue({
      id: 3,
      nome: 'Depilação',
      descricao: 'Depilação com cera',
      duracaoMin: 45,
      preco: 60,
      ativo: true,
    })
    vi.mocked(servicosService.listar)
      .mockResolvedValueOnce(servicosMock)
      .mockResolvedValueOnce([
        ...servicosMock,
        { id: 3, nome: 'Depilação', descricao: 'Depilação com cera', duracaoMin: 45, preco: 60, ativo: true },
      ])

    render(<AdminServicosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: 'Depilação' } })
    fireEvent.change(screen.getByLabelText(/descrição/i), { target: { value: 'Depilação com cera' } })
    fireEvent.change(screen.getByLabelText(/duração/i), { target: { value: '45' } })
    fireEvent.change(screen.getByLabelText(/preço/i), { target: { value: '60' } })
    fireEvent.click(screen.getByRole('button', { name: /^criar$/i }))

    await waitFor(() => {
      expect(servicosAdminService.criar).toHaveBeenCalledWith({
        nome: 'Depilação',
        descricao: 'Depilação com cera',
        duracaoMin: 45,
        preco: 60,
      })
    })
    await waitFor(() => {
      expect(screen.getByText('Depilação')).toBeInTheDocument()
    })
  })

  it('pre-fills the form when editing and submits an update with that id', async () => {
    vi.mocked(servicosAdminService.atualizar).mockResolvedValue({ ...servicosMock[0], preco: 55 })

    render(<AdminServicosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    const linhas = screen.getAllByRole('button', { name: /editar/i })
    fireEvent.click(linhas[0])

    expect(screen.getByLabelText(/^nome/i)).toHaveValue('Corte de Cabelo')
    expect(screen.getByLabelText(/preço/i)).toHaveValue(50)

    fireEvent.change(screen.getByLabelText(/preço/i), { target: { value: '55' } })
    fireEvent.click(screen.getByRole('button', { name: /^salvar$/i }))

    await waitFor(() => {
      expect(servicosAdminService.atualizar).toHaveBeenCalledWith(1, {
        nome: 'Corte de Cabelo',
        descricao: 'Corte feminino ou masculino',
        duracaoMin: 30,
        preco: 55,
      })
    })
  })

  it('deactivates a service and removes it from the visible list', async () => {
    vi.mocked(servicosAdminService.desativar).mockResolvedValue(undefined)

    render(<AdminServicosPage />)

    await waitFor(() => {
      expect(screen.getByText('Corte de Cabelo')).toBeInTheDocument()
    })

    const botoesDesativar = screen.getAllByRole('button', { name: /desativar/i })
    fireEvent.click(botoesDesativar[0])

    await waitFor(() => {
      expect(servicosAdminService.desativar).toHaveBeenCalledWith(1)
    })
    await waitFor(() => {
      expect(screen.queryByText('Corte de Cabelo')).not.toBeInTheDocument()
    })
    expect(screen.getByText('Manicure')).toBeInTheDocument()
  })
})
