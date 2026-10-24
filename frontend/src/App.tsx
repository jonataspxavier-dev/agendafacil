import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/contexts/AuthContext'
import { ServicosPage } from '@/pages/ServicosPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/servicos" replace />} />
          <Route path="/servicos" element={<ServicosPage />} />
          <Route path="/login" element={<div>Login</div>} />
          <Route path="/cadastro" element={<div>Cadastro</div>} />
          <Route
            path="/agendar"
            element={
              <ProtectedRoute>
                <div>Agendar</div>
              </ProtectedRoute>
            }
          />
          <Route
            path="/meus-agendamentos"
            element={
              <ProtectedRoute>
                <div>Meus agendamentos</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
