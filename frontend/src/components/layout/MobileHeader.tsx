import { Bell, Menu, UserRound } from 'lucide-react'
import { Link } from 'react-router'
import { Brand } from './Brand'

type MobileHeaderProps = {
  unreadNotificationCount: number
  showNotifications?: boolean
  onMenuOpen: () => void
}

export function MobileHeader({ unreadNotificationCount, showNotifications = true, onMenuOpen }: MobileHeaderProps) {
  return (
    <header className="sticky top-0 z-20 flex min-h-17 items-center justify-between border-b border-app-border/80 bg-app-surface/90 px-4 backdrop-blur-xl lg:hidden">
      <button
        type="button"
        onClick={onMenuOpen}
        className="flex size-11 items-center justify-center rounded-xl border border-app-border bg-app-surface text-app-text-secondary shadow-sm transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        aria-label="Menüyü aç"
      >
        <Menu className="size-5" aria-hidden="true" />
      </button>

      <div className="scale-90">
        <Brand />
      </div>

      <Link
        to={showNotifications ? '/bildirimler' : '/profil'}
        className="relative flex size-11 items-center justify-center rounded-xl border border-app-border bg-app-surface text-app-text-muted shadow-sm transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        aria-label={showNotifications ? 'Bildirimleri görüntüle' : 'Profili görüntüle'}
      >
        {showNotifications ? <Bell className="size-5" aria-hidden="true" /> : <UserRound className="size-5" aria-hidden="true" />}
        {showNotifications && unreadNotificationCount > 0 ? (
          <span className="absolute -right-1 -top-1 flex min-w-5 items-center justify-center rounded-full border-2 border-white bg-brand-600 px-1 py-0.5 text-[10px] font-bold text-white">
            {unreadNotificationCount}
          </span>
        ) : null}
      </Link>
    </header>
  )
}
