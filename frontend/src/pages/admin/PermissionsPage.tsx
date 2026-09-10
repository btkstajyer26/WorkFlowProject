import { KeyRound, ShieldCheck } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { getRolePermissions, listPermissions, updateRolePermissions } from '../../api/permissions'
import { listRoles } from '../../api/roles'
import { useToast } from '../../context/toastState'
import { queryKeys } from '../../query/queryKeys'
import type { AdminRole } from '../../types/admin'
import { ApiClientError } from '../../api/errors'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'

/**
 * AP-3 rol <-> permission matrisi. Admin yalnız backend'in kapalı
 * katalogundan seçim yapar; yeni capability kodu burada üretilemez.
 * Kaydetme tam değişimdir (replace): ekranda işaretli kod kümesi rolün yeni
 * hali olur.
 */
export function PermissionsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const rolesQuery = useQuery({
    queryKey: queryKeys.admin.roles.list(false),
    queryFn: () => listRoles(false),
  })
  const roles = rolesQuery.data ?? []

  const requestedRoleId = Number(searchParams.get('rol'))
  const selectedRoleId = roles.some((role) => role.id === requestedRoleId)
    ? requestedRoleId
    : roles[0]?.id ?? null

  const selectRole = (roleId: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('rol', String(roleId))
    setSearchParams(next, { replace: true })
  }

  const permissionsQuery = useQuery({
    queryKey: queryKeys.admin.permissions.list,
    queryFn: listPermissions,
  })
  const permissions = permissionsQuery.data ?? []

  const rolePermissionsQuery = useQuery({
    queryKey: queryKeys.admin.roles.permissions(selectedRoleId ?? -1),
    queryFn: () => getRolePermissions(selectedRoleId!),
    enabled: selectedRoleId !== null,
  })

  const [draft, setDraft] = useState<Set<string> | null>(null)

  useEffect(() => {
    setDraft(rolePermissionsQuery.data ? new Set(rolePermissionsQuery.data.permissionCodes) : null)
  }, [rolePermissionsQuery.data])

  const dirty = useMemo(() => {
    if (!draft || !rolePermissionsQuery.data) return false
    const saved = new Set(rolePermissionsQuery.data.permissionCodes)
    if (saved.size !== draft.size) return true
    for (const code of draft) if (!saved.has(code)) return true
    return false
  }, [draft, rolePermissionsQuery.data])

  const saveMutation = useMutation({
    mutationFn: () => updateRolePermissions(selectedRoleId!, Array.from(draft ?? [])),
    onSuccess: async (updated) => {
      queryClient.setQueryData(queryKeys.admin.roles.permissions(updated.roleId), updated)
      showToast({
        title: 'Yetkiler güncellendi',
        description: `${updated.roleName} rolünün yetkileri kaydedildi.`,
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Yetkiler kaydedilemedi',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const toggle = (code: string) => {
    setDraft((current) => {
      const next = new Set(current ?? [])
      if (next.has(code)) next.delete(code)
      else next.add(code)
      return next
    })
  }

  const selectedRole = roles.find((role) => role.id === selectedRoleId) ?? null

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Yetkiler</h1>
        <p className="mt-2 text-sm leading-6 text-app-text-muted">
          Rollerin hangi işlem yetkilerini (permission) taşıdığını görüntüleyin ve düzenleyin. Yeni yetki kodu
          burada üretilemez; yalnız mevcut katalogdan atama yapılır.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-[280px_1fr]">
        <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Rol listesi">
          <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle">
            Rol seçin
          </div>
          {rolesQuery.isPending ? (
            <ListLoadingSkeleton label="Roller yükleniyor" rows={4} />
          ) : (
            <ul className="divide-y divide-app-border-subtle">
              {roles.map((role) => (
                <li key={role.id}>
                  <RoleListItem role={role} selected={role.id === selectedRoleId} onSelect={() => selectRole(role.id)} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Rol yetkileri">
          {!selectedRole ? (
            <div className="px-5 py-14 text-center">
              <KeyRound className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
              <h2 className="mt-3 font-bold text-app-text-strong">Bir rol seçin</h2>
            </div>
          ) : permissionsQuery.isPending || rolePermissionsQuery.isPending ? (
            <ListLoadingSkeleton label="Yetkiler yükleniyor" rows={6} />
          ) : permissionsQuery.isError || rolePermissionsQuery.isError ? (
            <div className="px-5 py-14 text-center" role="alert">
              <h2 className="font-bold text-app-text-strong">Yetkiler yüklenemedi</h2>
              <p className="mt-1 text-sm text-app-text-muted">Backend bağlantısını kontrol edip yeniden deneyin.</p>
            </div>
          ) : (
            <>
              <div className="flex flex-col gap-3 border-b border-app-border-subtle px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                <div>
                  <p className="font-bold text-app-text">{selectedRole.name}</p>
                  <p className="text-xs text-app-text-subtle">
                    {(draft?.size ?? 0)} yetki seçili{selectedRole.isSystem ? ' · Sistem rolü' : ''}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={!dirty || saveMutation.isPending}
                  onClick={() => saveMutation.mutate()}
                  className="flex min-h-10 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-45"
                >
                  {saveMutation.isPending ? 'Kaydediliyor…' : 'Kaydet'}
                </button>
              </div>

              <ul className="divide-y divide-app-border-subtle">
                {permissions.map((permission) => {
                  const checked = draft?.has(permission.code) ?? false
                  // Pasif bir yetki yeni atanamaz; zaten atanmışsa kaldırılabilir.
                  const disabled = !permission.isActive && !checked
                  return (
                    <li key={permission.id} className="px-4 py-3 sm:px-6">
                      <label className={`flex items-start gap-3 ${disabled ? 'opacity-50' : 'cursor-pointer'}`}>
                        <input
                          type="checkbox"
                          checked={checked}
                          disabled={disabled}
                          onChange={() => toggle(permission.code)}
                          className="mt-0.5 size-4 shrink-0 accent-brand-700"
                        />
                        <span className="min-w-0">
                          <span className="block font-bold text-app-text-emphasis">
                            {permission.displayName}
                            <code className="ml-2 rounded bg-app-surface-muted px-1.5 py-0.5 text-xs font-normal text-app-text-subtle">
                              {permission.code}
                            </code>
                            {!permission.isActive ? (
                              <span className="ml-2 rounded-full bg-app-surface-strong px-2 py-0.5 text-xs font-bold text-app-text-muted ring-1 ring-inset ring-app-border">
                                Pasif
                              </span>
                            ) : null}
                          </span>
                          {permission.description ? (
                            <span className="mt-0.5 block text-xs text-app-text-subtle">{permission.description}</span>
                          ) : null}
                        </span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            </>
          )}
        </section>
      </div>
    </div>
  )
}

function RoleListItem({
  role,
  selected,
  onSelect,
}: {
  role: AdminRole
  selected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={`flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition ${selected
        ? 'bg-brand-50 dark:bg-brand-900/30'
        : 'hover:bg-app-surface-muted'}`}
    >
      <span className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${selected
        ? 'bg-brand-700 text-white'
        : 'bg-app-surface-muted text-app-text-muted'}`}>
        <ShieldCheck className="size-4" aria-hidden="true" />
      </span>
      <span className="min-w-0">
        <span className="block truncate font-bold text-app-text">{role.name}</span>
        <span className="block truncate text-xs text-app-text-subtle">
          {role.isSystem ? 'Sistem rolü' : 'Dinamik rol'}{role.isActive ? '' : ' · Pasif'}
        </span>
      </span>
    </button>
  )
}
