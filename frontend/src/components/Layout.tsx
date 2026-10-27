import { useEffect, useState, type ReactNode } from 'react'
import { setOnSlowRequestChange } from '@/services/api'
import { Nav } from './Nav'

export function Layout({ children }: { children: ReactNode }) {
  const [isWakingUp, setIsWakingUp] = useState(false)

  useEffect(() => {
    setOnSlowRequestChange(setIsWakingUp)
  }, [])

  return (
    <div className="min-h-screen bg-slate-50">
      <Nav />
      {isWakingUp && (
        <div className="border-b border-amber-200 bg-amber-50 px-6 py-2 text-center text-sm text-amber-800">
          Carregando o servidor, pode levar até 1 minuto…
        </div>
      )}
      <main>{children}</main>
    </div>
  )
}
