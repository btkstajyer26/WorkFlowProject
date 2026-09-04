import { ShieldCheck } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { listRoles } from '../../api/roles'
import { queryKeys } from '../../query/queryKeys'
import type { AdminRole } from '../../types/admin'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'

/**
 * AP-1 yalnız-okur rol kataloğu. Rol adları sunucudan geldiği gibi gösterilir;
 * `roleLabels` gibi sabit rol listesine dayalı bir çeviri uygulanmaz, çünkü
 * panelden dinamik rol açılabilir.
 */
export function RolesPage() {
  const rolesQuery = useQuery({
    queryKey: queryKeys.admin.roles.list(),
    queryFn: () => listRoles(),
  })
  const roles = rolesQuery.data ?? []

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Roller</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Sistemde tanımlı rolleri görüntüleyin. Bu ekran yalnızca okuma amaçlıdır.</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-app-text-muted">
          <span className="flex size-8 items-center justify-center rounded-lg bg-brand-50 dark:bg-brand-900/30 font-bold text-brand-700 dark:text-brand-300">
            {roles.length}
          </span>
          rol tanımlı
        </div>
      </header>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Rol listesi">
        {rolesQuery.isPending ? (
          <ListLoadingSkeleton label="Roller yükleniyor" rows={5} />
        ) : rolesQuery.isError ? (
          <div className="px-5 py-14 text-center" role="alert">
            <h2 className="font-bold text-app-text-strong">Roller yüklenemedi</h2>
            <p className="mt-1 text-sm text-app-text-muted">Backend bağlantısını kontrol edip yeniden deneyin.</p>
            <button
              type="button"
              onClick={() => void rolesQuery.refetch()}
              className="mt-4 min-h-10 rounded-lg border border-app-border px-4 text-xs font-bold text-app-text-secondary hover:bg-app-surface-muted"
            >
              Tekrar dene
            </button>
          </div>
        ) : roles.length ? (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-subtle">
                  <tr>
                    <th className="px-6 py-3">Rol</th>
                    <th className="px-6 py-3">Açıklama</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-app-border-subtle">
                  {roles.map((role) => (
                    <tr key={role.id} className="hover:bg-app-surface-muted/70">
                      <td className="px-6 py-4"><RoleName role={role} /></td>
                      <td className="px-6 py-4 text-app-text-muted">{role.description ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-app-border-subtle md:hidden">
              {roles.map((role) => (
                <article key={role.id} className="p-4">
                  <RoleName role={role} />
                  <p className="mt-2 text-xs text-app-text-muted">{role.description ?? '—'}</p>
                </article>
              ))}
            </div>
          </>
        ) : (
          <div className="px-5 py-14 text-center">
            <ShieldCheck className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
            <h2 className="mt-3 font-bold text-app-text-strong">Tanımlı rol bulunamadı</h2>
            <p className="mt-1 text-sm text-app-text-subtle">Sistemde görüntülenebilecek aktif bir rol yok.</p>
          </div>
        )}
      </section>
    </div>
  )
}

function RoleName({ role }: { role: AdminRole }) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
        <ShieldCheck className="size-4" aria-hidden="true" />
      </span>
      <p className="truncate font-bold text-app-text">{role.name}</p>
    </div>
  )
}
