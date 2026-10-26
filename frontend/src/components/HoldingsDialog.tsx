import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { holdingsApi, quotesApi } from '../api/finance'
import type { Account, AssetClass, Holding, QuoteCandidate } from '../api/types'
import { amountInput, blankToNull, errorText, formatMoney, formatNumber, parseAmount } from '../lib/format'
import { useLoader } from '../lib/useLoader'
import { Modal } from './Modal'
import { ErrorText } from './ui'

type Texts = {
  searchLabel: string
  searchPlaceholder: string
  quantityPlaceholder: string
}

const texts: Record<'INVESTMENT' | 'CRYPTO', Texts> = {
  INVESTMENT: {
    searchLabel: 'Wertpapier suchen',
    searchPlaceholder: 'Ticker oder Name, z. B. EUNL oder Apple',
    quantityPlaceholder: '10',
  },
  CRYPTO: {
    searchLabel: 'Kryptowährung suchen',
    searchPlaceholder: 'Symbol oder Name, z. B. BTC oder Ethereum',
    quantityPlaceholder: '0,5',
  },
}

function holdingTitle(holding: Holding): string {
  return holding.symbol ?? holding.name ?? 'Bestand'
}

export function HoldingsDialog({ account, onClose }: { account: Account; onClose: () => void }) {
  const holdings = useLoader(() => holdingsApi.list(account.id), [account.id])
  const [editing, setEditing] = useState<Holding | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete(holding: Holding) {
    if (!window.confirm(`Bestand ${holdingTitle(holding)} löschen?`)) return
    setError(null)
    try {
      await holdingsApi.remove(holding.id)
      holdings.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  const total = (holdings.data ?? []).reduce((sum, holding) => sum + (holding.valueEur ?? 0), 0)

  return (
    <Modal title={`Bestände: ${account.name}`} onClose={onClose}>
      <ErrorText message={error ?? holdings.error} />
      {holdings.data && holdings.data.length === 0 && <p className="muted">Noch keine Bestände.</p>}
      {holdings.data && holdings.data.length > 0 && (
        <table className="table compact">
          <thead>
            <tr>
              <th>Bestand</th>
              <th className="number">Stück</th>
              <th className="number">Kurs</th>
              <th className="number">Wert</th>
              <th aria-label="Aktionen" />
            </tr>
          </thead>
          <tbody>
            {holdings.data.map((holding) => (
              <tr key={holding.id}>
                <td>
                  <span className="item-title">{holdingTitle(holding)}</span>
                  {holding.manual && <span className="badge">manuell</span>}
                  {holding.symbol && holding.name && <span className="sub-text">{holding.name}</span>}
                </td>
                <td className="number">{formatNumber(holding.quantity, 18)}</td>
                <td className="number nowrap">
                  {holding.manual ? (
                    '–'
                  ) : holding.price !== null && holding.priceCurrency ? (
                    <>
                      {formatMoney(holding.price, holding.priceCurrency)}
                      {holding.priceStale && <span className="sub-text">veraltet</span>}
                    </>
                  ) : (
                    <span className="muted">kein Kurs</span>
                  )}
                </td>
                <td className="number nowrap">{holding.valueEur === null ? '–' : formatMoney(holding.valueEur, 'EUR')}</td>
                <td className="row-actions">
                  <button type="button" className="link" onClick={() => setEditing(holding)}>
                    Bearbeiten
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(holding)}>
                    Löschen
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={3}>Gesamt</td>
              <td className="number nowrap">{formatMoney(total, 'EUR')}</td>
              <td />
            </tr>
          </tfoot>
        </table>
      )}
      <HoldingForm
        key={editing?.id ?? 'new'}
        accountId={account.id}
        assetClass={account.assetClass}
        holding={editing}
        onCancel={() => setEditing(null)}
        onSaved={() => {
          setEditing(null)
          holdings.reload()
        }}
      />
    </Modal>
  )
}

function initialCandidate(holding: Holding | null): QuoteCandidate | null {
  if (!holding || holding.manual || !holding.quoteSource || !holding.quoteId) return null
  return {
    source: holding.quoteSource,
    quoteId: holding.quoteId,
    symbol: holding.symbol ?? holding.quoteId,
    name: holding.name ?? holding.quoteId,
    exchange: null,
  }
}

function HoldingForm({
  accountId,
  assetClass,
  holding,
  onCancel,
  onSaved,
}: {
  accountId: string
  assetClass: AssetClass
  holding: Holding | null
  onCancel: () => void
  onSaved: () => void
}) {
  const text = texts[assetClass === 'CRYPTO' ? 'CRYPTO' : 'INVESTMENT']
  const [manual, setManual] = useState(holding?.manual ?? false)
  const [selected, setSelected] = useState<QuoteCandidate | null>(initialCandidate(holding))
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<QuoteCandidate[] | null>(null)
  const [searching, setSearching] = useState(false)
  const [name, setName] = useState(holding?.manual ? (holding.name ?? '') : '')
  const [symbol, setSymbol] = useState(holding?.manual ? (holding.symbol ?? '') : '')
  const [quantity, setQuantity] = useState(holding ? amountInput(holding.quantity) : '')
  const [manualValue, setManualValue] = useState(amountInput(holding?.manualValue ?? null))
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function search() {
    if (query.trim().length < 2) {
      setError('Bitte mindestens zwei Zeichen eingeben.')
      return
    }
    setError(null)
    setSearching(true)
    try {
      setResults(await quotesApi.search(query.trim(), assetClass))
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSearching(false)
    }
  }

  function handleSearchKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault()
      search()
    }
  }

  function choose(candidate: QuoteCandidate) {
    setSelected(candidate)
    setResults(null)
    setQuery('')
  }

  function reset() {
    setManual(false)
    setSelected(null)
    setResults(null)
    setQuery('')
    setName('')
    setSymbol('')
    setQuantity('')
    setManualValue('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const quantityValue = parseAmount(quantity)
    const value = parseAmount(manualValue)
    if (!manual && !selected) {
      setError('Bitte zuerst einen Kurs aus der Suche auswählen.')
      return
    }
    if (manual && !name.trim()) {
      setError('Bitte einen Namen angeben.')
      return
    }
    if (quantityValue === null || quantityValue < 0) {
      setError('Die Stückzahl muss eine Zahl ab 0 sein.')
      return
    }
    if (manual && (value === null || value < 0)) {
      setError('Bitte den Gesamtwert in Euro angeben.')
      return
    }
    setError(null)
    setSubmitting(true)
    const input = manual
      ? { manual: true, symbol: blankToNull(symbol), name: name.trim(), quoteSource: null, quoteId: null, quantity: quantityValue, manualValue: value }
      : {
          manual: false,
          symbol: selected!.symbol,
          name: selected!.name,
          quoteSource: selected!.source,
          quoteId: selected!.quoteId,
          quantity: quantityValue,
          manualValue: null,
        }
    try {
      if (holding) {
        await holdingsApi.update(holding.id, input)
      } else {
        await holdingsApi.create(accountId, input)
        reset()
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="inline-form">
      <h3>{holding ? `${holdingTitle(holding)} bearbeiten` : 'Bestand hinzufügen'}</h3>
      <label className="checkbox">
        <input type="checkbox" checked={manual} onChange={(event) => setManual(event.target.checked)} />
        Ohne Ticker – Wert selbst eingeben
      </label>

      {!manual && (
        <>
          {selected ? (
            <div className="selected-quote">
              <div>
                <span className="item-title">{selected.symbol}</span>
                <span className="sub-text">
                  {selected.name}
                  {selected.exchange ? ` · ${selected.exchange}` : ''}
                </span>
              </div>
              <button type="button" className="link" onClick={() => setSelected(null)}>
                Ändern
              </button>
            </div>
          ) : (
            <label>
              {text.searchLabel}
              <div className="search-row">
                <input
                  value={query}
                  placeholder={text.searchPlaceholder}
                  onChange={(event) => setQuery(event.target.value)}
                  onKeyDown={handleSearchKey}
                />
                <button type="button" className="secondary" disabled={searching} onClick={search}>
                  {searching ? 'Suche …' : 'Suchen'}
                </button>
              </div>
            </label>
          )}
          {!selected && results && results.length === 0 && <p className="muted">Keine Treffer.</p>}
          {!selected && results && results.length > 0 && (
            <ul className="quote-results" role="listbox" aria-label="Suchergebnisse">
              {results.map((candidate) => (
                <li key={`${candidate.source}:${candidate.quoteId}`}>
                  <button type="button" role="option" aria-selected="false" onClick={() => choose(candidate)}>
                    <span className="item-title">{candidate.symbol}</span>
                    <span className="sub-text">
                      {candidate.name}
                      {candidate.exchange ? ` · ${candidate.exchange}` : ''}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <label>
            Stückzahl
            <input value={quantity} inputMode="decimal" placeholder={text.quantityPlaceholder} onChange={(event) => setQuantity(event.target.value)} />
          </label>
        </>
      )}

      {manual && (
        <>
          <div className="form-row">
            <label>
              Name
              <input value={name} maxLength={255} placeholder="z. B. Betriebsrente" onChange={(event) => setName(event.target.value)} />
            </label>
            <label>
              Kürzel (optional)
              <input value={symbol} maxLength={50} onChange={(event) => setSymbol(event.target.value)} />
            </label>
          </div>
          <div className="form-row">
            <label>
              Stückzahl
              <input value={quantity} inputMode="decimal" placeholder="1" onChange={(event) => setQuantity(event.target.value)} />
            </label>
            <label>
              Gesamtwert (€)
              <input value={manualValue} inputMode="decimal" placeholder="0,00" onChange={(event) => setManualValue(event.target.value)} />
            </label>
          </div>
        </>
      )}

      <ErrorText message={error} />
      <div className="form-actions">
        {holding && (
          <button type="button" className="secondary" onClick={onCancel}>
            Abbrechen
          </button>
        )}
        <button type="submit" disabled={submitting}>
          {holding ? 'Speichern' : 'Hinzufügen'}
        </button>
      </div>
    </form>
  )
}
