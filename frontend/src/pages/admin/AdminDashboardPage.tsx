import { ClipboardCheck, FileClock, UserCheck, UsersRound } from 'lucide-react'
import { Link } from 'react-router'
import { useAdmin } from '../../context/adminState'
import { roleLabels } from '../../types/auth'

export function AdminDashboardPage() {
  const { users, logs, registrationRequests } = useAdmin()
  const activeUsers = users.filter((user) => user.isActive)
  const cards = [
    { label: 'Toplam kullanıcı', value: users.length, icon: UsersRound, tone: 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300' },
    { label: 'Onay bekleyen', value: registrationRequests.length, icon: ClipboardCheck, tone: 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300' },
    { label: 'Aktif hesap', value: activeUsers.length, icon: UserCheck, tone: 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300' },
    { label: 'Denetim kaydı', value: logs.length, icon: FileClock, tone: 'bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300' },
  ]

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-brand-600 dark:text-brand-400">Sistem Yönetimi</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Yönetim Özeti</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Kayıt taleplerini, kullanıcı durumlarını ve son denetim hareketlerini takip edin.</p>
        </div>
        <Link to="/admin/kullanicilar" className="flex min-h-11 items-center justify-center rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
          Kayıt Taleplerini Yönet
        </Link>
      </header>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="Yönetim özeti">
        {cards.map((card) => {
          const Icon = card.icon
          return (
            <article key={card.label} className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm sm:p-5">
              <span className={`flex size-10 items-center justify-center rounded-xl ${card.tone}`}>
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <p className="mt-4 text-xs font-semibold text-app-text-subtle sm:text-sm">{card.label}</p>
              <p className="mt-1 text-2xl font-bold text-app-text sm:text-3xl">{card.value}</p>
            </article>
          )
        })}
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <div className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
          <div className="flex items-center justify-between border-b border-app-border-subtle px-4 py-4 sm:px-6">
            <div>
              <h2 className="font-bold text-app-text">Son işlemler</h2>
              <p className="mt-0.5 text-xs text-app-text-subtle">Evrak ve kullanıcı hareketleri birlikte gösterilir.</p>
            </div>
            <Link to="/admin/loglar" className="text-xs font-bold text-brand-700 dark:text-brand-300 hover:text-brand-800 dark:hover:text-brand-200">Tümünü gör</Link>
          </div>
          <div className="divide-y divide-app-border-subtle">
            {logs.slice(0, 6).map((log) => (
              <div key={log.id} className="flex gap-3 px-4 py-4 sm:px-6">
                <span className={`mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl ${log.type === 'USER' ? 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300' : 'bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300'}`}>
                  {log.type === 'USER' ? <UsersRound className="size-4" aria-hidden="true" /> : <FileClock className="size-4" aria-hidden="true" />}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-bold text-app-text-strong">{log.actionLabel}</p>
                    <time className="text-[11px] text-app-text-subtle">{formatDateTime(log.createdAt)}</time>
                  </div>
                  <p className="mt-1 truncate text-xs text-app-text-muted">{log.target}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <aside className="space-y-5">
          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm">
            <h2 className="text-sm font-bold text-app-text">Rol dağılımı</h2>
            <div className="mt-3 space-y-2">
              {(['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'] as const).map((role) => (
                <div key={role} className="flex items-center justify-between gap-3 text-sm">
                  <span className="text-app-text-muted">{roleLabels[role]}</span>
                  <strong className="text-app-text">{users.filter((user) => user.isActive && user.role === role).length}</strong>
                </div>
              ))}
            </div>
          </section>
        </aside>
      </section>
    </div>
  )
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}
