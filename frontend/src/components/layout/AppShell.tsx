import { useEffect, useRef, useState, type ReactNode } from 'react'
import { LogOut, X } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router'
import { NewRecordComposer } from '../records/NewRecordComposer'
import type { AuthUser, UserRole } from '../../types/auth'
import { MobileHeader } from './MobileHeader'
import { Sidebar } from './Sidebar'
import { useModalDialog } from '../../hooks/useModalDialog'

type AppShellProps = {
  children: ReactNode
  user: AuthUser
  unreadNotificationCount: number
  onRoleChange: (role: UserRole) => void
  onLogout: () => void
}

const previewRoles: { label: string; value: UserRole }[] = [
  { label: 'Çalışan', value: 'CALISAN' },
  { label: 'Bşk. Yrd.', value: 'BASKAN_YARDIMCISI' },
  { label: 'Başkan', value: 'BASKAN' },
  { label: 'Admin', value: 'ADMIN' },
]

export function AppShell({
  children,
  user,
  unreadNotificationCount,
  onRoleChange,
  onLogout,
}: AppShellProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const role = user.role
  const [mobileOpen, setMobileOpen] = useState(false)
  const [composerOpen, setComposerOpen] = useState(false)
  const [newRecordRequestId, setNewRecordRequestId] = useState(0)
  const [logoutOpen, setLogoutOpen] = useState(false)
  const logoutDialogRef = useRef<HTMLElement>(null)
  const logoutCloseButtonRef = useRef<HTMLButtonElement>(null)
  useModalDialog({
    open: logoutOpen,
    onClose: () => setLogoutOpen(false),
    dialogRef: logoutDialogRef,
    initialFocusRef: logoutCloseButtonRef,
  })

  useEffect(() => {
    if (role !== 'CALISAN') setComposerOpen(false)
  }, [role])

  useEffect(() => {
    if (role === 'CALISAN' && location.pathname === '/kayitlar/yeni') setComposerOpen(true)
  }, [location.pathname, role])

  useEffect(() => {
    const desktopQuery = window.matchMedia('(min-width: 1024px)')
    const closeMobileMenuAtDesktop = (event: MediaQueryListEvent | MediaQueryList) => {
      if (event.matches) setMobileOpen(false)
    }

    closeMobileMenuAtDesktop(desktopQuery)
    desktopQuery.addEventListener('change', closeMobileMenuAtDesktop)
    return () => desktopQuery.removeEventListener('change', closeMobileMenuAtDesktop)
  }, [])

  useEffect(() => {
    if (!mobileOpen) return

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMobileOpen(false)
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleEscape)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleEscape)
    }
  }, [mobileOpen])

  return (
    <div className="min-h-screen bg-[#f7f8fc]">
      <div inert={logoutOpen}>
        <Sidebar
          user={user}
          unreadNotificationCount={unreadNotificationCount}
          mobileOpen={mobileOpen}
          onMobileClose={() => setMobileOpen(false)}
          onNewRecord={() => {
            setMobileOpen(false)
            setComposerOpen(true)
            setNewRecordRequestId((current) => current + 1)
            navigate('/kayitlar/yeni')
          }}
          onLogout={() => {
            setMobileOpen(false)
            setLogoutOpen(true)
          }}
        />
        <div inert={mobileOpen}>
          <MobileHeader
            unreadNotificationCount={unreadNotificationCount}
            showNotifications={role !== 'ADMIN'}
            onMenuOpen={() => setMobileOpen(true)}
          />

          <main className="min-h-screen lg:pl-72">
            <div className="mx-auto w-full max-w-[1536px] px-4 py-5 sm:px-6 sm:py-7 lg:px-8 lg:py-8">
              {import.meta.env.DEV ? <RolePreview role={role} onRoleChange={onRoleChange} /> : null}
              {children}
            </div>
          </main>
        </div>
        {role === 'CALISAN' ? <NewRecordComposer
          open={composerOpen}
          requestId={newRecordRequestId}
          onClose={() => {
            setComposerOpen(false)
            if (location.pathname === '/kayitlar/yeni') navigate('/kayitlar')
          }}
        /> : null}
      </div>
      {logoutOpen ? (
        <div className="fixed inset-0 z-[90] flex items-end justify-center bg-slate-950/35 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
          <section ref={logoutDialogRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="logout-dialog-title" className="w-full rounded-t-3xl bg-white p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6">
            <div className="flex items-start gap-3">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-rose-50 text-rose-700">
                <LogOut className="size-5" aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1">
                <h2 id="logout-dialog-title" className="text-lg font-bold text-slate-950">Çıkış yapmak istediğinize emin misiniz?</h2>
                <p className="mt-2 text-sm leading-6 text-slate-600">Mevcut oturumunuz sonlandırılacak ve giriş ekranına yönlendirileceksiniz.</p>
              </div>
              <button
                ref={logoutCloseButtonRef}
                type="button"
                onClick={() => setLogoutOpen(false)}
                className="flex size-10 shrink-0 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-brand-500"
                aria-label="Çıkış penceresini kapat"
              >
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setLogoutOpen(false)}
                className="min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                Vazgeç
              </button>
              <button
                type="button"
                onClick={() => {
                  setLogoutOpen(false)
                  onLogout()
                }}
                className="min-h-11 rounded-xl bg-rose-600 px-4 text-sm font-bold text-white transition hover:bg-rose-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
              >
                Çıkış Yap
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}

function RolePreview({
  role,
  onRoleChange,
}: {
  role: UserRole
  onRoleChange: (role: UserRole) => void
}) {
  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-100 bg-brand-50/70 p-2.5 pl-4">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.12em] text-brand-700">Arayüz önizleme</p>
        <p className="mt-0.5 text-xs text-slate-600">Gerçek uygulamada rol, oturum bilgisinden gelecek.</p>
      </div>
      <div
        className="flex rounded-xl bg-white p-1 shadow-sm ring-1 ring-slate-200"
        role="group"
        aria-label="Önizlenecek rol"
      >
        {previewRoles.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => onRoleChange(item.value)}
            aria-pressed={role === item.value}
            className={`rounded-lg px-3 py-2 text-xs font-bold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 ${
              role === item.value
                ? 'bg-brand-600 text-white shadow-sm'
                : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>
    </div>
  )
}
