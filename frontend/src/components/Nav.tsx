import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

export function Nav() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  async function sair() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-2xl flex-wrap items-center justify-between gap-2 px-6 py-4">
        <Link to="/servicos" className="text-lg font-semibold text-slate-900">
          AgendaFácil
        </Link>
        <nav className="flex flex-wrap items-center gap-4 text-sm">
          {isAuthenticated && user?.perfil === 'ADMIN' ? (
            <>
              <Link to="/admin/dashboard" className="text-slate-700 hover:text-slate-900">
                Dashboard
              </Link>
              <Link to="/admin/servicos" className="text-slate-700 hover:text-slate-900">
                Serviços
              </Link>
              <Link to="/admin/profissionais" className="text-slate-700 hover:text-slate-900">
                Profissionais
              </Link>
              <Link to="/admin/agendamentos" className="text-slate-700 hover:text-slate-900">
                Agendamentos
              </Link>
              <button type="button" onClick={sair} className="text-slate-700 hover:text-slate-900">
                Sair
              </button>
            </>
          ) : isAuthenticated ? (
            <>
              <Link to="/meus-agendamentos" className="text-slate-700 hover:text-slate-900">
                Meus agendamentos
              </Link>
              <button type="button" onClick={sair} className="text-slate-700 hover:text-slate-900">
                Sair
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-slate-700 hover:text-slate-900">
                Entrar
              </Link>
              <Link to="/cadastro" className="text-slate-700 hover:text-slate-900">
                Cadastrar
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
