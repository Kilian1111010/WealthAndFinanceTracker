import { Navigate, Route, Routes } from 'react-router-dom'
import { GuestOnly, RequireAuth } from './auth/routes'
import { AppLayout } from './components/AppLayout'
import { BookingsPage } from './pages/BookingsPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { AccountTypesSettings } from './pages/settings/AccountTypesSettings'
import { AccountsSettings } from './pages/settings/AccountsSettings'
import { CategoriesSettings } from './pages/settings/CategoriesSettings'
import { ProvidersSettings } from './pages/settings/ProvidersSettings'
import { SettingsLayout } from './pages/settings/SettingsLayout'

export default function App() {
  return (
    <Routes>
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="bookings" element={<BookingsPage />} />
          <Route path="settings" element={<SettingsLayout />}>
            <Route index element={<Navigate to="accounts" replace />} />
            <Route path="accounts" element={<AccountsSettings />} />
            <Route path="account-types" element={<AccountTypesSettings />} />
            <Route path="providers" element={<ProvidersSettings />} />
            <Route path="categories" element={<CategoriesSettings />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
