import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('@/services/servicosService', () => ({
  listar: () => new Promise(() => {}),
}))

describe('App', () => {
  it('redirects / to /servicos and renders the service list page', () => {
    render(<App />)
    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })
})
