import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const navigation = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/bookings', label: 'Buchungen', end: false },
  { to: '/settings', label: 'Einstellungen', end: false },
]

export function AppLayout() {
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
        <nav className="main-nav" aria-label="Hauptnavigation">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="topbar-user">
          <span className="muted">{user?.name}</span>
          <button type="button" className="secondary" onClick={handleLogout}>
            Abmelden
          </button>
        </div>
      </header>
      {error && (
        <p className="error banner" role="alert">
          {error}
        </p>
      )}
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
