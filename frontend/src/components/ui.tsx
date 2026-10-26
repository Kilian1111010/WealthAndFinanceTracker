import type { ReactNode } from 'react'

export function ErrorText({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p className="error" role="alert">
      {message}
    </p>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}

export function FormActions({ submitting, label, onCancel }: { submitting: boolean; label: string; onCancel: () => void }) {
  return (
    <div className="form-actions">
      <button type="button" className="secondary" onClick={onCancel}>
        Abbrechen
      </button>
      <button type="submit" disabled={submitting}>
        {submitting ? 'Speichern …' : label}
      </button>
    </div>
  )
}
