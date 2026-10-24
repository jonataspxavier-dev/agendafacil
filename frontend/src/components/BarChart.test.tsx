import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BarChart } from './BarChart'

describe('BarChart', () => {
  it('renders no data message when given an empty list', () => {
    render(<BarChart data={[]} />)

    expect(screen.getByText(/sem dados/i)).toBeInTheDocument()
  })

  it('sizes each bar proportionally to the maximum value', () => {
    render(
      <BarChart
        data={[
          { label: 'Corte de Cabelo', value: 6 },
          { label: 'Manicure', value: 3 },
          { label: 'Depilação', value: 0 },
        ]}
      />,
    )

    const corte = screen.getByRole('progressbar', { name: 'Corte de Cabelo' })
    const manicure = screen.getByRole('progressbar', { name: 'Manicure' })
    const depilacao = screen.getByRole('progressbar', { name: 'Depilação' })

    expect(corte).toHaveAttribute('aria-valuenow', '6')
    expect(corte.firstElementChild).toHaveStyle({ width: '100%' })
    expect(manicure.firstElementChild).toHaveStyle({ width: '50%' })
    expect(depilacao.firstElementChild).toHaveStyle({ width: '0%' })
  })
})
