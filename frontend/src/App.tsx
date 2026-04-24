import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuth } from './auth'
import { Info } from './components/Info'
import { Browser } from './components/Browser'
import { Preview } from './components/Preview'
import './App.css'

function App() {
  const auth = useAuth()

  if (!auth.token || !auth.info) {
    return <Info />
  }

  return (
    <BrowserRouter>
      <header className="app-header">
        <div className="identity">
          <strong>{auth.info.tenant}</strong>
          <span className="sep">/</span>
          {auth.info.realm}
          <span className="sep">·</span>
          <span className="subject">{auth.info.subject}</span>
        </div>
        <button onClick={auth.logout} className="logout">
          <LogOut size={16} /> Log out
        </button>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/browse" replace />} />
          <Route path="/browse" element={<Browser />} />
          <Route path="/browse/*" element={<Browser />} />
          <Route path="/view/*" element={<Preview />} />
          <Route path="*" element={<Navigate to="/browse" replace />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

export default App
