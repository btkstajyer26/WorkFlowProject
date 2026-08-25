import { FileClock, ShieldCheck, UserCheck, UsersRound } from 'lucide-react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listAdminAuditLogs, listAdminUsers, type AdminUserListQuery } from '../../api/admin'
import { queryKeys } from '../../query/queryKeys'
import { roleLabels } from '../../types/auth'

const userStats: Array<{ key: string; filters: Pick<AdminUserListQuery, 'active' | 'role'> }> = [
  { key: 'total', filters: {} },
  { key: 'active', filters: { active: true } },
  { key: 'CALISAN', filters: { active: true, role: 'CALISAN' } },
  { key: 'BASKAN_YARDIMCISI', filters: { active: true, role: 'BASKAN_YARDIMCISI' } },
  { key: 'BASKAN', filters: { active: true, role: 'BASKAN' } },
  { key: 'ADMIN', filters: { active: true, role: 'ADMIN' } },
]

export function AdminDashboardPage() {
  const statQueries = useQueries({
    queries: userStats.map(({ key, filters }) => ({
      queryKey: queryKeys.admin.users.list({ scope: 'dashboard', key, ...filters }),
      queryFn: () => listAdminUsers({ ...filters, page: 0, size: 1 }),
    })),
  })
  const recentLogsQuery = useQuery({
    queryKey: queryKeys.admin.auditLogs.list({ scope: 'dashboard', page: 0, size: 6 }),
    queryFn: () => listAdminAuditLogs({ page: 0, size: 6 }),
  })
  const serverCounts = new Map(
    userStats.map((stat, index) => [stat.key, statQueries[index]?.data?.totalElements ?? 0]),
  )
  const totalUsers = serverCounts.get('total') ?? 0
  const activeUserCount = serverCounts.get('active') ?? 0
  const privilegedUserCount = ['BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'].reduce((total, role) => total + (serverCounts.get(role) ?? 0), 0)
  const totalLogCount = recentLogsQuery.data?.totalElements ?? 0
  const recentLogs = recentLogsQuery.data?.content ?? []
  const dashboardPending = statQueries.some((query) => query.isPending) || recentLogsQuery.isPending
  const dashboardError = statQueries.some((query) => query.isError) || recentLogsQuery.isError
  const cards = [
    { label: 'Toplam kullanıcı', value: totalUsers, icon: UsersRound, tone: 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300' },
    { label: 'Aktif hesap', value: activeUserCount, icon: UserCheck, tone: 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300' },
    { label: 'Yetkili hesap', value: privilegedUserCount, icon: ShieldCheck, tone: 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300' },
    { label: 'Denetim kaydı', value: totalLogCount, icon: FileClock, tone: 'bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300' },
  ]

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Yönetim Özeti</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Kullanıcı hesaplarını, rol dağılımını ve son denetim hareketlerini takip edin.</p>
        </div>
        <Link to="/admin/kullanicilar" className="flex min-h-11 items-center justify-center rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
          Kullanıcıları Yönet
        </Link>
      </header>

      {dashboardError ? (
        <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-800 dark:border-rose-900/70 dark:bg-rose-950/40 dark:text-rose-200" role="alert">
          Yönetim özeti yüklenemedi. Kullanıcı ve işlem kayıtları sayfalarından tekrar deneyebilirsiniz.
        </p>
      ) : null}

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="Yönetim özeti">
        {cards.map((card) => {
          const Icon = card.icon
          return (
            <article key={card.label} className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm sm:p-5">
              <span className={`flex size-10 items-center justify-center rounded-xl ${card.tone}`}>
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <p className="mt-4 text-xs font-semibold text-app-text-subtle sm:text-sm">{card.label}</p>
              <p className="mt-1 text-2xl font-bold text-app-text sm:text-3xl">{dashboardPending ? '—' : card.value}</p>
            </article>
          )
        })}
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <div className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
          <div className="flex items-center justify-between border-b border-app-border-subtle px-4 py-4 sm:px-6">
            <div>
              <h2 className="font-bold text-app-text">Son işlemler</h2>
              <p className="mt-0.5 text-xs text-app-text-subtle">Kullanıcı ve rol işlemleri gösterilir.</p>
            </div>
            <Link to="/admin/loglar" className="text-xs font-bold text-brand-700 dark:text-brand-300 hover:text-brand-800 dark:hover:text-brand-200">Tümünü gör</Link>
          </div>
          <div className="divide-y divide-app-border-subtle">
            {recentLogs.map((log) => (
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
                  <strong className="text-app-text">
                    {dashboardPending ? '—' : serverCounts.get(role) ?? 0}
                  </strong>
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
