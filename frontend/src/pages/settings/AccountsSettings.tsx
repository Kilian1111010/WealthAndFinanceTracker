import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { accountTypesApi, accountsApi, holdingsApi, providersApi } from '../../api/finance'
import { HoldingsDialog } from '../../components/HoldingsDialog'
import type { Account, AccountType, Provider } from '../../api/types'
import { Modal } from '../../components/Modal'
import { EmptyState, ErrorText, FormActions, PageHeader } from '../../components/ui'
import {
  allowsSubAccounts,
  amountInput,
  assetClassLabels,
  assetClassOrder,
  blankToNull,
  errorText,
  formatMoney,
  hasBalance,
  holdsPositions,
  parseAmount,
} from '../../lib/format'
import { useLoader } from '../../lib/useLoader'

type Dialog =
  | { kind: 'create' }
  | { kind: 'edit'; account: Account }
  | { kind: 'holdings'; account: Account }

function orderedAccounts(accounts: Account[]): Account[] {
  const ids = new Set(accounts.map((account) => account.id))
  const isChild = (account: Account) => account.parentAccountId !== null && ids.has(account.parentAccountId)
  const children = new Map<string, Account[]>()
  accounts.filter(isChild).forEach((account) => {
    const list = children.get(account.parentAccountId!) ?? []
    list.push(account)
    children.set(account.parentAccountId!, list)
  })
  return accounts
    .filter((account) => !isChild(account))
    .flatMap((account) => [account, ...(children.get(account.id) ?? [])])
}

