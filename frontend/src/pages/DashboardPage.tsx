import { useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/finance'
import type { CurrencyAmount, Dashboard } from '../api/types'
import { MonthlyChart } from '../components/MonthlyChart'
import { ErrorText } from '../components/ui'
import {
  assetClassLabels,
  assetClassOrder,
  currentMonth,
  formatDate,
  formatMoney,
  formatMonth,
  formatNumber,
  formatSignedMoney,
  monthRange,
  shiftMonth,
} from '../lib/format'
import { useLoader } from '../lib/useLoader'

function currenciesOf(dashboard: Dashboard): string[] {
  const all = [
    ...dashboard.netWorth,
    ...dashboard.income,
    ...dashboard.expenses,
    ...dashboard.monthly.map((entry) => ({ currency: entry.currency })),
  ].map((entry) => entry.currency)
  return [...new Set(all)].sort()
}

function amountIn(list: CurrencyAmount[], currency: string): number {
  return list.find((entry) => entry.currency === currency)?.amount ?? 0
}

function isEmpty(dashboard: Dashboard): boolean {
  return (
    dashboard.netWorth.length === 0 &&
    dashboard.holdings.length === 0 &&
    dashboard.recentTransactions.length === 0 &&
    dashboard.monthly.length === 0
  )
}

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth())
  const [selectedCurrency, setSelectedCurrency] = useState<string | null>(null)
  const range = monthRange(month)
  const refreshNext = useRef(false)
  const [refreshCount, setRefreshCount] = useState(0)
  const dashboard = useLoader(() => {
    const refresh = refreshNext.current
    refreshNext.current = false
    return dashboardApi.get(range.from, range.to, refresh)
  }, [month, refreshCount])

  function refreshPrices() {
    refreshNext.current = true
    setRefreshCount((count) => count + 1)
  }

  const currencies = useMemo(() => (dashboard.data ? currenciesOf(dashboard.data) : []), [dashboard.data])
  const currency =
    selectedCurrency && currencies.includes(selectedCurrency)
      ? selectedCurrency
      : currencies.includes('EUR')
        ? 'EUR'
        : currencies[0]

  return (
    <div className="dashboard">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Dein Überblick über Vermögen, Einnahmen und Ausgaben.</p>
        </div>
        <div className="page-actions">
          {currencies.length > 1 && (
            <select aria-label="Währung" value={currency} onChange={(event) => setSelectedCurrency(event.target.value)}>
              {currencies.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
            </select>
          )}
          <div className="month-picker">
            <button type="button" className="icon-button" aria-label="Vorheriger Monat" onClick={() => setMonth(shiftMonth(month, -1))}>
              ‹
            </button>
            <span className="month-label">{formatMonth(month, 'long')}</span>
            <button type="button" className="icon-button" aria-label="Nächster Monat" onClick={() => setMonth(shiftMonth(month, 1))}>
              ›
            </button>
          </div>
        </div>
      </div>

      <ErrorText message={dashboard.error} />
      {dashboard.loading && !dashboard.data && <p className="muted">Lädt …</p>}
      {dashboard.data && isEmpty(dashboard.data) && <Onboarding />}
      {dashboard.data && !isEmpty(dashboard.data) && (
        <DashboardContent
          dashboard={dashboard.data}
          currency={currency ?? 'EUR'}
          month={month}
          refreshing={dashboard.loading}
          onRefreshPrices={refreshPrices}
        />
      )}
    </div>
  )
}

function Onboarding() {
  return (
    <div className="card onboarding">
      <h2>Los geht’s</h2>
      <ol>
        <li>
          Lege unter <Link to="/settings/account-types">Einstellungen → Kontotypen</Link> Typen an, z. B. „Girokonto“ oder „Depot“.
        </li>
        <li>
          Lege unter <Link to="/settings/accounts">Einstellungen → Konten</Link> deine Konten mit Anfangsstand an.
        </li>
        <li>
          Erfasse unter <Link to="/bookings">Buchungen</Link> deine Einnahmen und Ausgaben.
        </li>
      </ol>
    </div>
  )
}

