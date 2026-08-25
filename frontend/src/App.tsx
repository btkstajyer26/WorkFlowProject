import { lazy, Suspense, useEffect, useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router'
import { AppShell } from './components/layout/AppShell'
import { AppErrorBoundary } from './components/errors/AppErrorBoundary'
import { AdminProvider } from './context/AdminContext'
import { ThemeProvider } from './context/ThemeContext'
import { ToastProvider } from './context/ToastContext'
import {
  clearAuthSession,
  endAuthSession,
  restoreAuthSession,
  subscribeAuthSessionExpired,
} from './auth/authSession'
import { CategoryProvider } from './context/CategoryContext'
import type { AuthUser } from './types/auth'
import { AppQueryProvider } from './query/queryClient'
import { useUnreadNotificationCount } from './hooks/useNotificationCenter'
import { RoutePageSkeleton } from './components/feedback/LoadingSkeleton'

const DashboardPage = lazy(() => import('./pages/DashboardPage').then((module) => ({ default: module.DashboardPage })))
const ErrorStatePage = lazy(() => import('./pages/ErrorStatePage').then((module) => ({ default: module.ErrorStatePage })))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage').then((module) => ({ default: module.ForgotPasswordPage })))
const LoginPage = lazy(() => import('./pages/LoginPage').then((module) => ({ default: module.LoginPage })))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage').then((module) => ({ default: module.NotificationsPage })))
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((module) => ({ default: module.ProfilePage })))
const PasswordChangePage = lazy(() => import('./pages/PasswordChangePage').then((module) => ({ default: module.PasswordChangePage })))
const QuickActionPage = lazy(() => import('./pages/QuickActionPage').then((module) => ({ default: module.QuickActionPage })))
const RecordDetailPage = lazy(() => import('./pages/RecordDetailPage').then((module) => ({ default: module.RecordDetailPage })))
const RecordEditPage = lazy(() => import('./pages/RecordEditPage').then((module) => ({ default: module.RecordEditPage })))
const RecordsPage = lazy(() => import('./pages/RecordsPage').then((module) => ({ default: module.RecordsPage })))
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })))
const AdminLogsPage = lazy(() => import('./pages/admin/AdminLogsPage').then((module) => ({ default: module.AdminLogsPage })))
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })))

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

  const handleLogout = () => {
    void endAuthSession().catch(() => undefined)
    endSessionAt('/giris')
  }

  return (
    <AppQueryProvider key={`${user?.id ?? 'anonymous'}:${user?.role ?? 'none'}`}>
      <ThemeProvider>
        <ToastProvider>
          <AppErrorBoundary>
            {authReady ? <Suspense fallback={<RoutePageSkeleton label="Oturum sayfası yükleniyor" />}><Routes>
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
              path="/sifre-sifirla"
              element={<ForgotPasswordPage user={user} />}
            />
            {/*
              E-postadaki "Hızlı İşlem" bağlantısı buraya düşer. Kimlik
              doğrulaması aranmaz: kullanıcı postadan gelir ve kimliği,
              sayfanın adres parçasından okuyup gövdede backend'e taşıdığı tek
              kullanımlık anahtardan gelir. Oturumu açık bir kullanıcı da aynı
              sayfayı görür; anahtar zaten kime ait olduğunu taşır.
            */}
            <Route
              path="/hizli-islem"
              element={<QuickActionPage />}
            />
            <Route
              path="/sifre-degistir"
              element={(
                <PasswordChangePage
                  user={user}
                  onPasswordChanged={() => endSessionAt('/giris?reason=password-changed')}
                  onPasswordReset={() => endSessionAt('/giris?reason=password-reset')}
                  onUseAnotherAccount={() => endSessionAt('/giris')}
                />
              )}
            />
            <Route
              path="/*"
              element={sessionEndPath
                ? <Navigate to={sessionEndPath} replace />
                : user?.mustChangePassword
                ? <Navigate to="/sifre-degistir" replace />
                : (
                  <ProtectedApplication
                    user={user}
                    onLogout={handleLogout}
                  />
                )}
            />
            </Routes></Suspense> : <AuthBootstrapScreen />}
          </AppErrorBoundary>
        </ToastProvider>
      </ThemeProvider>
    </AppQueryProvider>
  )
}

function ProtectedApplication({
  user,
  onLogout,
}: {
  user: AuthUser | null
  onLogout: () => void
}) {
  const location = useLocation()

  if (!user) {
    const returnTo = `${location.pathname}${location.search}${location.hash}`
    return <Navigate to={`/giris?returnTo=${encodeURIComponent(returnTo)}`} replace />
  }

  if (user.role === 'ADMIN') {
    return (
      <AdminProvider actor={user}>
        <AdminApplication
          user={user}
          onLogout={onLogout}
        />
      </AdminProvider>
    )
  }

  return (
    <CategoryProvider>
      <WorkflowApplication user={user} onLogout={onLogout} />
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
  onLogout,
}: {
  user: AuthUser
  onLogout: () => void
}) {
  const unreadNotificationCountQuery = useUnreadNotificationCount(true)
  const unreadNotificationCount = unreadNotificationCountQuery.data ?? 0

  return (
    <AppShell user={user} unreadNotificationCount={unreadNotificationCount} onLogout={onLogout}>
      <Suspense fallback={<RoutePageSkeleton />}><Routes>
        <Route path="/dashboard" element={<DashboardPage user={user} />} />
        <Route path="/kayitlar" element={<RecordsPage role={user.role} />} />
        <Route path="/kayitlar/:recordId/duzenle" element={<RecordEditPage role={user.role} />} />
        <Route path="/kayitlar/:recordId" element={<RecordDetailPage user={user} />} />
        <Route path="/records/:recordId" element={<RecordDeepLinkRedirect />} />
        <Route
          path="/bildirimler"
          element={<NotificationsPage />}
        />
        <Route path="/profil" element={<ProfilePage user={user} />} />
        <Route path="/403" element={<ErrorStatePage type="403" />} />
        <Route path="/404" element={<ErrorStatePage type="404" />} />
        <Route path="/admin/*" element={<Navigate to="/403" replace />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<ErrorStatePage type="404" />} />
      </Routes></Suspense>
    </AppShell>
  )
}

function AdminApplication({
  user,
  onLogout,
}: {
  user: AuthUser
  onLogout: () => void
}) {
  return (
    <AppShell
      user={user}
      unreadNotificationCount={0}
      onLogout={onLogout}
    >
      <Suspense fallback={<RoutePageSkeleton />}><Routes>
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
      </Routes></Suspense>
    </AppShell>
  )
}

export default App
