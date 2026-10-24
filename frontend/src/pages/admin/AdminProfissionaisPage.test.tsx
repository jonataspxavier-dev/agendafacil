import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as profissionaisAdminService from '@/services/profissionaisAdminService'
import * as profissionaisService from '@/services/profissionaisService'
import * as servicosService from '@/services/servicosService'
import { AdminProfissionaisPage } from './AdminProfissionaisPage'

vi.mock('@/services/servicosService')
vi.mock('@/services/profissionaisService')
vi.mock('@/services/profissionaisAdminService')

const servicosMock = [
  { id: 1, nome: 'Corte de Cabelo', descricao: '', duracaoMin: 30, preco: 50, ativo: true },
  { id: 2, nome: 'Manicure', descricao: '', duracaoMin: 40, preco: 35, ativo: true },
]

const profissionaisMock = [
  {
    id: 10,
    nome: 'Ana Souza',
    ativo: true,
    servicoIds: [1],
    jornada: [{ diaSemana: 'MONDAY' as const, horaInicio: '09:00:00', horaFim: '18:00:00' }],
  },
]

describe('AdminProfissionaisPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(servicosService.listar).mockResolvedValue(servicosMock)
    vi.mocked(profissionaisService.listar).mockResolvedValue(profissionaisMock)
  })

  it('shows a loading state before the data arrives', () => {
    vi.mocked(profissionaisService.listar).mockReturnValue(new Promise(() => {}))

    render(<AdminProfissionaisPage />)

    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('renders the professional list once loaded', async () => {
    render(<AdminProfissionaisPage />)

    await waitFor(() => {
      expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    })
  })

  it('shows a validation error and never calls criar when nome is empty', async () => {
    render(<AdminProfissionaisPage />)

    await waitFor(() => {
      expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /^criar$/i }))

    await waitFor(() => {
      expect(screen.getByText(/informe o nome/i)).toBeInTheDocument()
    })
    expect(profissionaisAdminService.criar).not.toHaveBeenCalled()
  })

  it('adding and removing jornada rows updates what gets submitted', async () => {
    vi.mocked(profissionaisAdminService.criar).mockResolvedValue(profissionaisMock[0])

    render(<AdminProfissionaisPage />)

    await waitFor(() => {
      expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/^nome/i), { target: { value: 'Carla Mendes' } })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Corte de Cabelo' }))

    fireEvent.click(screen.getByRole('button', { name: /adicionar horário/i }))
    fireEvent.click(screen.getByRole('button', { name: /adicionar horário/i }))

    fireEvent.change(screen.getByLabelText(/dia da semana \(horário 1\)/i), { target: { value: 'TUESDAY' } })
    fireEvent.change(screen.getByLabelText(/início \(horário 1\)/i), { target: { value: '10:00' } })
    fireEvent.change(screen.getByLabelText(/fim \(horário 1\)/i), { target: { value: '14:00' } })

    fireEvent.click(screen.getByRole('button', { name: /remover horário 2/i }))

    fireEvent.click(screen.getByRole('button', { name: /^criar$/i }))

    await waitFor(() => {
      expect(profissionaisAdminService.criar).toHaveBeenCalledWith({
        nome: 'Carla Mendes',
        servicoIds: [1],
        jornada: [{ diaSemana: 'TUESDAY', horaInicio: '10:00', horaFim: '14:00' }],
      })
    })
  })

  it('pre-fills nome, servicoIds, and jornada when editing, and submits an update with that id', async () => {
    vi.mocked(profissionaisAdminService.atualizar).mockResolvedValue(profissionaisMock[0])

    render(<AdminProfissionaisPage />)

    await waitFor(() => {
      expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(screen.getByLabelText(/^nome/i)).toHaveValue('Ana Souza')
    expect(screen.getByRole('checkbox', { name: 'Corte de Cabelo' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Manicure' })).not.toBeChecked()
    expect(screen.getByLabelText(/dia da semana \(horário 1\)/i)).toHaveValue('MONDAY')
    expect(screen.getByLabelText(/início \(horário 1\)/i)).toHaveValue('09:00')
    expect(screen.getByLabelText(/fim \(horário 1\)/i)).toHaveValue('18:00')

    fireEvent.click(screen.getByRole('button', { name: /^salvar$/i }))

    await waitFor(() => {
      expect(profissionaisAdminService.atualizar).toHaveBeenCalledWith(10, {
        nome: 'Ana Souza',
        servicoIds: [1],
        jornada: [{ diaSemana: 'MONDAY', horaInicio: '09:00', horaFim: '18:00' }],
      })
    })
  })

  it('deactivates a professional and removes it from the visible list', async () => {
    vi.mocked(profissionaisAdminService.desativar).mockResolvedValue(undefined)

    render(<AdminProfissionaisPage />)

    await waitFor(() => {
      expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /desativar/i }))

    await waitFor(() => {
      expect(profissionaisAdminService.desativar).toHaveBeenCalledWith(10)
    })
    await waitFor(() => {
      expect(screen.queryByText('Ana Souza')).not.toBeInTheDocument()
    })
  })
})
