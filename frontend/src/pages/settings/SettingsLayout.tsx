import { NavLink, Outlet } from 'react-router-dom'

const sections = [
  { to: 'accounts', label: 'Konten' },
  { to: 'account-types', label: 'Kontotypen' },
  { to: 'providers', label: 'Anbieter' },
  { to: 'categories', label: 'Kategorien' },
]

export function SettingsLayout() {
  return (
    <div className="settings">
      <aside className="settings-nav">
        <h1>Einstellungen</h1>
        <nav aria-label="Einstellungen">
          {sections.map((section) => (
            <NavLink key={section.to} to={section.to}>
              {section.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <section className="settings-content">
        <Outlet />
      </section>
    </div>
  )
}
