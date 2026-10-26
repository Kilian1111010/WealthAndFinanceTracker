import { useState, type FormEvent } from 'react'
import { providersApi } from '../../api/finance'
import type { Provider } from '../../api/types'
import { Modal } from '../../components/Modal'
import { EmptyState, ErrorText, FormActions, PageHeader } from '../../components/ui'
import { amountInput, blankToNull, errorText, formatMoney, parseAmount, safeHref } from '../../lib/format'
import { useLoader } from '../../lib/useLoader'

type Editing = { provider: Provider | null }

export function ProvidersSettings() {
  const providers = useLoader(() => providersApi.list(), [])
  const [editing, setEditing] = useState<Editing | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete(provider: Provider) {
    if (!window.confirm(`Anbieter „${provider.name}“ löschen? Zugeordnete Konten bleiben erhalten.`)) {
      return
    }
    setError(null)
    try {
      await providersApi.remove(provider.id)
      providers.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  const exemptionTotal = (providers.data ?? []).reduce((sum, provider) => sum + (provider.exemptionAmount ?? 0), 0)

  return (
    <>
      <PageHeader
        title="Anbieter"
        subtitle="Banken, Broker und Börsen, bei denen deine Konten liegen."
        actions={
          <button type="button" onClick={() => setEditing({ provider: null })}>
            Neuer Anbieter
          </button>
        }
      />
      <ErrorText message={error ?? providers.error} />
      {providers.data && providers.data.length === 0 && <EmptyState>Noch keine Anbieter angelegt.</EmptyState>}
      {providers.data && providers.data.length > 0 && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Land</th>
                <th className="number">Freistellungsauftrag</th>
                <th aria-label="Aktionen" />
              </tr>
            </thead>
            <tbody>
              {providers.data.map((provider) => (
                <tr key={provider.id}>
                  <td>
                    <span className="item-title">{provider.name}</span>
                    {safeHref(provider.website) && (
                      <a className="sub-link" href={safeHref(provider.website) ?? undefined} target="_blank" rel="noreferrer">
                        {provider.website}
                      </a>
                    )}
                  </td>
                  <td>{provider.country ?? '–'}</td>
                  <td className="number">
                    {provider.exemptionAmount === null ? '–' : formatMoney(provider.exemptionAmount, 'EUR')}
                  </td>
                  <td className="row-actions">
                    <button type="button" className="link" onClick={() => setEditing({ provider })}>
                      Bearbeiten
                    </button>
                    <button type="button" className="link danger" onClick={() => handleDelete(provider)}>
                      Löschen
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {exemptionTotal > 0 && (
            <p className="muted note">Freistellungsaufträge gesamt: {formatMoney(exemptionTotal, 'EUR')}</p>
          )}
        </>
      )}
      {editing && (
        <ProviderForm
          provider={editing.provider}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            providers.reload()
          }}
        />
      )}
    </>
  )
}

function ProviderForm({ provider, onClose, onSaved }: { provider: Provider | null; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(provider?.name ?? '')
  const [country, setCountry] = useState(provider?.country ?? '')
  const [exemption, setExemption] = useState(amountInput(provider?.exemptionAmount ?? null))
  const [website, setWebsite] = useState(provider?.website ?? '')
  const [logoUrl, setLogoUrl] = useState(provider?.logoUrl ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const exemptionAmount = parseAmount(exemption)
    if (!name.trim()) {
      setError('Bitte einen Namen eingeben.')
      return
    }
    if (country.trim() && !/^[A-Za-z]{2}$/.test(country.trim())) {
      setError('Das Land muss ein zweistelliger Code sein, z. B. DE.')
      return
    }
    if (exemption.trim() && (exemptionAmount === null || exemptionAmount < 0)) {
      setError('Der Freistellungsauftrag muss eine Zahl ab 0 sein.')
      return
    }
    setError(null)
    setSubmitting(true)
    const input = {
      name: name.trim(),
      country: blankToNull(country)?.toUpperCase() ?? null,
      exemptionAmount,
      website: blankToNull(website),
      logoUrl: blankToNull(logoUrl),
    }
    try {
      if (provider) {
        await providersApi.update(provider.id, input)
      } else {
        await providersApi.create(input)
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={provider ? 'Anbieter bearbeiten' : 'Neuer Anbieter'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input value={name} maxLength={255} onChange={(event) => setName(event.target.value)} autoFocus />
        </label>
        <div className="form-row">
          <label>
            Land
            <input value={country} maxLength={2} placeholder="DE" onChange={(event) => setCountry(event.target.value)} />
          </label>
          <label>
            Freistellungsauftrag (€)
            <input value={exemption} inputMode="decimal" placeholder="0,00" onChange={(event) => setExemption(event.target.value)} />
          </label>
        </div>
        <label>
          Website
          <input value={website} maxLength={255} placeholder="https://" onChange={(event) => setWebsite(event.target.value)} />
        </label>
        <label>
          Logo-URL
          <input value={logoUrl} maxLength={512} placeholder="https://" onChange={(event) => setLogoUrl(event.target.value)} />
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}
