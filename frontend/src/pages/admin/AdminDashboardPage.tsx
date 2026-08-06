import { FileClock, UserCheck, UsersRound, UserX } from 'lucide-react'
import { Link } from 'react-router'
import { useAdmin } from '../../context/adminState'
import { roleLabels } from '../../types/auth'

export function AdminDashboardPage() {
  const { users, logs } = useAdmin()
  const activeUsers = users.filter((user) => user.isActive)
  const inactiveUsers = users.filter((user) => !user.isActive)
  const cards = [
    { label: 'Toplam kullanıcı', value: users.length, icon: UsersRound, tone: 'bg-brand-50 text-brand-700' },
    { label: 'Aktif hesap', value: activeUsers.length, icon: UserCheck, tone: 'bg-emerald-50 text-emerald-700' },
    { label: 'Pasif hesap', value: inactiveUsers.length, icon: UserX, tone: 'bg-rose-50 text-rose-700' },
    { label: 'Denetim kaydı', value: logs.length, icon: FileClock, tone: 'bg-blue-50 text-blue-700' },
  ]

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-brand-600">Sistem Yönetimi</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Yönetim Özeti</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">Kullanıcı durumlarını ve son denetim hareketlerini takip edin.</p>
        </div>
        <Link to="/admin/kullanicilar" className="flex min-h-11 items-center justify-center rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
          Kullanıcıları Yönet
        </Link>
      </header>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="Yönetim özeti">
        {cards.map((card) => {
          const Icon = card.icon
          return (
            <article key={card.label} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
              <span className={`flex size-10 items-center justify-center rounded-xl ${card.tone}`}>
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <p className="mt-4 text-xs font-semibold text-slate-500 sm:text-sm">{card.label}</p>
              <p className="mt-1 text-2xl font-bold text-slate-950 sm:text-3xl">{card.value}</p>
            </article>
          )
        })}
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4 sm:px-6">
            <div>
              <h2 className="font-bold text-slate-950">Son işlemler</h2>
              <p className="mt-0.5 text-xs text-slate-500">Evrak ve kullanıcı hareketleri birlikte gösterilir.</p>
            </div>
            <Link to="/admin/loglar" className="text-xs font-bold text-brand-700 hover:text-brand-800">Tümünü gör</Link>
          </div>
          <div className="divide-y divide-slate-100">
            {logs.slice(0, 6).map((log) => (
              <div key={log.id} className="flex gap-3 px-4 py-4 sm:px-6">
                <span className={`mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl ${log.type === 'USER' ? 'bg-brand-50 text-brand-700' : 'bg-blue-50 text-blue-700'}`}>
                  {log.type === 'USER' ? <UsersRound className="size-4" aria-hidden="true" /> : <FileClock className="size-4" aria-hidden="true" />}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-bold text-slate-900">{log.actionLabel}</p>
                    <time className="text-[11px] text-slate-500">{formatDateTime(log.createdAt)}</time>
                  </div>
                  <p className="mt-1 truncate text-xs text-slate-600">{log.target}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <aside className="space-y-5">
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-bold text-slate-950">Rol dağılımı</h2>
            <div className="mt-3 space-y-2">
              {(['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN'] as const).map((role) => (
                <div key={role} className="flex items-center justify-between gap-3 text-sm">
                  <span className="text-slate-600">{roleLabels[role]}</span>
                  <strong className="text-slate-950">{users.filter((user) => user.isActive && user.role === role).length}</strong>
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
