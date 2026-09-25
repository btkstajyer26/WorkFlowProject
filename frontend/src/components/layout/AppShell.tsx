import { useEffect, useRef, useState, type ReactNode } from 'react'
import { LogOut, X } from 'lucide-react'
import { NewRecordComposer } from '../records/NewRecordComposer'
import type { AuthUser } from '../../types/auth'
import { MobileHeader } from './MobileHeader'
import { Sidebar } from './Sidebar'
import { useModalDialog } from '../../hooks/useModalDialog'

type AppShellProps = {
  children: ReactNode
  user: AuthUser
  unreadNotificationCount: number
  onLogout: () => void
}

export function AppShell({
  children,
  user,
  unreadNotificationCount,
  onLogout,
}: AppShellProps) {
  const systemKey = user.systemKey
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
    if (systemKey !== 'CALISAN') setComposerOpen(false)
  }, [systemKey])

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
    <div className="min-h-screen bg-app-canvas">
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
          }}
          onLogout={() => {
            setMobileOpen(false)
            setLogoutOpen(true)
          }}
        />
        <div inert={mobileOpen}>
          <MobileHeader
            unreadNotificationCount={unreadNotificationCount}
            showNotifications={systemKey !== 'ADMIN'}
            onMenuOpen={() => setMobileOpen(true)}
          />

          <main className="min-h-screen lg:pl-72">
            <div className="mx-auto w-full max-w-[1536px] px-4 py-5 sm:px-6 sm:py-7 lg:px-8 lg:py-8">
              {children}
            </div>
          </main>
        </div>
        {systemKey === 'CALISAN' ? <NewRecordComposer
          open={composerOpen}
          requestId={newRecordRequestId}
          onClose={() => setComposerOpen(false)}
        /> : null}
      </div>
      {logoutOpen ? (
        <div className="fixed inset-0 z-[90] flex items-end justify-center bg-slate-950/35 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
          <section ref={logoutDialogRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="logout-dialog-title" className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6">
            <div className="flex items-start gap-3">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300">
                <LogOut className="size-5" aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1">
                <h2 id="logout-dialog-title" className="text-lg font-bold text-app-text">Çıkış yapmak istediğinize emin misiniz?</h2>
                <p className="mt-2 text-sm leading-6 text-app-text-muted">Mevcut oturumunuz sonlandırılacak ve giriş ekranına yönlendirileceksiniz.</p>
              </div>
              <button
                ref={logoutCloseButtonRef}
                type="button"
                onClick={() => setLogoutOpen(false)}
                className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
                aria-label="Çıkış penceresini kapat"
              >
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setLogoutOpen(false)}
                className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
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
