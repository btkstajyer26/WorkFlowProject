import { useEffect, useRef, useState, type RefObject } from 'react'
import {
  Bell,
  ChevronDown,
  ChevronRight,
  LogOut,
  Plus,
  UserRound,
  X,
} from 'lucide-react'
import { Link, NavLink, useLocation } from 'react-router'
import {
  adminNavigation,
  notificationNavigation,
  primaryNavigation,
  recordNavigation,
  recordSection,
  type RecordView,
} from '../../config/navigation'
import { UserAvatar } from '../users/UserAvatar'
import { roleLabels, type AuthUser } from '../../types/auth'
import { Brand } from './Brand'
import { ThemeToggle } from './ThemeToggle'

type SidebarContentProps = {
  user: AuthUser
  unreadNotificationCount: number
  onNavigate?: () => void
  onClose?: () => void
  showCloseButton?: boolean
  closeButtonRef?: RefObject<HTMLButtonElement | null>
  onNewRecord?: () => void
  onLogout: () => void
}

const baseNavigationClass =
  'flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500'

function getRecordLink(view: RecordView) {
  return view.view ? `/kayitlar?gorunum=${view.view}` : '/kayitlar'
}

function SidebarContent({
  user,
  unreadNotificationCount,
  onNavigate,
  onClose,
  showCloseButton = false,
  closeButtonRef,
  onNewRecord,
  onLogout,
}: SidebarContentProps) {
  const role = user.role
  const location = useLocation()
  const [recordsExpanded, setRecordsExpanded] = useState(true)
  const currentView = new URLSearchParams(location.search).get('gorunum')
  const recordItems = recordNavigation[role]
  const navigationItems = role === 'ADMIN' ? adminNavigation : primaryNavigation
  const RecordsIcon = recordSection.icon

  const isRecordViewActive = (view?: string) => {
    if (location.pathname !== '/kayitlar') return false
    return view ? currentView === view : currentView === null
  }

  return (
    <div className="flex h-full flex-col bg-app-surface">
      <div className="flex items-center justify-between px-5 pb-5 pt-6">
        <Brand />
        {showCloseButton ? (
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="flex size-10 items-center justify-center rounded-xl text-app-text-subtle transition-colors hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            aria-label="Menüyü kapat"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        ) : null}
      </div>

      {role === 'CALISAN' ? (
        <div className="px-4">
          <button
            type="button"
            onClick={() => {
              onNavigate?.()
              onNewRecord?.()
            }}
            className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Plus className="size-5" aria-hidden="true" />
            Yeni Kayıt
          </button>
        </div>
      ) : null}

      <nav className="mt-5 flex-1 overflow-y-auto px-4 pb-5" aria-label="Ana menü">
        <p className="mb-2 px-3 text-[11px] font-bold uppercase tracking-[0.16em] text-app-text-muted">
          Menü
        </p>

        <div className="space-y-1">
          {navigationItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/admin'}
                onClick={onNavigate}
                className={({ isActive }) =>
                  `${baseNavigationClass} ${
                    isActive
                      ? 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300'
                      : 'text-app-text-muted hover:bg-app-surface-muted hover:text-app-text-strong'
                  }`
                }
              >
                <Icon className="size-[19px]" aria-hidden="true" />
                <span>{item.label}</span>
              </NavLink>
            )
          })}

          {role !== 'ADMIN' ? <div>
            <div className="flex items-center gap-1">
              <Link
                to="/kayitlar"
                onClick={onNavigate}
                className={`${baseNavigationClass} min-w-0 flex-1 ${
                  location.pathname.startsWith('/kayitlar')
                    ? 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300'
                    : 'text-app-text-muted hover:bg-app-surface-muted hover:text-app-text-strong'
                }`}
              >
                <RecordsIcon className="size-[19px]" aria-hidden="true" />
                <span>{recordSection.label}</span>
              </Link>
              <button
                type="button"
                onClick={() => setRecordsExpanded((expanded) => !expanded)}
                className="flex size-11 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition-colors hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                aria-label={recordsExpanded ? 'Kayıt menüsünü daralt' : 'Kayıt menüsünü genişlet'}
                aria-expanded={recordsExpanded}
              >
                <ChevronDown
                  className={`size-4 transition-transform ${recordsExpanded ? 'rotate-0' : '-rotate-90'}`}
                  aria-hidden="true"
                />
              </button>
            </div>

            {recordsExpanded ? (
              <div className="ml-[21px] mt-1 space-y-0.5 border-l border-app-border pl-4">
                {recordItems.map((item) => {
                  const isActive = isRecordViewActive(item.view)
                  return (
                    <Link
                      key={item.view ?? 'all'}
                      to={getRecordLink(item)}
                      onClick={onNavigate}
                      aria-current={isActive ? 'page' : undefined}
                      className={`group flex min-h-9 items-center gap-2 rounded-lg px-2 text-[13px] font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 ${
                        isActive
                          ? 'text-brand-700 dark:text-brand-300'
                          : 'text-app-text-subtle hover:bg-app-surface-muted hover:text-app-text-strong'
                      }`}
                    >
                      <ChevronRight
                        className={`size-3.5 ${isActive ? 'text-brand-500' : 'text-app-text-disabled group-hover:text-app-text-subtle'}`}
                        aria-hidden="true"
                      />
                      <span>{item.label}</span>
                    </Link>
                  )
                })}
              </div>
            ) : null}
          </div> : null}

          {role !== 'ADMIN' ? <NavLink
            to={notificationNavigation.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `${baseNavigationClass} ${
                isActive
                  ? 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300'
                  : 'text-app-text-muted hover:bg-app-surface-muted hover:text-app-text-strong'
              }`
            }
          >
            <Bell className="size-[19px]" aria-hidden="true" />
            <span className="flex-1">{notificationNavigation.label}</span>
            {unreadNotificationCount > 0 ? (
              <span className="flex min-w-6 items-center justify-center rounded-full bg-brand-100 dark:bg-brand-900/45 px-1.5 py-0.5 text-[11px] font-bold text-brand-700 dark:text-brand-300">
                {unreadNotificationCount}
              </span>
            ) : null}
          </NavLink> : null}
        </div>
      </nav>

      <div className="border-t border-app-border-subtle p-4">
        <ThemeToggle />
        <div className="mt-3 rounded-2xl bg-app-surface-muted p-3">
          <div className="flex items-center gap-3">
            <UserAvatar user={user} className="size-10 rounded-full text-xs" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-app-text-strong">{user.firstName} {user.lastName}</p>
              <p className="truncate text-xs text-app-text-subtle">{roleLabels[role]}</p>
            </div>
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2 border-t border-app-border pt-3">
            <NavLink
              to="/profil"
              onClick={onNavigate}
              className={({ isActive }) => `flex items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-xs font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 ${
                isActive ? 'bg-app-surface text-brand-700 dark:text-brand-300 shadow-sm' : 'text-app-text-muted hover:bg-app-surface hover:text-app-text-strong'
              }`}
            >
              <UserRound className="size-4" aria-hidden="true" />
              Profil
            </NavLink>
            <button
              type="button"
              onClick={onLogout}
              className="flex items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-xs font-semibold text-app-text-muted transition hover:bg-app-surface hover:text-rose-600 dark:hover:text-rose-400 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            >
              <LogOut className="size-4" aria-hidden="true" />
              Çıkış
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

type SidebarProps = {
  user: AuthUser
  unreadNotificationCount: number
  mobileOpen: boolean
  onMobileClose: () => void
  onNewRecord: () => void
  onLogout: () => void
}

export function Sidebar({
  user,
  unreadNotificationCount,
  mobileOpen,
  onMobileClose,
  onNewRecord,
  onLogout,
}: SidebarProps) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (mobileOpen) closeButtonRef.current?.focus()
  }, [mobileOpen])

  return (
    <>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 border-r border-app-border lg:block">
        <SidebarContent
          user={user}
          unreadNotificationCount={unreadNotificationCount}
          onNewRecord={onNewRecord}
          onLogout={onLogout}
        />
      </aside>

      <div
        className={`fixed inset-0 z-50 transition-[visibility] duration-300 lg:hidden ${
          mobileOpen ? 'visible' : 'invisible delay-300'
        }`}
        aria-hidden={!mobileOpen}
      >
        <button
          type="button"
          aria-label="Menüyü kapat"
          onClick={onMobileClose}
          className={`absolute inset-0 bg-slate-950/30 backdrop-blur-[2px] transition-opacity duration-300 ${
            mobileOpen ? 'opacity-100' : 'opacity-0'
          }`}
        />
        <aside
          className={`absolute inset-y-0 left-0 w-[min(88vw,20rem)] border-r border-app-border bg-app-surface shadow-2xl transition-transform duration-300 ease-out ${
            mobileOpen ? 'translate-x-0' : '-translate-x-full'
          }`}
          role="dialog"
          aria-modal="true"
          aria-label="Mobil menü"
        >
          <SidebarContent
            user={user}
            unreadNotificationCount={unreadNotificationCount}
            onNavigate={onMobileClose}
            onClose={onMobileClose}
            showCloseButton
            closeButtonRef={closeButtonRef}
            onNewRecord={onNewRecord}
            onLogout={onLogout}
          />
        </aside>
      </div>
    </>
  )
}
