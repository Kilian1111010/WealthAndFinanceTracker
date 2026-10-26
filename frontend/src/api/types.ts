export type AssetClass = 'CASH' | 'SAVINGS' | 'INVESTMENT' | 'CRYPTO' | 'LIABILITY' | 'OTHER'

export type Category = {
  id: string
  name: string
}

export type CategoryInput = {
  name: string
}

export type Provider = {
  id: string
  name: string
  country: string | null
  exemptionAmount: number | null
  website: string | null
  logoUrl: string | null
}

export type ProviderInput = Omit<Provider, 'id'>

export type AccountType = {
  id: string
  name: string
  assetClass: AssetClass
  includeInNetWorth: boolean
}

export type AccountTypeInput = Omit<AccountType, 'id'>

export type Account = {
  id: string
  name: string
  accountTypeId: string
  accountTypeName: string
  assetClass: AssetClass
  includeInNetWorth: boolean
  providerId: string | null
  providerName: string | null
  parentAccountId: string | null
  balance: number | null
  currency: string | null
  description: string | null
  archived: boolean
  virtual: boolean
}

export type AccountCreateInput = {
  name: string
  accountTypeId: string
  providerId: string | null
  parentAccountId: string | null
  currency: string | null
  balance: number | null
  description: string | null
}

export type AccountUpdateInput = {
  name: string
  providerId: string | null
  balance: number | null
  description: string | null
  archived: boolean
}

export type QuoteSource = 'YAHOO' | 'COINGECKO'

export type QuoteCandidate = {
  source: QuoteSource
  quoteId: string
  symbol: string
  name: string
  exchange: string | null
}

export type Holding = {
  id: string
  accountId: string
  accountName: string
  manual: boolean
  symbol: string | null
  name: string | null
  quantity: number
  quoteSource: QuoteSource | null
  quoteId: string | null
  manualValue: number | null
  price: number | null
  priceCurrency: string | null
  priceAt: string | null
  priceStale: boolean
  valueEur: number | null
}

export type HoldingInput = {
  manual: boolean
  symbol: string | null
  name: string | null
  quoteSource: QuoteSource | null
  quoteId: string | null
  quantity: number
  manualValue: number | null
}

export type Transaction = {
  id: string
  accountId: string
  accountName: string
  currency: string | null
  categoryId: string | null
  categoryName: string | null
  amount: number
  date: string
  counterparty: string | null
  description: string | null
}

export type TransactionInput = {
  accountId: string
  categoryId: string | null
  amount: number
  date: string
  counterparty: string | null
  description: string | null
}

export type TransactionKind = 'INCOME' | 'EXPENSE'

export type TransactionQuery = {
  from?: string
  to?: string
  accountId?: string
  categoryId?: string
  kind?: TransactionKind
  page?: number
  size?: number
}

export type Transfer = {
  id: string
  sendingAccountId: string
  sendingAccountName: string
  sendingCurrency: string | null
  receivingAccountId: string
  receivingAccountName: string
  receivingCurrency: string | null
  sentAmount: number
  receivedAmount: number
  date: string
  description: string | null
}

export type TransferInput = {
  sendingAccountId: string
  receivingAccountId: string
  sentAmount: number
  receivedAmount: number | null
  date: string
  description: string | null
}

export type TransferQuery = {
  from?: string
  to?: string
  accountId?: string
  page?: number
  size?: number
}

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type CurrencyAmount = {
  currency: string
  amount: number
}

export type Dashboard = {
  from: string
  to: string
  netWorth: CurrencyAmount[]
  netWorthByAssetClass: { assetClass: AssetClass; currency: string; amount: number }[]
  income: CurrencyAmount[]
  expenses: CurrencyAmount[]
  net: CurrencyAmount[]
  transfers: { count: number; sent: CurrencyAmount[] }
  expensesByCategory: { categoryId: string | null; categoryName: string | null; currency: string; amount: number }[]
  monthly: { month: string; currency: string; income: number; expenses: number }[]
  holdings: {
    key: string
    symbol: string | null
    name: string | null
    quantity: number
    accounts: number
    manual: boolean
    valueEur: number | null
    stale: boolean
  }[]
  holdingsValueEur: number
  pricesIncomplete: boolean
  recentTransactions: Transaction[]
}
