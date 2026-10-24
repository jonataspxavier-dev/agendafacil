export interface BarChartItem {
  label: string
  value: number
}

export function BarChart({ data }: { data: BarChartItem[] }) {
  if (data.length === 0) {
    return <p className="text-sm text-slate-600">Sem dados para exibir.</p>
  }

  const max = Math.max(...data.map((item) => item.value))

  return (
    <ul className="space-y-3">
      {data.map((item) => (
        <li key={item.label}>
          <div className="flex items-center justify-between text-sm text-slate-700">
            <span>{item.label}</span>
            <span>{item.value}</span>
          </div>
          <div
            role="progressbar"
            aria-label={item.label}
            aria-valuenow={item.value}
            aria-valuemin={0}
            aria-valuemax={max}
            className="mt-1 h-2 w-full overflow-hidden rounded bg-slate-100"
          >
            <div
              className="h-2 rounded bg-slate-900"
              style={{ width: `${max > 0 ? (item.value / max) * 100 : 0}%` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}
