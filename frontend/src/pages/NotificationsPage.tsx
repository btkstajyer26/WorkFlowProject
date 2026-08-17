import { Bell, BellRing, Check, ChevronRight, Inbox } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { apiMode } from '../api/config'
import { useNotificationCenter, type NotificationViewItem } from '../hooks/useNotificationCenter'
import type { NotificationItem } from '../types/notification'

type NotificationsPageProps = {
  notifications: NotificationItem[]
  onMarkRead: (notificationId: string) => void
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
}: NotificationsPageProps) {
  const [searchParams, setSearchParams] = useSearchParams()
  const unreadOnly = searchParams.get('gorunum') === 'okunmamis'
  const backendMode = apiMode === 'backend'
  const notificationCenter = useNotificationCenter({ enabled: backendMode, unreadOnly })
  const mockUnreadCount = notifications.reduce(
    (count, notification) => count + Number(!notification.isRead),
    0,
  )
  const mockVisibleNotifications = unreadOnly
    ? notifications.filter((notification) => !notification.isRead)
    : notifications
  const visibleNotifications: NotificationViewItem[] = backendMode
    ? notificationCenter.notifications
    : mockVisibleNotifications
  const unreadCount = backendMode ? notificationCenter.unreadCount : mockUnreadCount
  const totalCount = backendMode ? notificationCenter.totalCount : notifications.length
  const markRead = backendMode ? notificationCenter.markRead : onMarkRead

  const setView = (nextUnreadOnly: boolean) => {
    if (nextUnreadOnly) setSearchParams({ gorunum: 'okunmamis' })
    else setSearchParams({})
  }

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Bildirimleriniz</h1>
        <p className="mt-2 text-sm text-app-text-subtle">
          Kayıtlarınızdaki durum değişikliklerini ve bekleyen işlemleri buradan takip edin.
        </p>
      </header>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        <div className="flex flex-col gap-4 border-b border-app-border p-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
              <BellRing className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h2 className="font-bold text-app-text">Bildirim merkezi</h2>
              <p className="mt-0.5 text-xs text-app-text-subtle">
                {unreadCount > 0 ? `${unreadCount} okunmamış bildiriminiz var` : 'Tüm bildirimleri okudunuz'}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 rounded-xl bg-app-surface-strong p-1" role="group" aria-label="Bildirim görünümü">
            <FilterButton active={!unreadOnly} onClick={() => setView(false)}>
              Tümü <span className="text-[11px] opacity-70">{totalCount}</span>
            </FilterButton>
            <FilterButton active={unreadOnly} onClick={() => setView(true)}>
              Okunmamış <span className="text-[11px] opacity-70">{unreadCount}</span>
            </FilterButton>
          </div>
        </div>

        {backendMode && notificationCenter.isPending ? (
          <div className="flex min-h-80 items-center justify-center p-6 text-center text-sm font-semibold text-app-text-muted" role="status">
            Bildirimler yükleniyor…
          </div>
        ) : backendMode && notificationCenter.isError ? (
          <div className="flex min-h-80 flex-col items-center justify-center p-6 text-center" role="alert">
            <h2 className="font-bold text-app-text">Bildirimler yüklenemedi</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-app-text-subtle">
              {notificationCenter.error instanceof Error ? notificationCenter.error.message : 'Beklenmeyen bir hata oluştu.'}
            </p>
            <button
              type="button"
              onClick={() => void notificationCenter.retry()}
              className="mt-5 min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted"
            >
              Tekrar dene
            </button>
          </div>
        ) : visibleNotifications.length > 0 ? (
          <>
            <ul className="divide-y divide-app-border-subtle" aria-label="Bildirim listesi">
              {visibleNotifications.map((notification) => (
                <NotificationRow
                  key={notification.id}
                  notification={notification}
                  onMarkRead={markRead}
                  marking={backendMode && notificationCenter.markingId === notification.id}
                />
              ))}
            </ul>
            {backendMode && notificationCenter.hasNextPage ? (
              <div className="border-t border-app-border-subtle p-4 text-center">
                <button
                  type="button"
                  disabled={notificationCenter.isFetchingNextPage}
                  onClick={() => void notificationCenter.fetchNextPage()}
                  className="min-h-10 rounded-xl border border-app-border px-4 text-xs font-bold text-app-text-secondary transition hover:bg-app-surface-muted disabled:cursor-wait disabled:opacity-60"
                >
                  {notificationCenter.isFetchingNextPage ? 'Yükleniyor…' : 'Daha fazla göster'}
                </button>
              </div>
            ) : null}
          </>
        ) : (
          <div className="flex min-h-80 flex-col items-center justify-center p-6 text-center">
            <span className="flex size-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400">
              <Inbox className="size-6" aria-hidden="true" />
            </span>
            <h2 className="mt-4 font-bold text-app-text">Okunmamış bildiriminiz yok</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-app-text-subtle">
              Yeni bir kayıt hareketi olduğunda bildirimleriniz burada görüntülenecek.
            </p>
            {unreadOnly ? (
              <button
                type="button"
                onClick={() => setView(false)}
                className="mt-5 min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                Tüm bildirimleri göster
              </button>
            ) : null}
          </div>
        )}
        {backendMode && notificationCenter.markReadError ? (
          <p className="border-t border-rose-200 bg-rose-50 px-5 py-3 text-xs font-semibold text-rose-800 dark:border-rose-900/70 dark:bg-rose-950/40 dark:text-rose-200" role="alert">
            Bildirim okundu olarak işaretlenemedi. Lütfen tekrar deneyin.
          </p>
        ) : null}
      </section>
    </div>
  )
}

