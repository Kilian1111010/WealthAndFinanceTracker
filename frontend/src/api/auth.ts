import { ApiError, request } from './client'

export type User = {
  userId: string
  name: string
}

export type Credentials = {
  username: string
  password: string
}

export const authApi = {
  async me(): Promise<User | null> {
    try {
      return await request<User>('GET', '/auth/me')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        return null
      }
      throw error
    }
  },
  login: (credentials: Credentials) => request<User>('POST', '/auth/login', credentials),
  register: (credentials: Credentials) => request<User>('POST', '/auth/register', credentials),
  logout: () => request<void>('POST', '/auth/logout'),
}
