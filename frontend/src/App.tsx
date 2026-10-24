import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/contexts/AuthContext'
import { AgendarPage } from '@/pages/AgendarPage'
import { LoginPage } from '@/pages/LoginPage'
import { MeusAgendamentosPage } from '@/pages/MeusAgendamentosPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ServicosPage } from '@/pages/ServicosPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
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
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
