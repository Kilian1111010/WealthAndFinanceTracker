import { useState, type FormEvent } from 'react'
import { accountTypesApi } from '../../api/finance'
import type { AccountType, AssetClass } from '../../api/types'
import { Modal } from '../../components/Modal'
import { EmptyState, ErrorText, FormActions, PageHeader } from '../../components/ui'
import { assetClassLabels, assetClassOrder, errorText } from '../../lib/format'
import { useLoader } from '../../lib/useLoader'

const assetClassHints: Record<AssetClass, string> = {
  CASH: 'Konten mit Kontostand, auf die du buchst (Girokonto, Bargeld).',
  SAVINGS: 'Sparkonten mit Kontostand, auch als virtuelle Spartöpfe.',
  INVESTMENT: 'Depots mit Beständen (Aktien, ETFs), ohne eigenen Kontostand.',
  CRYPTO: 'Wallets mit Beständen (BTC, ETH …), ohne eigenen Kontostand.',
  LIABILITY: 'Kredite und Kreditkarten, Kontostand negativ.',
  OTHER: 'Sachwerte wie Auto oder Sammlungen mit geschätztem Wert.',
}

type Editing = { accountType: AccountType | null }

export function AccountTypesSettings() {
  const accountTypes = useLoader(() => accountTypesApi.list(), [])
  const [editing, setEditing] = useState<Editing | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete(accountType: AccountType) {
    if (!window.confirm(`Kontotyp „${accountType.name}“ löschen?`)) {
      return
    }
    setError(null)
    try {
      await accountTypesApi.remove(accountType.id)
      accountTypes.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <>
      <PageHeader
        title="Kontotypen"
        subtitle="Die Anlageklasse bestimmt, ob ein Konto einen Kontostand oder Bestände hat."
        actions={
          <button type="button" onClick={() => setEditing({ accountType: null })}>
            Neuer Kontotyp
          </button>
        }
      />
      <ErrorText message={error ?? accountTypes.error} />
      {accountTypes.data && accountTypes.data.length === 0 && (
        <EmptyState>Noch keine Kontotypen. Lege z. B. „Girokonto“ (Bargeld & Giro) oder „Depot“ (Wertpapiere) an.</EmptyState>
      )}
      {accountTypes.data && accountTypes.data.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Anlageklasse</th>
              <th>Im Vermögen</th>
              <th aria-label="Aktionen" />
            </tr>
          </thead>
          <tbody>
            {accountTypes.data.map((accountType) => (
              <tr key={accountType.id}>
                <td className="item-title">{accountType.name}</td>
                <td>{assetClassLabels[accountType.assetClass]}</td>
                <td>{accountType.includeInNetWorth ? 'Ja' : 'Nein'}</td>
                <td className="row-actions">
                  <button type="button" className="link" onClick={() => setEditing({ accountType })}>
                    Bearbeiten
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(accountType)}>
                    Löschen
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {editing && (
        <AccountTypeForm
          accountType={editing.accountType}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            accountTypes.reload()
          }}
        />
      )}
    </>
  )
}

function AccountTypeForm({
  accountType,
  onClose,
  onSaved,
}: {
  accountType: AccountType | null
  onClose: () => void
  onSaved: () => void
}) {
  const [name, setName] = useState(accountType?.name ?? '')
  const [assetClass, setAssetClass] = useState<AssetClass>(accountType?.assetClass ?? 'CASH')
  const [includeInNetWorth, setIncludeInNetWorth] = useState(accountType?.includeInNetWorth ?? true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Bitte einen Namen eingeben.')
      return
    }
    setError(null)
    setSubmitting(true)
    const input = { name: name.trim(), assetClass, includeInNetWorth }
    try {
      if (accountType) {
        await accountTypesApi.update(accountType.id, input)
      } else {
        await accountTypesApi.create(input)
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={accountType ? 'Kontotyp bearbeiten' : 'Neuer Kontotyp'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input value={name} maxLength={255} placeholder="z. B. Girokonto" onChange={(event) => setName(event.target.value)} autoFocus />
        </label>
        <label>
          Anlageklasse
          <select value={assetClass} onChange={(event) => setAssetClass(event.target.value as AssetClass)}>
            {assetClassOrder.map((value) => (
              <option key={value} value={value}>
                {assetClassLabels[value]}
              </option>
            ))}
          </select>
          <span className="hint">{assetClassHints[assetClass]}</span>
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={includeInNetWorth} onChange={(event) => setIncludeInNetWorth(event.target.checked)} />
          Zum Vermögen zählen
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}