function DashboardContent({
  dashboard,
  currency,
  month,
  refreshing,
  onRefreshPrices,
}: {
  dashboard: Dashboard
  currency: string
  month: string
  refreshing: boolean
  onRefreshPrices: () => void
}) {
  const netWorth = amountIn(dashboard.netWorth, currency)
  const income = amountIn(dashboard.income, currency)
  const expenses = amountIn(dashboard.expenses, currency)
  const net = amountIn(dashboard.net, currency)
  const transferVolume = amountIn(dashboard.transfers.sent, currency)
  const monthly = dashboard.monthly.filter((entry) => entry.currency === currency)
  const categories = dashboard.expensesByCategory.filter((entry) => entry.currency === currency)
  const assetClasses = assetClassOrder
    .map((assetClass) => dashboard.netWorthByAssetClass.find((entry) => entry.assetClass === assetClass && entry.currency === currency))
    .filter((entry) => entry !== undefined)
  const otherCurrencies = dashboard.netWorth.filter((entry) => entry.currency !== currency)
  const monthName = formatMonth(month, 'long')

  return (
    <>
      <div className="tiles">
        <div className="tile hero">
          <span className="tile-label">Vermögen</span>
          <span className="tile-value">{formatMoney(netWorth, currency)}</span>
          <span className="tile-note">
            {otherCurrencies.length > 0 && `zzgl. ${otherCurrencies.map((entry) => formatMoney(entry.amount, entry.currency)).join(', ')} · `}
            {dashboard.holdings.length === 0
              ? 'aktueller Stand'
              : dashboard.pricesIncomplete
                ? 'inkl. Bestände, einige Kurse fehlen oder sind veraltet'
                : 'inkl. Wertpapiere und Krypto'}
          </span>
        </div>
        <div className="tile">
          <span className="tile-label">Einnahmen</span>
          <span className="tile-value">{formatMoney(income, currency)}</span>
          <span className="tile-note">{monthName}</span>
        </div>
        <div className="tile">
          <span className="tile-label">Ausgaben</span>
          <span className="tile-value">{formatMoney(expenses, currency)}</span>
          <span className="tile-note">{monthName}</span>
        </div>
        <div className="tile">
          <span className="tile-label">Überschuss</span>
          <span className={`tile-value ${net > 0 ? 'positive' : ''}`}>{formatSignedMoney(net, currency)}</span>
          <span className="tile-note">Einnahmen − Ausgaben</span>
        </div>
        <div className="tile">
          <span className="tile-label">Übertragen</span>
          <span className="tile-value">{formatMoney(transferVolume, currency)}</span>
          <span className="tile-note">
            {dashboard.transfers.count} {dashboard.transfers.count === 1 ? 'Transfer' : 'Transfers'} zwischen Konten
          </span>
        </div>
      </div>

      <section className="card">
        <h2>Einnahmen &amp; Ausgaben · letzte 12 Monate</h2>
        {monthly.length > 0 ? (
          <MonthlyChart data={monthly} currency={currency} />
        ) : (
          <p className="muted">Noch keine Buchungen in {currency}.</p>
        )}
      </section>

      <div className="columns">
        <section className="card">
          <h2>Ausgaben nach Kategorie</h2>
          <p className="muted card-sub">{monthName}</p>
          {categories.length === 0 ? (
            <p className="muted">Keine Ausgaben in diesem Monat.</p>
          ) : (
            <ul className="bar-list">
              {categories.map((entry) => (
                <li key={entry.categoryId ?? 'none'}>
                  <div className="bar-list-row">
                    <span>{entry.categoryName ?? 'Ohne Kategorie'}</span>
                    <span className="bar-list-value">
                      {formatMoney(entry.amount, currency)}
                      <span className="muted"> · {Math.round((entry.amount / expenses) * 100)} %</span>
                    </span>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill expense" style={{ width: `${(entry.amount / categories[0].amount) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h2>Vermögen nach Anlageklasse</h2>
          <p className="muted card-sub">aktueller Stand</p>
          {assetClasses.length === 0 ? (
            <p className="muted">Keine Konten mit Kontostand in {currency}.</p>
          ) : (
            <ul className="bar-list">
              {assetClasses.map((entry) => {
                const largest = Math.max(...assetClasses.map((item) => item.amount))
                return (
                  <li key={entry.assetClass}>
                    <div className="bar-list-row">
                      <span>{assetClassLabels[entry.assetClass]}</span>
                      <span className="bar-list-value">{formatMoney(entry.amount, currency)}</span>
                    </div>
                    {entry.amount > 0 && largest > 0 && (
                      <div className="bar-track">
                        <div className="bar-fill income" style={{ width: `${(entry.amount / largest) * 100}%` }} />
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      </div>

      <div className="columns">
        <section className="card">
          <div className="card-header">
            <h2>Letzte Buchungen</h2>
            <Link to="/bookings">Alle anzeigen</Link>
          </div>
          {dashboard.recentTransactions.length === 0 ? (
            <p className="muted">Noch keine Buchungen.</p>
          ) : (
            <ul className="activity">
              {dashboard.recentTransactions.map((transaction) => (
                <li key={transaction.id}>
                  <div>
                    <span className="item-title">
                      {transaction.counterparty ?? transaction.categoryName ?? (transaction.amount > 0 ? 'Einnahme' : 'Ausgabe')}
                    </span>
                    <span className="sub-text">
                      {formatDate(transaction.date)} · {transaction.accountName}
                      {transaction.categoryName && transaction.counterparty ? ` · ${transaction.categoryName}` : ''}
                    </span>
                  </div>
                  <span className={`nowrap ${transaction.amount > 0 ? 'positive' : ''}`}>
                    {formatSignedMoney(transaction.amount, transaction.currency)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <div className="card-header">
            <h2>Bestände</h2>
            <button type="button" className="link" disabled={refreshing} onClick={onRefreshPrices}>
              {refreshing ? 'Aktualisiere …' : 'Kurse aktualisieren'}
            </button>
          </div>
          {dashboard.holdings.length === 0 ? (
            <p className="muted">
              Keine Wertpapier- oder Krypto-Bestände. <Link to="/settings/accounts">Bestände anlegen</Link>
            </p>
          ) : (
            <>
              <ul className="activity">
                {dashboard.holdings.map((holding) => (
                  <li key={holding.key}>
                    <div>
                      <span className="item-title">
                        {holding.symbol ?? holding.name}
                        {holding.manual && <span className="badge">manuell</span>}
                      </span>
                      <span className="sub-text">
                        {formatNumber(holding.quantity)} Stück
                        {holding.symbol && holding.name ? ` · ${holding.name}` : ''}
                        {holding.accounts > 1 ? ` · in ${holding.accounts} Konten` : ''}
                      </span>
                    </div>
                    <span className="nowrap holding-value">
                      {holding.valueEur === null ? <span className="muted">kein Kurs</span> : formatMoney(holding.valueEur, 'EUR')}
                      {holding.stale && <span className="sub-text">Kurs veraltet</span>}
                    </span>
                  </li>
                ))}
              </ul>
              <div className="holdings-total">
                <span>Gesamtwert</span>
                <span className="item-title">{formatMoney(dashboard.holdingsValueEur, 'EUR')}</span>
              </div>
              <p className="muted note">Kurse von Yahoo Finance und CoinGecko, umgerechnet mit EZB-Kursen.</p>
            </>
          )}
        </section>
      </div>
    </>
  )
}
