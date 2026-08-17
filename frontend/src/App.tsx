import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router'
import { AppShell } from './components/layout/AppShell'
import { AppErrorBoundary } from './components/errors/AppErrorBoundary'
import { WorkflowProvider } from './context/WorkflowContext'
import { useWorkflow } from './context/workflowState'
import { AdminProvider } from './context/AdminContext'
import { AdminDataBoundary } from './components/admin/AdminDataBoundary'
import { ThemeProvider } from './context/ThemeContext'
import { ToastProvider } from './context/ToastContext'
import {
  clearAuthSession,
  endAuthSession,
  restoreAuthSession,
  startAuthSession,
  subscribeAuthSessionExpired,
} from './auth/authSession'
import { apiMode, isApiMockEnabled } from './api/config'
import { CategoryProvider } from './context/CategoryContext'
import { demoAccounts } from './mocks/users'
import { DashboardPage } from './pages/DashboardPage'
import { ErrorStatePage } from './pages/ErrorStatePage'
import { LoginPage } from './pages/LoginPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { ProfilePage } from './pages/ProfilePage'
import { PasswordChangePage } from './pages/PasswordChangePage'
import { RecordDetailPage } from './pages/RecordDetailPage'
import { RecordEditPage } from './pages/RecordEditPage'
import { RecordsPage } from './pages/RecordsPage'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage'
import { AdminLogsPage } from './pages/admin/AdminLogsPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
import type { AuthUser, UserRole } from './types/auth'
import { AppQueryProvider } from './query/queryClient'
import { useUnreadNotificationCount } from './hooks/useNotificationCenter'

async function startDemoAuthSession(role: UserRole) {
  const account = demoAccounts.find((candidate) => candidate.role === role)
  if (!account) throw new Error(`${role} rolü için demo hesabı bulunamadı.`)
  return startAuthSession(account.email, account.password)
}

function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const [user, setUser] = useState<AuthUser | null>(null)
  const [authReady, setAuthReady] = useState(false)
  const [sessionEndPath, setSessionEndPath] = useState<string | null>(null)

  useEffect(() => subscribeAuthSessionExpired(() => {
    setUser(null)
    setSessionEndPath('/giris?reason=expired')
  }), [])

  useEffect(() => {
    let active = true

    void restoreAuthSession().then((restoredUser) => {
      if (!active) return
      setUser(restoredUser)
      setAuthReady(true)
    })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!sessionEndPath) return
    const currentPath = `${location.pathname}${location.search}`
    if (currentPath === sessionEndPath) {
      setSessionEndPath(null)
      return
    }
    navigate(sessionEndPath, { replace: true })
  }, [location.pathname, location.search, navigate, sessionEndPath])

  const handleLogin = (authenticatedUser: AuthUser) => {
    setSessionEndPath(null)
    setUser(authenticatedUser)
  }

  const endSessionAt = (path: string) => {
    clearAuthSession()
    setUser(null)
    setSessionEndPath(path)
  }

  return (
    <AppQueryProvider key={`${user?.id ?? 'anonymous'}:${user?.role ?? 'none'}`}>
      <ThemeProvider>
        <ToastProvider>
          <AppErrorBoundary>
            {authReady ? <Routes>
            <Route
              path="/giris"
              element={(
                <LoginPage
                  user={user}
                  onLogin={handleLogin}
                />
              )}
            />
            <Route
              path="/sifre-degistir"
              element={(
                <PasswordChangePage
                  user={user}
                  onPasswordChanged={() => endSessionAt('/giris?reason=password-changed')}
                  onUseAnotherAccount={() => endSessionAt('/giris')}
                />
              )}
            />
            <Route
              path="/*"
              element={user?.mustChangePassword
                ? <Navigate to="/sifre-degistir" replace />
                : (
                  <ProtectedApplication
                    user={user}
                    onUserChange={(nextUser) => {
                      setUser(nextUser)
                    }}
                  />
                )}
            />
            </Routes> : <AuthBootstrapScreen />}
          </AppErrorBoundary>
        </ToastProvider>
      </ThemeProvider>
    </AppQueryProvider>
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

  const handleRoleChange = async (nextRole: UserRole) => {
    if (!isApiMockEnabled || nextRole === user.role) return

    try {
      const session = await startDemoAuthSession(nextRole)
      onUserChange(session.user)
      navigate(session.user.role === 'ADMIN' ? '/admin' : '/dashboard')
    } catch {
      return
    }
  }

  const handleLogout = () => {
    void endAuthSession().catch(() => undefined)
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
    <CategoryProvider>
      <WorkflowProvider user={user}>
        <WorkflowApplication
          user={user}
          onRoleChange={handleRoleChange}
          onLogout={handleLogout}
        />
      </WorkflowProvider>
    </CategoryProvider>
  )
}

function AuthBootstrapScreen() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-app-canvas px-6 text-center" role="status">
      <p className="text-sm font-semibold text-app-text-muted">Oturum kontrol ediliyor…</p>
    </main>
  )
}

function RecordDeepLinkRedirect() {
  const { recordId } = useParams()
  return <Navigate to={recordId ? `/kayitlar/${recordId}` : '/kayitlar'} replace />
}

function WorkflowApplication({
  user,
  onRoleChange,
  onLogout,
}: {
  user: AuthUser
  onRoleChange: (role: UserRole) => void | Promise<void>
  onLogout: () => void
}) {
  const {
    notifications,
    unreadNotificationCount: mockUnreadNotificationCount,
    markNotificationRead,
  } = useWorkflow()
  const backendMode = apiMode === 'backend'
  const unreadNotificationCountQuery = useUnreadNotificationCount(backendMode)
  const unreadNotificationCount = backendMode
    ? unreadNotificationCountQuery.data ?? 0
    : mockUnreadNotificationCount

  return (
    <AppShell user={user} unreadNotificationCount={unreadNotificationCount} onRoleChange={onRoleChange} onLogout={onLogout}>
      <Routes>
        <Route path="/dashboard" element={<DashboardPage user={user} />} />
        <Route path="/kayitlar" element={<RecordsPage role={user.role} />} />
        <Route path="/kayitlar/:recordId/duzenle" element={<RecordEditPage role={user.role} />} />
        <Route path="/kayitlar/:recordId" element={<RecordDetailPage role={user.role} />} />
        <Route path="/records/:recordId" element={<RecordDeepLinkRedirect />} />
        <Route
          path="/bildirimler"
          element={
            <NotificationsPage
              notifications={notifications}
              onMarkRead={markNotificationRead}
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
  onRoleChange: (role: UserRole) => void | Promise<void>
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
        <Route path="/admin" element={<AdminDataBoundary><AdminDashboardPage /></AdminDataBoundary>} />
        <Route path="/admin/kullanicilar" element={<AdminDataBoundary><AdminUsersPage /></AdminDataBoundary>} />
        <Route path="/admin/loglar" element={<AdminDataBoundary><AdminLogsPage /></AdminDataBoundary>} />
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