function NotificationRow({
  notification,
  onMarkRead,
  marking,
}: {
  notification: NotificationViewItem
  onMarkRead: (notificationId: string) => void
  marking: boolean
}) {
  return (
    <li className={`relative p-4 transition hover:bg-app-surface-muted/80 sm:px-5 ${notification.isRead ? 'bg-app-surface' : 'bg-brand-50/35 dark:bg-brand-900/20'}`}>
      {!notification.isRead ? (
        <span className="absolute inset-y-0 left-0 w-1 bg-brand-500" aria-hidden="true" />
      ) : null}
      <div className="flex items-start gap-3 sm:gap-4">
        <span className={`mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl ${
          notification.isRead
            ? 'bg-app-surface-strong text-app-text-subtle'
            : 'bg-brand-100 text-brand-700 dark:bg-brand-900/45 dark:text-brand-300'
        }`}>
          <Bell className="size-[18px]" aria-hidden="true" />
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-start gap-3">
            <p className={`min-w-0 flex-1 text-sm leading-6 ${notification.isRead ? 'font-medium text-app-text-secondary' : 'font-bold text-app-text'}`}>
              {notification.message}
            </p>
            {!notification.isRead ? (
              <span className="mt-2 size-2 shrink-0 rounded-full bg-brand-500" aria-label="Okunmamış" />
            ) : null}
          </div>
          <time className="mt-1 block text-xs font-medium text-app-text-subtle" dateTime={notification.createdAt}>
            {dateFormatter.format(new Date(notification.createdAt))}
          </time>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Link
              to={`/kayitlar/${notification.recordId}`}
              onClick={() => onMarkRead(notification.id)}
              className="inline-flex min-h-9 items-center gap-1 rounded-lg bg-brand-50 px-3 text-xs font-bold text-brand-700 transition hover:bg-brand-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:bg-brand-900/30 dark:text-brand-300 dark:hover:bg-brand-900/45"
            >
              İlgili kaydı görüntüle
              <ChevronRight className="size-3.5" aria-hidden="true" />
            </Link>
            {!notification.isRead ? (
              <button
                type="button"
                disabled={marking}
                onClick={() => onMarkRead(notification.id)}
                className="inline-flex min-h-9 items-center gap-1.5 rounded-lg px-3 text-xs font-bold text-app-text-muted transition hover:bg-app-surface hover:text-app-text focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-wait disabled:opacity-60"
              >
                <Check className="size-3.5" aria-hidden="true" />
                {marking ? 'İşleniyor…' : 'Okundu yap'}
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
        active
          ? 'bg-app-surface text-brand-700 shadow-sm dark:text-brand-300'
          : 'text-app-text-subtle hover:text-app-text-strong'
      }`}
    >
      {children}
    </button>
  )
}
