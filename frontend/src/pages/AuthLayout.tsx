import type { ReactNode } from 'react'

type Props = {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

export function AuthLayout({ title, subtitle, children, footer }: Props) {
  return (
    <main className="auth">
      <div className="auth-card">
        <div className="brand">Wealth &amp; Finance Tracker</div>
        <h1>{title}</h1>
        <p className="muted">{subtitle}</p>
        {children}
        <p className="auth-footer">{footer}</p>
      </div>
    </main>
  )
}
