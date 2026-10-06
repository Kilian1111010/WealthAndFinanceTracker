import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from './AuthLayout'

function loginError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Benutzername oder Passwort ist falsch.'
    if (error.status === 0) return error.message
  }
  return 'Anmeldung fehlgeschlagen. Bitte später erneut versuchen.'
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login({ username: username.trim(), password })
      navigate(from, { replace: true })
    } catch (err) {
      setError(loginError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Anmelden"
      subtitle="Melde dich mit deinem Benutzernamen an."
      footer={
        <>
          Noch kein Konto? <Link to="/register">Registrieren</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Benutzername
          <input
            name="username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            autoFocus
          />
        </label>
        <label>
          Passwort
          <input
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting || !username.trim() || !password}>
          {submitting ? 'Anmelden …' : 'Anmelden'}
        </button>
      </form>
    </AuthLayout>
  )
}
