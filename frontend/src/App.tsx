import { useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router'
import { AppShell } from './components/layout/AppShell'
import { AppErrorBoundary } from './components/errors/AppErrorBoundary'
import { WorkflowProvider } from './context/WorkflowContext'
import { useWorkflow } from './context/workflowState'
import { AdminProvider } from './context/AdminContext'
import { ThemeProvider } from './context/ThemeContext'
import { getDemoUserByRole } from './mocks/users'
import { createMockRegistrationRequest } from './mocks/registrationRequests'
import { DashboardPage } from './pages/DashboardPage'
import { ErrorStatePage } from './pages/ErrorStatePage'
import { LoginPage } from './pages/LoginPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { ProfilePage } from './pages/ProfilePage'
import { RecordDetailPage } from './pages/RecordDetailPage'
import { RecordEditPage } from './pages/RecordEditPage'
import { RecordsPage } from './pages/RecordsPage'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage'
import { AdminLogsPage } from './pages/admin/AdminLogsPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
import type { AuthUser, UserRole } from './types/auth'

const mockSessionKey = 'ebys:mock-session:v1'
const userRoles: UserRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']

function isAuthUser(value: unknown): value is AuthUser {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<AuthUser>
  return (
    typeof candidate.id === 'string' &&
    typeof candidate.firstName === 'string' &&
    typeof candidate.lastName === 'string' &&
    typeof candidate.email === 'string' &&
    Boolean(candidate.role && userRoles.includes(candidate.role))
  )
}

function readMockSession(): AuthUser | null {
  try {
    const storedSession = window.sessionStorage.getItem(mockSessionKey)
    if (!storedSession) return null
    const parsedSession: unknown = JSON.parse(storedSession)
    if (isAuthUser(parsedSession)) return parsedSession
    window.sessionStorage.removeItem(mockSessionKey)
    return null
  } catch {
    window.sessionStorage.removeItem(mockSessionKey)
    return null
  }
}

function persistMockSession(user: AuthUser | null) {
  if (user) window.sessionStorage.setItem(mockSessionKey, JSON.stringify(user))
  else window.sessionStorage.removeItem(mockSessionKey)
}

function App() {
  const [user, setUser] = useState<AuthUser | null>(readMockSession)

  const handleLogin = (authenticatedUser: AuthUser) => {
    persistMockSession(authenticatedUser)
    setUser(authenticatedUser)
  }

  return (
    <ThemeProvider>
      <AppErrorBoundary>
        <Routes>
          <Route
            path="/giris"
            element={(
              <LoginPage
                user={user}
                onLogin={handleLogin}
                onRegister={createMockRegistrationRequest}
              />
            )}
          />
          <Route
            path="/*"
            element={
              <ProtectedApplication
                user={user}
                onUserChange={(nextUser) => {
                  persistMockSession(nextUser)
                  setUser(nextUser)
                }}
              />
            }
          />
        </Routes>
      </AppErrorBoundary>
    </ThemeProvider>
  )
}

function ProtectedApplication({
  user,
  onUserChange,
}: {
  user: AuthUser | null
  onUserChange: (user: AuthUser | null) => void
}) {
  const location = useLocation()
  const navigate = useNavigate()

  if (!user) {
    const returnTo = `${location.pathname}${location.search}${location.hash}`
    return <Navigate to={`/giris?returnTo=${encodeURIComponent(returnTo)}`} replace />
  }

  const handleRoleChange = (nextRole: UserRole) => {
    onUserChange(getDemoUserByRole(nextRole))
    navigate(nextRole === 'ADMIN' ? '/admin' : '/dashboard')
  }

  const handleLogout = () => {
    navigate('/giris', { replace: true })
    onUserChange(null)
  }

  if (user.role === 'ADMIN') {
    return (
      <AdminProvider actor={user}>
        <AdminApplication
          user={user}
          onRoleChange={handleRoleChange}
          onLogout={handleLogout}
        />
      </AdminProvider>
    )
  }

  return (
    <WorkflowProvider user={user}>
      <WorkflowApplication
        user={user}
        onRoleChange={handleRoleChange}
        onLogout={handleLogout}
      />
    </WorkflowProvider>
  )
}

function WorkflowApplication({
  user,
  onRoleChange,
  onLogout,
}: {
  user: AuthUser
  onRoleChange: (role: UserRole) => void
  onLogout: () => void
}) {
  const {
    notifications,
    unreadNotificationCount,
    markNotificationRead,
    markAllNotificationsRead,
  } = useWorkflow()

  return (
    <AppShell user={user} unreadNotificationCount={unreadNotificationCount} onRoleChange={onRoleChange} onLogout={onLogout}>
      <Routes>
        <Route path="/dashboard" element={<DashboardPage user={user} />} />
        <Route path="/kayitlar" element={<RecordsPage role={user.role} />} />
        <Route path="/kayitlar/:recordId/duzenle" element={<RecordEditPage role={user.role} />} />
        <Route path="/kayitlar/:recordId" element={<RecordDetailPage role={user.role} />} />
        <Route
          path="/bildirimler"
          element={
            <NotificationsPage
              notifications={notifications}
              onMarkRead={markNotificationRead}
              onMarkAllRead={markAllNotificationsRead}
            />
          }
        />
        <Route path="/profil" element={<ProfilePage user={user} />} />
        <Route path="/403" element={<ErrorStatePage type="403" />} />
        <Route path="/404" element={<ErrorStatePage type="404" />} />
        <Route path="/admin/*" element={<Navigate to="/403" replace />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<ErrorStatePage type="404" />} />
      </Routes>
    </AppShell>
  )
}

function AdminApplication({
  user,
  onRoleChange,
  onLogout,
}: {
  user: AuthUser
  onRoleChange: (role: UserRole) => void
  onLogout: () => void
}) {
  return (
    <AppShell
      user={user}
      unreadNotificationCount={0}
      onRoleChange={onRoleChange}
      onLogout={onLogout}
    >
      <Routes>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/kullanicilar" element={<AdminUsersPage />} />
        <Route path="/admin/loglar" element={<AdminLogsPage />} />
        <Route path="/profil" element={<ProfilePage user={user} />} />
        <Route path="/403" element={<ErrorStatePage type="403" />} />
        <Route path="/404" element={<ErrorStatePage type="404" />} />
        <Route path="/dashboard" element={<Navigate to="/admin" replace />} />
        <Route path="/kayitlar/*" element={<Navigate to="/403" replace />} />
        <Route path="/bildirimler" element={<Navigate to="/403" replace />} />
        <Route path="/" element={<Navigate to="/admin" replace />} />
        <Route path="*" element={<ErrorStatePage type="404" />} />
      </Routes>
    </AppShell>
  )
}

export default App
