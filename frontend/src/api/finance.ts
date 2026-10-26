import { request } from './client'
import type {
  Account,
  AccountCreateInput,
  AccountType,
  AccountTypeInput,
  AccountUpdateInput,
  AssetClass,
  Category,
  CategoryInput,
  Dashboard,
  Holding,
  HoldingInput,
  Page,
  Provider,
  ProviderInput,
  QuoteCandidate,
  Transaction,
  TransactionInput,
  TransactionQuery,
  Transfer,
  TransferInput,
  TransferQuery,
} from './types'

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

function crud<T, Input>(base: string) {
  return {
    list: () => request<T[]>('GET', base),
    create: (input: Input) => request<T>('POST', base, input),
    update: (id: string, input: Input) => request<T>('PUT', `${base}/${id}`, input),
    remove: (id: string) => request<void>('DELETE', `${base}/${id}`),
  }
}

export const categoriesApi = crud<Category, CategoryInput>('/api/categories')

export const providersApi = crud<Provider, ProviderInput>('/api/providers')

export const accountTypesApi = crud<AccountType, AccountTypeInput>('/api/account-types')

export const accountsApi = {
  list: () => request<Account[]>('GET', '/api/accounts'),
  create: (input: AccountCreateInput) => request<Account>('POST', '/api/accounts', input),
  update: (id: string, input: AccountUpdateInput) => request<Account>('PUT', `/api/accounts/${id}`, input),
  remove: (id: string) => request<void>('DELETE', `/api/accounts/${id}`),
}

export const holdingsApi = {
  listAll: (refresh = false) => request<Holding[]>('GET', `/api/holdings${query({ refresh: refresh ? 'true' : undefined })}`),
  list: (accountId: string) => request<Holding[]>('GET', `/api/accounts/${accountId}/holdings`),
  create: (accountId: string, input: HoldingInput) =>
    request<Holding>('POST', `/api/accounts/${accountId}/holdings`, input),
  update: (id: string, input: HoldingInput) => request<Holding>('PUT', `/api/holdings/${id}`, input),
  remove: (id: string) => request<void>('DELETE', `/api/holdings/${id}`),
}

export const transactionsApi = {
  list: (params: TransactionQuery) => request<Page<Transaction>>('GET', `/api/transactions${query(params)}`),
  create: (input: TransactionInput) => request<Transaction>('POST', '/api/transactions', input),
  update: (id: string, input: TransactionInput) => request<Transaction>('PUT', `/api/transactions/${id}`, input),
  remove: (id: string) => request<void>('DELETE', `/api/transactions/${id}`),
}

export const transfersApi = {
  list: (params: TransferQuery) => request<Page<Transfer>>('GET', `/api/transfers${query(params)}`),
  create: (input: TransferInput) => request<Transfer>('POST', '/api/transfers', input),
  update: (id: string, input: TransferInput) => request<Transfer>('PUT', `/api/transfers/${id}`, input),
  remove: (id: string) => request<void>('DELETE', `/api/transfers/${id}`),
}

export const dashboardApi = {
  get: (from: string, to: string, refresh = false) =>
    request<Dashboard>('GET', `/api/dashboard${query({ from, to, refresh: refresh ? 'true' : undefined })}`),
}

export const quotesApi = {
  search: (searchQuery: string, assetClass: AssetClass) =>
    request<QuoteCandidate[]>('GET', `/api/quotes/search${query({ query: searchQuery, assetClass })}`),
}