export function AccountsSettings() {
  const accounts = useLoader(() => accountsApi.list(), [])
  const accountTypes = useLoader(() => accountTypesApi.list(), [])
  const providers = useLoader(() => providersApi.list(), [])
  const holdings = useLoader(() => holdingsApi.listAll(), [])
  const [dialog, setDialog] = useState<Dialog | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const holdingValues = useMemo(() => {
    const values = new Map<string, number>()
    ;(holdings.data ?? []).forEach((holding) => {
      values.set(holding.accountId, (values.get(holding.accountId) ?? 0) + (holding.valueEur ?? 0))
    })
    return values
  }, [holdings.data])

  const groups = useMemo(() => {
    const all = accounts.data ?? []
    const visible = all.filter((account) => showArchived || !account.archived)
    const groupOf = (account: Account) =>
      all.find((candidate) => candidate.id === account.parentAccountId)?.assetClass ?? account.assetClass
    return assetClassOrder
      .map((assetClass) => ({
        assetClass,
        accounts: orderedAccounts(visible.filter((account) => groupOf(account) === assetClass)),
      }))
      .filter((group) => group.accounts.length > 0)
  }, [accounts.data, showArchived])

  async function toggleArchived(account: Account) {
    setError(null)
    try {
      await accountsApi.update(account.id, {
        name: account.name,
        providerId: account.providerId,
        balance: null,
        description: account.description,
        archived: !account.archived,
      })
      accounts.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  async function handleDelete(account: Account) {
    if (!window.confirm(`Konto „${account.name}“ endgültig löschen?`)) {
      return
    }
    setError(null)
    try {
      await accountsApi.remove(account.id)
      accounts.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  const archivedCount = (accounts.data ?? []).filter((account) => account.archived).length
  const noTypes = accountTypes.data !== null && accountTypes.data.length === 0

  return (
    <>
      <PageHeader
        title="Konten"
        subtitle="Echte und virtuelle Konten, Depots, Wallets und Sachwerte."
        actions={
          <button type="button" disabled={noTypes} onClick={() => setDialog({ kind: 'create' })}>
            Neues Konto
          </button>
        }
      />
      <ErrorText message={error ?? accounts.error ?? accountTypes.error ?? providers.error} />
      {noTypes && (
        <EmptyState>
          Lege zuerst unter <Link to="/settings/account-types">Kontotypen</Link> mindestens einen Typ an, z. B. „Girokonto“.
        </EmptyState>
      )}
      {!noTypes && accounts.data && accounts.data.length === 0 && <EmptyState>Noch keine Konten angelegt.</EmptyState>}
      {archivedCount > 0 && (
        <label className="checkbox inline-toggle">
          <input type="checkbox" checked={showArchived} onChange={(event) => setShowArchived(event.target.checked)} />
          Archivierte Konten anzeigen ({archivedCount})
        </label>
      )}
      {groups.map((group) => (
        <section key={group.assetClass} className="account-group">
          <h2>{assetClassLabels[group.assetClass]}</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Konto</th>
                <th>Typ</th>
                <th>Anbieter</th>
                <th className="number">{hasBalance(group.assetClass) ? 'Kontostand' : 'Wert'}</th>
                <th aria-label="Aktionen" />
              </tr>
            </thead>
            <tbody>
              {group.accounts.map((account) => (
                <tr key={account.id} className={account.archived ? 'archived' : undefined}>
                  <td className={account.virtual ? 'indented' : undefined}>
                    <span className="item-title">{account.name}</span>
                    {account.virtual && <span className="badge">virtuell</span>}
                    {account.archived && <span className="badge">archiviert</span>}
                    {account.description && <span className="sub-text">{account.description}</span>}
                  </td>
                  <td>{account.accountTypeName}</td>
                  <td>{account.providerName ?? '–'}</td>
                  <td className="number">
                    {account.balance !== null ? (
                      formatMoney(account.balance, account.currency)
                    ) : (
                      <>
                        {holdingValues.has(account.id) && formatMoney(holdingValues.get(account.id)!, 'EUR')}
                        <button type="button" className="link sub-link" onClick={() => setDialog({ kind: 'holdings', account })}>
                          Bestände verwalten
                        </button>
                      </>
                    )}
                  </td>
                  <td className="row-actions">
                    <button type="button" className="link" onClick={() => setDialog({ kind: 'edit', account })}>
                      Bearbeiten
                    </button>
                    <button type="button" className="link" onClick={() => toggleArchived(account)}>
                      {account.archived ? 'Wiederherstellen' : 'Archivieren'}
                    </button>
                    <button type="button" className="link danger" onClick={() => handleDelete(account)}>
                      Löschen
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}
      {dialog?.kind === 'create' && accountTypes.data && (
        <AccountCreateForm
          accountTypes={accountTypes.data}
          providers={providers.data ?? []}
          accounts={accounts.data ?? []}
          onClose={() => setDialog(null)}
          onSaved={() => {
            setDialog(null)
            accounts.reload()
          }}
        />
      )}
      {dialog?.kind === 'edit' && (
        <AccountEditForm
          account={dialog.account}
          providers={providers.data ?? []}
          onClose={() => setDialog(null)}
          onSaved={() => {
            setDialog(null)
            accounts.reload()
          }}
        />
      )}
      {dialog?.kind === 'holdings' && (
        <HoldingsDialog
          key={dialog.account.id}
          account={dialog.account}
          onClose={() => {
            setDialog(null)
            holdings.reload()
          }}
        />
      )}
    </>
  )
}

function ProviderSelect({ providers, value, onChange }: { providers: Provider[]; value: string; onChange: (value: string) => void }) {
  return (
    <label>
      Anbieter
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">Kein Anbieter</option>
        {providers.map((provider) => (
          <option key={provider.id} value={provider.id}>
            {provider.name}
          </option>
        ))}
      </select>
    </label>
  )
}

function AccountCreateForm({
  accountTypes,
  providers,
  accounts,
  onClose,
  onSaved,
}: {
  accountTypes: AccountType[]
  providers: Provider[]
  accounts: Account[]
  onClose: () => void
  onSaved: () => void
}) {
  const [name, setName] = useState('')
  const [accountTypeId, setAccountTypeId] = useState(accountTypes[0]?.id ?? '')
  const [providerId, setProviderId] = useState('')
  const [parentAccountId, setParentAccountId] = useState('')
  const [currency, setCurrency] = useState('EUR')
  const [balance, setBalance] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const assetClass = accountTypes.find((type) => type.id === accountTypeId)?.assetClass ?? 'CASH'
  const parentCandidates = accounts.filter(
    (account) => !account.virtual && !account.archived && allowsSubAccounts(account.assetClass),
  )
  const canHaveParent = allowsSubAccounts(assetClass) && parentCandidates.length > 0
  const parent = canHaveParent ? parentCandidates.find((account) => account.id === parentAccountId) : undefined

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const balanceValue = parseAmount(balance)
    if (!name.trim()) {
      setError('Bitte einen Namen eingeben.')
      return
    }
    if (hasBalance(assetClass) && !parent && !/^[A-Za-z]{3}$/.test(currency.trim())) {
      setError('Bitte einen dreistelligen Währungscode angeben, z. B. EUR.')
      return
    }
    if (balance.trim() && balanceValue === null) {
      setError('Der Kontostand muss eine Zahl sein.')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await accountsApi.create({
        name: name.trim(),
        accountTypeId,
        providerId: providerId || null,
        parentAccountId: parent ? parent.id : null,
        currency: hasBalance(assetClass) && !parent ? currency.trim().toUpperCase() : null,
        balance: hasBalance(assetClass) ? balanceValue : null,
        description: blankToNull(description),
      })
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Neues Konto" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input value={name} maxLength={255} placeholder="z. B. Girokonto Sparkasse" onChange={(event) => setName(event.target.value)} autoFocus />
        </label>
        <div className="form-row">
          <label>
            Kontotyp
            <select value={accountTypeId} onChange={(event) => setAccountTypeId(event.target.value)}>
              {accountTypes.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name} ({assetClassLabels[type.assetClass]})
                </option>
              ))}
            </select>
          </label>
          <ProviderSelect providers={providers} value={providerId} onChange={setProviderId} />
        </div>
        {canHaveParent && (
          <label>
            Virtuelles Unterkonto von
            <select value={parentAccountId} onChange={(event) => setParentAccountId(event.target.value)}>
              <option value="">Kein Elternkonto (echtes Konto)</option>
              {parentCandidates.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
            <span className="hint">Ein virtuelles Konto ist ein Spartopf mit eigenem Guthaben, z. B. für Urlaub.</span>
          </label>
        )}
        {hasBalance(assetClass) && (
          <div className="form-row">
            <label>
              Währung
              <input
                value={parent ? parent.currency ?? '' : currency}
                maxLength={3}
                disabled={Boolean(parent)}
                onChange={(event) => setCurrency(event.target.value)}
              />
            </label>
            <label>
              {assetClass === 'OTHER' ? 'Wert' : 'Anfangsstand'}
              <input value={balance} inputMode="decimal" placeholder="0,00" onChange={(event) => setBalance(event.target.value)} />
              {assetClass === 'LIABILITY' && <span className="hint">Schulden als negativen Betrag eingeben.</span>}
            </label>
          </div>
        )}
        {holdsPositions(assetClass) && (
          <p className="hint">Depots und Wallets haben keinen Kontostand. Bestände legst du nach dem Speichern an.</p>
        )}
        <label>
          Beschreibung
          <textarea value={description} maxLength={1000} rows={2} onChange={(event) => setDescription(event.target.value)} />
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Anlegen" onCancel={onClose} />
      </form>
    </Modal>
  )
}

function AccountEditForm({
  account,
  providers,
  onClose,
  onSaved,
}: {
  account: Account
  providers: Provider[]
  onClose: () => void
  onSaved: () => void
}) {
  const [name, setName] = useState(account.name)
  const [providerId, setProviderId] = useState(account.providerId ?? '')
  const [balance, setBalance] = useState(amountInput(account.balance))
  const [description, setDescription] = useState(account.description ?? '')
  const [archived, setArchived] = useState(account.archived)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const balanceValue = parseAmount(balance)
    if (!name.trim()) {
      setError('Bitte einen Namen eingeben.')
      return
    }
    if (account.balance !== null && balanceValue === null) {
      setError('Der Kontostand muss eine Zahl sein.')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await accountsApi.update(account.id, {
        name: name.trim(),
        providerId: providerId || null,
        balance: account.balance !== null && balanceValue !== account.balance ? balanceValue : null,
        description: blankToNull(description),
        archived,
      })
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Konto bearbeiten" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input value={name} maxLength={255} onChange={(event) => setName(event.target.value)} autoFocus />
        </label>
        <ProviderSelect providers={providers} value={providerId} onChange={setProviderId} />
        {account.balance !== null && (
          <label>
            {account.assetClass === 'OTHER' ? 'Wert' : 'Kontostand'} ({account.currency})
            <input value={balance} inputMode="decimal" onChange={(event) => setBalance(event.target.value)} />
            {account.assetClass !== 'OTHER' && (
              <span className="hint">Nur zum Korrigieren, normalerweise ändert sich der Stand über Buchungen.</span>
            )}
          </label>
        )}
        <label>
          Beschreibung
          <textarea value={description} maxLength={1000} rows={2} onChange={(event) => setDescription(event.target.value)} />
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={archived} onChange={(event) => setArchived(event.target.checked)} />
          Archiviert
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}
