import { ApiError } from '../api/client'
import type { AssetClass } from '../api/types'

const LOCALE = 'de-DE'

export const assetClassLabels: Record<AssetClass, string> = {
  CASH: 'Bargeld & Giro',
  SAVINGS: 'Sparen',
  INVESTMENT: 'Wertpapiere',
  CRYPTO: 'Krypto',
  LIABILITY: 'Verbindlichkeiten',
  OTHER: 'Sachwerte & Sonstiges',
}

export const assetClassOrder: AssetClass[] = ['CASH', 'SAVINGS', 'INVESTMENT', 'CRYPTO', 'LIABILITY', 'OTHER']

export function hasBalance(assetClass: AssetClass): boolean {
  return assetClass !== 'INVESTMENT' && assetClass !== 'CRYPTO'
}

export function holdsPositions(assetClass: AssetClass): boolean {
  return assetClass === 'INVESTMENT' || assetClass === 'CRYPTO'
}

export function isBookable(assetClass: AssetClass): boolean {
  return assetClass === 'CASH' || assetClass === 'SAVINGS' || assetClass === 'LIABILITY'
}

export function allowsSubAccounts(assetClass: AssetClass): boolean {
  return assetClass === 'CASH' || assetClass === 'SAVINGS'
}

export function formatMoney(amount: number, currency: string | null): string {
  if (!currency) {
    return formatNumber(amount, 2)
  }
  return new Intl.NumberFormat(LOCALE, { style: 'currency', currency }).format(amount)
}

export function formatSignedMoney(amount: number, currency: string | null): string {
  const text = formatMoney(Math.abs(amount), currency)
  if (amount > 0) return `+${text}`
  if (amount < 0) return `−${text}`
  return text
}

export function formatNumber(value: number, maximumFractionDigits = 8): string {
  return new Intl.NumberFormat(LOCALE, { maximumFractionDigits }).format(value)
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: 'medium' }).format(new Date(iso))
}

export function formatMonth(month: string, style: 'short' | 'long' = 'short'): string {
  const [year, monthIndex] = month.split('-').map(Number)
  return new Intl.DateTimeFormat(LOCALE, { month: style, year: style === 'long' ? 'numeric' : '2-digit' }).format(
    new Date(year, monthIndex - 1, 1),
  )
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function toDateInput(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function toDateTimeInput(date: Date): string {
  return `${toDateInput(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function currentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`
}

export function shiftMonth(month: string, delta: number): string {
  const [year, monthIndex] = month.split('-').map(Number)
  const date = new Date(year, monthIndex - 1 + delta, 1)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

export function monthRange(month: string): { from: string; to: string } {
  const [year, monthIndex] = month.split('-').map(Number)
  return {
    from: toDateInput(new Date(year, monthIndex - 1, 1)),
    to: toDateInput(new Date(year, monthIndex, 0)),
  }
}

export function parseAmount(value: string): number | null {
  const normalized = value.trim().replace(/\s/g, '').replace(',', '.')
  if (normalized === '') return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

export function amountInput(value: number | null): string {
  return value === null ? '' : String(value).replace('.', ',')
}

export function errorText(error: unknown, fallback = 'Aktion fehlgeschlagen. Bitte erneut versuchen.'): string {
  if (error instanceof ApiError && error.message) {
    return error.message
  }
  return fallback
}

export function safeHref(value: string | null): string | null {
  if (!value) return null
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
}

export function blankToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
