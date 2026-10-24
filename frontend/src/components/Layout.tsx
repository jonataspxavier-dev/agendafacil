import type { ReactNode } from 'react'
import { Nav } from './Nav'

export function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50">
      <Nav />
      <main>{children}</main>
    </div>
  )
}
