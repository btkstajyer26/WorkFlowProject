import { ShieldCheck, ShieldPlus } from 'lucide-react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { createRole, listRoles, updateRole } from '../../api/roles'
import { RoleFormDialog } from '../../components/admin/RoleFormDialog'
import { useToast } from '../../context/toastState'
import { queryKeys } from '../../query/queryKeys'
import type { AdminRole } from '../../types/admin'
import type { RoleFormValues } from '../../schemas/admin'
import { ApiClientError } from '../../api/errors'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'

/**
 * AP-2 rol yönetimi. Rol adları sunucudan geldiği gibi gösterilir; `roleLabels`
 * gibi sabit rol listesine dayalı bir çeviri uygulanmaz, çünkü panelden dinamik
 * rol açılabilir. Silme yoktur: erişim pasifleştirmeyle kapanır.
 */
export function RolesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const includeInactive = searchParams.get('pasif') === '1'
  const [formRole, setFormRole] = useState<AdminRole | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const rolesQuery = useQuery({
    queryKey: queryKeys.admin.roles.list(includeInactive),
    queryFn: () => listRoles(includeInactive),
    placeholderData: keepPreviousData,
  })
  const roles = rolesQuery.data ?? []

  const invalidateRoles = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.roles.all })

  const activeMutation = useMutation({
    mutationFn: ({ role, active }: { role: AdminRole; active: boolean }) =>
      updateRole(role.id, { active }),
    onSuccess: async (role) => {
      await invalidateRoles()
      showToast({
        title: role.isActive ? 'Rol etkinleştirildi' : 'Rol pasifleştirildi',
        description: `${role.name} güncellendi.`,
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Rol güncellenemedi',
        description: error instanceof ApiClientError
          ? error.message
          : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const submitForm = async (values: RoleFormValues) => {
    const saved = formRole
      ? await updateRole(formRole.id, {
        name: values.name,
        description: values.description,
        ...(formRole.isSystem ? {} : { workflowActor: values.workflowActor }),
      })
      : await createRole({
        name: values.name,
        description: values.description,
        workflowActor: values.workflowActor,
      })
    await invalidateRoles()
    return saved
  }

  const openCreate = () => {
    setFormRole(null)
    setFormOpen(true)
  }

  const openEdit = (role: AdminRole) => {
    setFormRole(role)
    setFormOpen(true)
  }

  const toggleInactive = (checked: boolean) => {
    const next = new URLSearchParams(searchParams)
    if (checked) next.set('pasif', '1')
    else next.delete('pasif')
    setSearchParams(next, { replace: true })
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Roller</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Sistemde tanımlı rolleri görüntüleyin, yeni rol açın ve erişimlerini yönetin.</p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        >
          <ShieldPlus className="size-4" aria-hidden="true" />
          Yeni rol
        </button>
      </header>

      <section className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm" aria-label="Rol filtreleri">
        <label className="flex items-center gap-3 text-sm font-semibold text-app-text-secondary">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(event) => toggleInactive(event.target.checked)}
            className="size-4 accent-brand-700"
          />
          Pasif rolleri de göster
        </label>
      </section>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Rol listesi">
        <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle sm:px-6">
          {roles.length} rol tanımlı
        </div>
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
                    <th className="px-4 py-3">Tür</th>
                    <th className="px-4 py-3">Durum</th>
                    <th className="px-6 py-3 text-right">İşlemler</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-app-border-subtle">
                  {roles.map((role) => (
                    <tr key={role.id} className="hover:bg-app-surface-muted/70">
                      <td className="px-6 py-4"><RoleIdentity role={role} /></td>
                      <td className="px-4 py-4"><RoleBadges role={role} /></td>
                      <td className="px-4 py-4"><StatusBadge active={role.isActive} /></td>
                      <td className="px-6 py-4">
                        <RoleActions
                          role={role}
                          busy={activeMutation.isPending}
                          onEdit={() => openEdit(role)}
                          onToggleActive={() => activeMutation.mutate({ role, active: !role.isActive })}
                          alignRight
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-app-border-subtle md:hidden">
              {roles.map((role) => (
                <article key={role.id} className="p-4">
                  <RoleIdentity role={role} />
                  <div className="mt-3 flex flex-wrap gap-2">
                    <RoleBadges role={role} />
                    <StatusBadge active={role.isActive} />
                  </div>
                  <RoleActions
                    role={role}
                    busy={activeMutation.isPending}
                    onEdit={() => openEdit(role)}
                    onToggleActive={() => activeMutation.mutate({ role, active: !role.isActive })}
                  />
                </article>
              ))}
            </div>
          </>
        ) : (
          <div className="px-5 py-14 text-center">
            <ShieldCheck className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
            <h2 className="mt-3 font-bold text-app-text-strong">Tanımlı rol bulunamadı</h2>
            <p className="mt-1 text-sm text-app-text-subtle">Yeni rol açarak başlayabilirsiniz.</p>
          </div>
        )}
      </section>

      <RoleFormDialog
        open={formOpen}
        role={formRole}
        onClose={() => setFormOpen(false)}
        onSubmit={submitForm}
      />
    </div>
  )
}

function RoleIdentity({ role }: { role: AdminRole }) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
        <ShieldCheck className="size-4" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <p className="truncate font-bold text-app-text">{role.name}</p>
        <p className="truncate text-xs text-app-text-subtle">{role.description ?? '—'}</p>
      </div>
    </div>
  )
}

function RoleBadges({ role }: { role: AdminRole }) {
  return (
    <span className="flex flex-wrap gap-1.5">
      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${role.isSystem
        ? 'bg-app-surface-strong text-app-text-secondary ring-app-border'
        : 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300 ring-brand-100 dark:ring-brand-800/60'}`}>
        {role.isSystem ? 'Sistem rolü' : 'Dinamik rol'}
      </span>
      {role.isWorkflowActor ? (
        <span className="inline-flex rounded-full bg-app-surface-muted px-2.5 py-1 text-xs font-bold text-app-text-muted ring-1 ring-inset ring-app-border">
          İş akışı aktörü
        </span>
      ) : null}
    </span>
  )
}

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${active
      ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 ring-emerald-200 dark:ring-emerald-800/70'
      : 'bg-app-surface-strong text-app-text-muted ring-app-border'}`}>
      <span className={`size-1.5 rounded-full ${active ? 'bg-emerald-500' : 'bg-slate-400'}`} />
      {active ? 'Aktif' : 'Pasif'}
    </span>
  )
}

function RoleActions({
  role,
  busy,
  onEdit,
  onToggleActive,
  alignRight = false,
}: {
  role: AdminRole
  busy: boolean
  onEdit: () => void
  onToggleActive: () => void
  alignRight?: boolean
}) {
  return (
    <div className={`mt-4 flex gap-2 md:mt-0 ${alignRight ? 'justify-end' : ''}`}>
      <button
        type="button"
        onClick={onEdit}
        className="min-h-9 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary hover:bg-app-surface-muted"
      >
        Düzenle
      </button>
      {/* Sistem rolü pasifleştirilemez; sunucu da reddeder, düğme hiç açılmaz. */}
      <button
        type="button"
        onClick={onToggleActive}
        disabled={role.isSystem || busy}
        title={role.isSystem ? 'Sistem rolü pasifleştirilemez' : undefined}
        className={`min-h-9 rounded-lg px-3 text-xs font-bold disabled:cursor-not-allowed disabled:opacity-45 ${role.isActive
          ? 'bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 hover:bg-rose-100 dark:hover:bg-rose-900/60'
          : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60'}`}
      >
        {role.isActive ? 'Pasifleştir' : 'Etkinleştir'}
      </button>
    </div>
  )
}
