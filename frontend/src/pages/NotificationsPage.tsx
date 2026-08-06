import { Bell, BellRing, Check, CheckCheck, ChevronRight, Inbox } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { NotificationItem } from '../types/notification'

type NotificationsPageProps = {
  notifications: NotificationItem[]
  onMarkRead: (notificationId: string) => void
  onMarkAllRead: () => void
}

const dateFormatter = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function NotificationsPage({
  notifications,
  onMarkRead,
  onMarkAllRead,
}: NotificationsPageProps) {
  const [searchParams, setSearchParams] = useSearchParams()
  const unreadOnly = searchParams.get('gorunum') === 'okunmamis'
  const unreadCount = notifications.reduce((count, notification) => count + Number(!notification.isRead), 0)
  const visibleNotifications = unreadOnly
    ? notifications.filter((notification) => !notification.isRead)
    : notifications

  const setView = (nextUnreadOnly: boolean) => {
    if (nextUnreadOnly) setSearchParams({ gorunum: 'okunmamis' })
    else setSearchParams({})
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-brand-600">Bildirimler</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Bildirimleriniz</h1>
          <p className="mt-2 text-sm text-slate-500">
            Kayıtlarınızdaki durum değişikliklerini ve bekleyen işlemleri buradan takip edin.
          </p>
        </div>
        <button
          type="button"
          onClick={onMarkAllRead}
          disabled={unreadCount === 0}
          className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-bold text-slate-700 shadow-sm transition hover:border-brand-200 hover:text-brand-700 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        >
          <CheckCheck className="size-[18px]" aria-hidden="true" />
          Tümünü okundu yap
        </button>
      </header>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
              <BellRing className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h2 className="font-bold text-slate-950">Bildirim merkezi</h2>
              <p className="mt-0.5 text-xs text-slate-500">
                {unreadCount > 0 ? `${unreadCount} okunmamış bildiriminiz var` : 'Tüm bildirimleri okudunuz'}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 rounded-xl bg-slate-100 p-1" role="group" aria-label="Bildirim görünümü">
            <FilterButton active={!unreadOnly} onClick={() => setView(false)}>
              Tümü <span className="text-[11px] opacity-70">{notifications.length}</span>
            </FilterButton>
            <FilterButton active={unreadOnly} onClick={() => setView(true)}>
              Okunmamış <span className="text-[11px] opacity-70">{unreadCount}</span>
            </FilterButton>
          </div>
        </div>

        {visibleNotifications.length > 0 ? (
          <ul className="divide-y divide-slate-100" aria-label="Bildirim listesi">
            {visibleNotifications.map((notification) => (
              <NotificationRow
                key={notification.id}
                notification={notification}
                onMarkRead={onMarkRead}
              />
            ))}
          </ul>
        ) : (
          <div className="flex min-h-80 flex-col items-center justify-center p-6 text-center">
            <span className="flex size-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
              <Inbox className="size-6" aria-hidden="true" />
            </span>
            <h2 className="mt-4 font-bold text-slate-950">Okunmamış bildiriminiz yok</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-slate-500">
              Yeni bir kayıt hareketi olduğunda bildirimleriniz burada görüntülenecek.
            </p>
            <button
              type="button"
              onClick={() => setView(false)}
              className="mt-5 min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            >
              Tüm bildirimleri göster
            </button>
          </div>
        )}
      </section>
    </div>
  )
}

function NotificationRow({
  notification,
  onMarkRead,
}: {
  notification: NotificationItem
  onMarkRead: (notificationId: string) => void
}) {
  return (
    <li className={`relative p-4 transition hover:bg-slate-50/80 sm:px-5 ${notification.isRead ? 'bg-white' : 'bg-brand-50/35'}`}>
      {!notification.isRead ? (
        <span className="absolute inset-y-0 left-0 w-1 bg-brand-500" aria-hidden="true" />
      ) : null}
      <div className="flex items-start gap-3 sm:gap-4">
        <span className={`mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl ${
          notification.isRead ? 'bg-slate-100 text-slate-500' : 'bg-brand-100 text-brand-700'
        }`}>
          <Bell className="size-[18px]" aria-hidden="true" />
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-start gap-3">
            <p className={`min-w-0 flex-1 text-sm leading-6 ${notification.isRead ? 'font-medium text-slate-700' : 'font-bold text-slate-950'}`}>
              {notification.message}
            </p>
            {!notification.isRead ? (
              <span className="mt-2 size-2 shrink-0 rounded-full bg-brand-500" aria-label="Okunmamış" />
            ) : null}
          </div>
          <time className="mt-1 block text-xs font-medium text-slate-500" dateTime={notification.createdAt}>
            {dateFormatter.format(new Date(notification.createdAt))}
          </time>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Link
              to={`/kayitlar/${notification.recordId}`}
              onClick={() => onMarkRead(notification.id)}
              className="inline-flex min-h-9 items-center gap-1 rounded-lg bg-brand-50 px-3 text-xs font-bold text-brand-700 transition hover:bg-brand-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            >
              İlgili kaydı görüntüle
              <ChevronRight className="size-3.5" aria-hidden="true" />
            </Link>
            {!notification.isRead ? (
              <button
                type="button"
                onClick={() => onMarkRead(notification.id)}
                className="inline-flex min-h-9 items-center gap-1.5 rounded-lg px-3 text-xs font-bold text-slate-600 transition hover:bg-white hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                <Check className="size-3.5" aria-hidden="true" />
                Okundu yap
              </button>
            ) : null}
          </div>
        </div>
      </div>
    </li>
  )
}

function FilterButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`flex min-h-9 items-center justify-center gap-1.5 rounded-lg px-3 text-xs font-bold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 ${
        active ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-900'
      }`}
    >
      {children}
    </button>
  )
}
