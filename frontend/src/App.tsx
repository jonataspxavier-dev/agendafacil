import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AdminRoute } from '@/components/AdminRoute'
import { Layout } from '@/components/Layout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/contexts/AuthContext'
import { AgendarPage } from '@/pages/AgendarPage'
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage'
import { AdminAgendamentosPage } from '@/pages/admin/AdminAgendamentosPage'
import { AdminProfissionaisPage } from '@/pages/admin/AdminProfissionaisPage'
import { AdminServicosPage } from '@/pages/admin/AdminServicosPage'
import { LoginPage } from '@/pages/LoginPage'
import { MeusAgendamentosPage } from '@/pages/MeusAgendamentosPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ServicosPage } from '@/pages/ServicosPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<Navigate to="/servicos" replace />} />
            <Route path="/servicos" element={<ServicosPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/cadastro" element={<RegisterPage />} />
            <Route
              path="/agendar"
              element={
                <ProtectedRoute>
                  <AgendarPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/meus-agendamentos"
              element={
                <ProtectedRoute>
                  <MeusAgendamentosPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/dashboard"
              element={
                <AdminRoute>
                  <AdminDashboardPage />
                </AdminRoute>
              }
            />
            <Route
              path="/admin/servicos"
              element={
                <AdminRoute>
                  <AdminServicosPage />
                </AdminRoute>
              }
            />
            <Route
              path="/admin/profissionais"
              element={
                <AdminRoute>
                  <AdminProfissionaisPage />
                </AdminRoute>
              }
            />
            <Route
              path="/admin/agendamentos"
              element={
                <AdminRoute>
                  <AdminAgendamentosPage />
                </AdminRoute>
              }
            />
          </Routes>
        </Layout>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
