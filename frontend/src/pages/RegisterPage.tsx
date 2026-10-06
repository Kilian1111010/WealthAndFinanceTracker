import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from './AuthLayout'

const PASSWORD_MIN = 8
const PASSWORD_MAX = 72
const USERNAME_MAX = 255

function validate(username: string, password: string, confirmation: string): string | null {
  if (!username) return 'Bitte einen Benutzernamen eingeben.'
  if (username.length > USERNAME_MAX) return `Der Benutzername darf höchstens ${USERNAME_MAX} Zeichen haben.`
  if (password.length < PASSWORD_MIN) return `Das Passwort muss mindestens ${PASSWORD_MIN} Zeichen haben.`
  if (password.length > PASSWORD_MAX) return `Das Passwort darf höchstens ${PASSWORD_MAX} Zeichen haben.`
  if (password !== confirmation) return 'Die Passwörter stimmen nicht überein.'
  return null
}

function registerError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 409) return 'Dieser Benutzername ist bereits vergeben.'
    if (error.status === 400) return error.message
    if (error.status === 0) return error.message
  }
  return 'Registrierung fehlgeschlagen. Bitte später erneut versuchen.'
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = username.trim()
    const validationError = validate(trimmed, password, confirmation)
    if (validationError) {
      setError(validationError)
      return
    }

    setError(null)
    setSubmitting(true)
    try {
      await register({ username: trimmed, password })
      navigate('/', { replace: true })
    } catch (err) {
      setError(registerError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Registrieren"
      subtitle="Lege ein neues Konto an."
      footer={
        <>
          Schon registriert? <Link to="/login">Anmelden</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Benutzername
          <input
            name="username"
            autoComplete="username"
            maxLength={USERNAME_MAX}
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
            autoComplete="new-password"
            maxLength={PASSWORD_MAX}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <span className="hint">
            {PASSWORD_MIN}–{PASSWORD_MAX} Zeichen
          </span>
        </label>
        <label>
          Passwort wiederholen
          <input
            name="confirmation"
            type="password"
            autoComplete="new-password"
            maxLength={PASSWORD_MAX}
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            required
          />
        </label>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Registrieren …' : 'Registrieren'}
        </button>
      </form>
    </AuthLayout>
  )
}
