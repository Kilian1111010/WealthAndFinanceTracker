import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { accountsApi, categoriesApi, transactionsApi, transfersApi } from '../api/finance'
import type { Account, Category, Transaction, TransactionKind, Transfer } from '../api/types'
import { Modal } from '../components/Modal'
import { EmptyState, ErrorText, FormActions, PageHeader } from '../components/ui'
import {
  amountInput,
  blankToNull,
  currentMonth,
  errorText,
  formatDate,
  formatMoney,
  formatSignedMoney,
  isBookable,
  monthRange,
  parseAmount,
  shiftMonth,
  formatMonth,
  toDateTimeInput,
} from '../lib/format'
import { useLoader } from '../lib/useLoader'

const PAGE_SIZE = 25

type Tab = 'transactions' | 'transfers'

type Dialog =
  | { kind: 'transaction'; transaction: Transaction | null; type: TransactionKind }
  | { kind: 'transfer'; transfer: Transfer | null }

export function BookingsPage() {
  const [tab, setTab] = useState<Tab>('transactions')
  const [month, setMonth] = useState(currentMonth())
  const [accountId, setAccountId] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [kind, setKind] = useState<'' | TransactionKind>('')
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState<Dialog | null>(null)
  const [error, setError] = useState<string | null>(null)

  const accounts = useLoader(() => accountsApi.list(), [])
  const categories = useLoader(() => categoriesApi.list(), [])
  const range = monthRange(month)
  const transactions = useLoader(
    () =>
      transactionsApi.list({
        ...range,
        accountId: accountId || undefined,
        categoryId: categoryId || undefined,
        kind: kind || undefined,
        page,
        size: PAGE_SIZE,
      }),
    [month, accountId, categoryId, kind, page],
  )
  const transfers = useLoader(
    () => transfersApi.list({ ...range, accountId: accountId || undefined, page, size: PAGE_SIZE }),
    [month, accountId, page],
  )

  const bookableAccounts = (accounts.data ?? []).filter((account) => !account.archived && isBookable(account.assetClass))
  const noAccounts = accounts.data !== null && bookableAccounts.length === 0

  function changeFilter(update: () => void) {
    update()
    setPage(0)
  }

  function reload() {
    transactions.reload()
    transfers.reload()
  }

  async function deleteTransaction(transaction: Transaction) {
    if (!window.confirm('Buchung löschen? Der Kontostand wird zurückgerechnet.')) return
    setError(null)
    try {
      await transactionsApi.remove(transaction.id)
      reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  async function deleteTransfer(transfer: Transfer) {
    if (!window.confirm('Transfer löschen? Beide Kontostände werden zurückgerechnet.')) return
    setError(null)
    try {
      await transfersApi.remove(transfer.id)
      reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  const current = tab === 'transactions' ? transactions.data : transfers.data

  return (
    <>
      <PageHeader
        title="Buchungen"
        subtitle="Einnahmen, Ausgaben und Transfers zwischen deinen Konten."
        actions={
          <>
            <button type="button" className="secondary" disabled={noAccounts} onClick={() => setDialog({ kind: 'transfer', transfer: null })}>
              Transfer
            </button>
            <button type="button" className="secondary" disabled={noAccounts} onClick={() => setDialog({ kind: 'transaction', transaction: null, type: 'INCOME' })}>
              Einnahme
            </button>
            <button type="button" disabled={noAccounts} onClick={() => setDialog({ kind: 'transaction', transaction: null, type: 'EXPENSE' })}>
              Ausgabe
            </button>
          </>
        }
      />
      {noAccounts && (
        <EmptyState>
          Um zu buchen, lege unter <Link to="/settings/accounts">Einstellungen → Konten</Link> ein Bargeld-, Spar- oder Kreditkonto an.
        </EmptyState>
      )}
      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'transactions'} onClick={() => changeFilter(() => setTab('transactions'))}>
          Einnahmen &amp; Ausgaben
        </button>
        <button type="button" role="tab" aria-selected={tab === 'transfers'} onClick={() => changeFilter(() => setTab('transfers'))}>
          Transfers
        </button>
      </div>
      <div className="filters">
        <div className="month-picker">
          <button type="button" className="icon-button" aria-label="Vorheriger Monat" onClick={() => changeFilter(() => setMonth(shiftMonth(month, -1)))}>
            ‹
          </button>
          <span className="month-label">{formatMonth(month, 'long')}</span>
          <button type="button" className="icon-button" aria-label="Nächster Monat" onClick={() => changeFilter(() => setMonth(shiftMonth(month, 1)))}>
            ›
          </button>
        </div>
        <select aria-label="Konto" value={accountId} onChange={(event) => changeFilter(() => setAccountId(event.target.value))}>
          <option value="">Alle Konten</option>
          {(accounts.data ?? []).filter((account) => isBookable(account.assetClass)).map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </select>
        {tab === 'transactions' && (
          <>
            <select aria-label="Kategorie" value={categoryId} onChange={(event) => changeFilter(() => setCategoryId(event.target.value))}>
              <option value="">Alle Kategorien</option>
              {(categories.data ?? []).map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            <select aria-label="Art" value={kind} onChange={(event) => changeFilter(() => setKind(event.target.value as '' | TransactionKind))}>
              <option value="">Einnahmen &amp; Ausgaben</option>
              <option value="INCOME">Nur Einnahmen</option>
              <option value="EXPENSE">Nur Ausgaben</option>
            </select>
          </>
        )}
      </div>
      <ErrorText message={error ?? (tab === 'transactions' ? transactions.error : transfers.error)} />

      {tab === 'transactions' && transactions.data && (
        transactions.data.content.length === 0 ? (
          <EmptyState>Keine Buchungen in diesem Zeitraum.</EmptyState>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Datum</th>
                <th>Beschreibung</th>
                <th>Kategorie</th>
                <th>Konto</th>
                <th className="number">Betrag</th>
                <th aria-label="Aktionen" />
              </tr>
            </thead>
            <tbody>
              {transactions.data.content.map((transaction) => (
                <tr key={transaction.id}>
                  <td className="nowrap">{formatDate(transaction.date)}</td>
                  <td>
                    <span className="item-title">{transaction.counterparty ?? (transaction.amount > 0 ? 'Einnahme' : 'Ausgabe')}</span>
                    {transaction.description && <span className="sub-text">{transaction.description}</span>}
                  </td>
                  <td>{transaction.categoryName ?? '–'}</td>
                  <td>{transaction.accountName}</td>
                  <td className={`number nowrap ${transaction.amount > 0 ? 'positive' : ''}`}>
                    {formatSignedMoney(transaction.amount, transaction.currency)}
                  </td>
                  <td className="row-actions">
                    <button
                      type="button"
                      className="link"
                      onClick={() => setDialog({ kind: 'transaction', transaction, type: transaction.amount > 0 ? 'INCOME' : 'EXPENSE' })}
                    >
                      Bearbeiten
                    </button>
                    <button type="button" className="link danger" onClick={() => deleteTransaction(transaction)}>
                      Löschen
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}

      {tab === 'transfers' && transfers.data && (
        transfers.data.content.length === 0 ? (
          <EmptyState>Keine Transfers in diesem Zeitraum.</EmptyState>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Datum</th>
                <th>Von</th>
                <th>Nach</th>
                <th className="number">Betrag</th>
                <th aria-label="Aktionen" />
              </tr>
            </thead>
            <tbody>
              {transfers.data.content.map((transfer) => (
                <tr key={transfer.id}>
                  <td className="nowrap">{formatDate(transfer.date)}</td>
                  <td>
                    <span className="item-title">{transfer.sendingAccountName}</span>
                    {transfer.description && <span className="sub-text">{transfer.description}</span>}
                  </td>
                  <td>{transfer.receivingAccountName}</td>
                  <td className="number nowrap">
                    {formatMoney(transfer.sentAmount, transfer.sendingCurrency)}
                    {transfer.sendingCurrency !== transfer.receivingCurrency && (
                      <span className="sub-text">→ {formatMoney(transfer.receivedAmount, transfer.receivingCurrency)}</span>
                    )}
                  </td>
                  <td className="row-actions">
                    <button type="button" className="link" onClick={() => setDialog({ kind: 'transfer', transfer })}>
                      Bearbeiten
                    </button>
                    <button type="button" className="link danger" onClick={() => deleteTransfer(transfer)}>
                      Löschen
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}

      {current && current.totalPages > 1 && (
        <div className="pagination">
          <button type="button" className="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Zurück
          </button>
          <span className="muted">
            Seite {page + 1} von {current.totalPages}
          </span>
          <button type="button" className="secondary" disabled={page + 1 >= current.totalPages} onClick={() => setPage(page + 1)}>
            Weiter
          </button>
        </div>
      )}

      {dialog?.kind === 'transaction' && (
        <TransactionForm
          transaction={dialog.transaction}
          initialType={dialog.type}
          accounts={bookableAccounts}
          categories={categories.data ?? []}
          onClose={() => setDialog(null)}
          onSaved={() => {
            setDialog(null)
            reload()
          }}
        />
      )}
      {dialog?.kind === 'transfer' && (
        <TransferForm
          transfer={dialog.transfer}
          accounts={bookableAccounts}
          onClose={() => setDialog(null)}
          onSaved={() => {
            setDialog(null)
            reload()
          }}
        />
      )}
    </>
  )
}

function AccountOptions({ accounts }: { accounts: Account[] }) {
  return (
    <>
      {accounts.map((account) => (
        <option key={account.id} value={account.id}>
          {account.name} ({account.currency})
        </option>
      ))}
    </>
  )
}

function TransactionForm({
  transaction,
  initialType,
  accounts,
  categories,
  onClose,
  onSaved,
}: {
  transaction: Transaction | null
  initialType: TransactionKind
  accounts: Account[]
  categories: Category[]
  onClose: () => void
  onSaved: () => void
}) {
  const [type, setType] = useState<TransactionKind>(initialType)
  const [amount, setAmount] = useState(transaction ? amountInput(Math.abs(transaction.amount)) : '')
  const [accountId, setAccountId] = useState(transaction?.accountId ?? accounts[0]?.id ?? '')
  const [categoryId, setCategoryId] = useState(transaction?.categoryId ?? '')
  const [date, setDate] = useState(() => (transaction ? transaction.date.slice(0, 16) : toDateTimeInput(new Date())))
  const [counterparty, setCounterparty] = useState(transaction?.counterparty ?? '')
  const [description, setDescription] = useState(transaction?.description ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = parseAmount(amount)
    if (value === null || value <= 0) {
      setError('Bitte einen Betrag größer als 0 eingeben.')
      return
    }
    if (!accountId) {
      setError('Bitte ein Konto wählen.')
      return
    }
    if (!date) {
      setError('Bitte ein Datum angeben.')
      return
    }
    setError(null)
    setSubmitting(true)
    const input = {
      accountId,
      categoryId: categoryId || null,
      amount: type === 'EXPENSE' ? -value : value,
      date,
      counterparty: blankToNull(counterparty),
      description: blankToNull(description),
    }
    try {
      if (transaction) {
        await transactionsApi.update(transaction.id, input)
      } else {
        await transactionsApi.create(input)
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={transaction ? 'Buchung bearbeiten' : type === 'EXPENSE' ? 'Neue Ausgabe' : 'Neue Einnahme'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <div className="segmented" role="radiogroup" aria-label="Art">
          <button type="button" role="radio" aria-checked={type === 'EXPENSE'} onClick={() => setType('EXPENSE')}>
            Ausgabe
          </button>
          <button type="button" role="radio" aria-checked={type === 'INCOME'} onClick={() => setType('INCOME')}>
            Einnahme
          </button>
        </div>
        <div className="form-row">
          <label>
            Betrag
            <input value={amount} inputMode="decimal" placeholder="0,00" onChange={(event) => setAmount(event.target.value)} autoFocus />
          </label>
          <label>
            Datum
            <input type="datetime-local" value={date} onChange={(event) => setDate(event.target.value)} />
          </label>
        </div>
        <div className="form-row">
          <label>
            Konto
            <select value={accountId} onChange={(event) => setAccountId(event.target.value)}>
              <AccountOptions accounts={accounts} />
            </select>
          </label>
          <label>
            Kategorie
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="">Keine Kategorie</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label>
          {type === 'EXPENSE' ? 'Empfänger' : 'Absender'}
          <input value={counterparty} maxLength={255} placeholder={type === 'EXPENSE' ? 'z. B. Supermarkt' : 'z. B. Arbeitgeber'} onChange={(event) => setCounterparty(event.target.value)} />
        </label>
        <label>
          Beschreibung
          <textarea value={description} maxLength={1000} rows={2} onChange={(event) => setDescription(event.target.value)} />
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}

function TransferForm({
  transfer,
  accounts,
  onClose,
  onSaved,
}: {
  transfer: Transfer | null
  accounts: Account[]
  onClose: () => void
  onSaved: () => void
}) {
  const [sendingAccountId, setSendingAccountId] = useState(transfer?.sendingAccountId ?? accounts[0]?.id ?? '')
  const [receivingAccountId, setReceivingAccountId] = useState(transfer?.receivingAccountId ?? accounts[1]?.id ?? '')
  const [sentAmount, setSentAmount] = useState(transfer ? amountInput(transfer.sentAmount) : '')
  const [receivedAmount, setReceivedAmount] = useState(transfer ? amountInput(transfer.receivedAmount) : '')
  const [date, setDate] = useState(() => (transfer ? transfer.date.slice(0, 16) : toDateTimeInput(new Date())))
  const [description, setDescription] = useState(transfer?.description ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const sending = accounts.find((account) => account.id === sendingAccountId)
  const receiving = accounts.find((account) => account.id === receivingAccountId)
  const differentCurrency = Boolean(sending && receiving && sending.currency !== receiving.currency)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const sent = parseAmount(sentAmount)
    const received = parseAmount(receivedAmount)
    if (!sendingAccountId || !receivingAccountId || sendingAccountId === receivingAccountId) {
      setError('Bitte zwei verschiedene Konten wählen.')
      return
    }
    if (sent === null || sent <= 0) {
      setError('Bitte einen Betrag größer als 0 eingeben.')
      return
    }
    if (differentCurrency && (received === null || received <= 0)) {
      setError('Bitte den empfangenen Betrag in der Zielwährung angeben.')
      return
    }
    setError(null)
    setSubmitting(true)
    const input = {
      sendingAccountId,
      receivingAccountId,
      sentAmount: sent,
      receivedAmount: differentCurrency ? received : null,
      date,
      description: blankToNull(description),
    }
    try {
      if (transfer) {
        await transfersApi.update(transfer.id, input)
      } else {
        await transfersApi.create(input)
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={transfer ? 'Transfer bearbeiten' : 'Neuer Transfer'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <div className="form-row">
          <label>
            Von
            <select value={sendingAccountId} onChange={(event) => setSendingAccountId(event.target.value)}>
              <AccountOptions accounts={accounts} />
            </select>
          </label>
          <label>
            Nach
            <select value={receivingAccountId} onChange={(event) => setReceivingAccountId(event.target.value)}>
              <AccountOptions accounts={accounts} />
            </select>
          </label>
        </div>
        <div className="form-row">
          <label>
            Betrag{sending?.currency ? ` (${sending.currency})` : ''}
            <input value={sentAmount} inputMode="decimal" placeholder="0,00" onChange={(event) => setSentAmount(event.target.value)} autoFocus />
          </label>
          {differentCurrency ? (
            <label>
              Empfangen ({receiving?.currency})
              <input value={receivedAmount} inputMode="decimal" placeholder="0,00" onChange={(event) => setReceivedAmount(event.target.value)} />
            </label>
          ) : (
            <label>
              Datum
              <input type="datetime-local" value={date} onChange={(event) => setDate(event.target.value)} />
            </label>
          )}
        </div>
        {differentCurrency && (
          <label>
            Datum
            <input type="datetime-local" value={date} onChange={(event) => setDate(event.target.value)} />
          </label>
        )}
        <label>
          Beschreibung
          <textarea value={description} maxLength={1000} rows={2} onChange={(event) => setDescription(event.target.value)} />
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}
