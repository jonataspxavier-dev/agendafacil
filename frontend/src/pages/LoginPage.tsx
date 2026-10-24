import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '@/hooks/useAuth'
import { extractErrorMessage } from '@/services/api'

const loginSchema = z.object({
  email: z.email('E-mail inválido'),
  senha: z.string().min(1, 'Informe a senha'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [erro, setErro] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  async function onSubmit(values: LoginFormValues) {
    setErro(null)
    try {
      const usuario = await login(values.email, values.senha)
      const state = location.state as { from?: { pathname?: string } } | null
      const destinoPadrao = usuario.perfil === 'ADMIN' ? '/admin/dashboard' : '/servicos'
      navigate(state?.from?.pathname ?? destinoPadrao, { replace: true })
    } catch (error) {
      setErro(extractErrorMessage(error))
    }
  }

  return (
    <div className="mx-auto max-w-sm p-6">
      <h1 className="text-2xl font-semibold text-slate-900">Entrar</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="mt-4 space-y-4" noValidate>
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-slate-700">
            E-mail
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            {...register('email')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          {errors.email && <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>}
        </div>

        <div>
          <label htmlFor="senha" className="block text-sm font-medium text-slate-700">
            Senha
          </label>
          <input
            id="senha"
            type="password"
            autoComplete="current-password"
            {...register('senha')}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          {errors.senha && <p className="mt-1 text-sm text-red-600">{errors.senha.message}</p>}
        </div>

        {erro && <p className="text-sm text-red-600">{erro}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {isSubmitting ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Não tem conta?{' '}
        <Link to="/cadastro" className="font-medium text-slate-900 underline">
          Cadastre-se
        </Link>
      </p>
    </div>
  )
}
