import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/contexts/AuthContext'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<div>AgendaFácil</div>} />
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
