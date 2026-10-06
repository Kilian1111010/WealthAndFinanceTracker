import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { user, logout } = useAuth()
  const [error, setError] = useState<string | null>(null)

  async function handleLogout() {
    setError(null)
    try {
      await logout()
    } catch {
      setError('Abmelden fehlgeschlagen. Bitte erneut versuchen.')
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Wealth &amp; Finance Tracker</span>
        <div className="topbar-user">
          <span className="muted">{user?.name}</span>
          <button type="button" className="secondary" onClick={handleLogout}>
            Abmelden
          </button>
        </div>
      </header>
      <main className="content">
        <h1>Willkommen, {user?.name}</h1>
        <p className="muted">Hier entsteht dein Überblick über Konten, Buchungen und Vermögen.</p>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
      </main>
    </div>
  )
}
