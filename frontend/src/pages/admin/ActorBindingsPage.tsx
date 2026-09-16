import { ArrowRight, ShieldCheck, Workflow } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { bindActor, listActorBindings, unbindActor } from '../../api/actorBindings'
import { listRoles } from '../../api/roles'
import { useToast } from '../../context/toastState'
import { queryKeys } from '../../query/queryKeys'
import type { AdminActorBinding } from '../../types/admin'
import { ApiClientError } from '../../api/errors'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'

const ACTOR_REQUIREMENT_LABELS: Record<AdminActorBinding['actorRequirement'], string> = {
  CREATOR: 'Kaydı oluşturan',
  ASSIGNEE: 'Doğrudan atanan',
  CREATOR_AND_ASSIGNEE: 'Oluşturan ve atanan aynı kişi',
}

type BindingGroup = {
  key: string
  fromStatusDisplayName: string
  actionDisplayName: string
  toStatusDisplayName: string
  actorRequirement: AdminActorBinding['actorRequirement']
  bindings: AdminActorBinding[]
  /** Yeni rol bağlarken şablon olarak kullanılacak aktif satır; hiç yoksa bağlama kapalıdır. */
  templateBindingId: number | null
}

function groupBindings(bindings: AdminActorBinding[]): BindingGroup[] {
  const groups = new Map<string, BindingGroup>()
  for (const binding of bindings) {
    const key = `${binding.fromStatusId}:${binding.actionId}:${binding.toStatusId}`
    let group = groups.get(key)
    if (!group) {
      group = {
        key,
        fromStatusDisplayName: binding.fromStatusDisplayName,
        actionDisplayName: binding.actionDisplayName,
        toStatusDisplayName: binding.toStatusDisplayName,
        actorRequirement: binding.actorRequirement,
        bindings: [],
        templateBindingId: null,
      }
      groups.set(key, group)
    }
    group.bindings.push(binding)
    if (binding.isActive) group.templateBindingId ??= binding.bindingId
  }
  return Array.from(groups.values())
}

/**
 * AP-8: WF-8'in hazır servisine ekran. Grafik topolojisi (durum/aksiyon
 * kataloğu) burada değiştirilmez; yalnız hangi dinamik rolün hangi sabit
 * geçişte aktör olabileceği yönetilir. Sistem rolüne ait bağlar korumalıdır
 * ve kaldırılamaz.
 */
export function ActorBindingsPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const bindingsQuery = useQuery({
    queryKey: queryKeys.admin.actorBindings.list,
    queryFn: listActorBindings,
  })
  const rolesQuery = useQuery({
    queryKey: queryKeys.admin.roles.list(false),
    queryFn: () => listRoles(false),
  })

  const groups = useMemo(() => groupBindings(bindingsQuery.data ?? []), [bindingsQuery.data])

  // Bağlanabilir roller: dinamik (sistem değil), aktif ve workflow aktörü işaretli.
  const eligibleRoles = useMemo(
    () => (rolesQuery.data ?? []).filter((role) => !role.isSystem && role.isWorkflowActor),
    [rolesQuery.data],
  )

  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.actorBindings.all })

  const bindMutation = useMutation({
    mutationFn: ({ templateBindingId, actorRoleId }: { templateBindingId: number; actorRoleId: number }) =>
      bindActor(templateBindingId, actorRoleId),
    onSuccess: async (binding) => {
      await invalidate()
      showToast({
        title: 'Rol bağlandı',
        description: `${binding.actorRoleName}, ${binding.fromStatusDisplayName} → ${binding.actionDisplayName} geçişinde aktör oldu.`,
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Rol bağlanamadı',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const unbindMutation = useMutation({
    mutationFn: (bindingId: number) => unbindActor(bindingId),
    onSuccess: async (binding) => {
      await invalidate()
      showToast({
        title: 'Bağ kaldırıldı',
        description: `${binding.actorRoleName} artık bu geçişte aktör değil.`,
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Bağ kaldırılamadı',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const isPending = bindingsQuery.isPending || rolesQuery.isPending
  const isError = bindingsQuery.isError || rolesQuery.isError

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Aktör-Rol Bağlama</h1>
        <p className="mt-2 text-sm leading-6 text-app-text-muted">
          Mevcut iş akışı geçişlerine dinamik roller aktör olarak bağlanır. Akışın durum ve aksiyon yapısı
          (grafik topolojisi) burada değiştirilemez; yalnız o geçişi kimin işleyebileceği yönetilir.
        </p>
      </header>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        {isPending ? (
          <ListLoadingSkeleton label="Geçişler yükleniyor" rows={5} />
        ) : isError ? (
          <div className="px-5 py-14 text-center" role="alert">
            <h2 className="font-bold text-app-text-strong">Geçişler yüklenemedi</h2>
            <p className="mt-1 text-sm text-app-text-muted">Backend bağlantısını kontrol edip yeniden deneyin.</p>
          </div>
        ) : groups.length === 0 ? (
          <div className="px-5 py-14 text-center">
            <Workflow className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
            <h2 className="mt-3 font-bold text-app-text-strong">Tanımlı geçiş bulunamadı</h2>
          </div>
        ) : (
          <ul className="divide-y divide-app-border-subtle">
            {groups.map((group) => (
              <li key={group.key}>
                <BindingGroupCard
                  group={group}
                  eligibleRoles={eligibleRoles}
                  onBind={(actorRoleId) => {
                    if (group.templateBindingId === null) return
                    bindMutation.mutate({ templateBindingId: group.templateBindingId, actorRoleId })
                  }}
                  onUnbind={(bindingId) => unbindMutation.mutate(bindingId)}
                  bindPending={bindMutation.isPending}
                  unbindPending={unbindMutation.isPending}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

function BindingGroupCard({
  group,
  eligibleRoles,
  onBind,
  onUnbind,
  bindPending,
  unbindPending,
}: {
  group: BindingGroup
  eligibleRoles: { id: number; name: string }[]
  onBind: (actorRoleId: number) => void
  onUnbind: (bindingId: number) => void
  bindPending: boolean
  unbindPending: boolean
}) {
  const boundRoleIds = new Set(group.bindings.filter((b) => b.isActive).map((b) => b.actorRoleId))
  const bindableRoles = eligibleRoles.filter((role) => !boundRoleIds.has(role.id))
  const [selectedRoleId, setSelectedRoleId] = useState<number | ''>('')

  return (
    <div className="px-4 py-4 sm:px-6">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="rounded-full bg-app-surface-muted px-2.5 py-1 font-bold text-app-text">
          {group.fromStatusDisplayName}
        </span>
        <ArrowRight className="size-3.5 text-app-text-disabled" aria-hidden="true" />
        <span className="font-semibold text-brand-700 dark:text-brand-300">{group.actionDisplayName}</span>
        <ArrowRight className="size-3.5 text-app-text-disabled" aria-hidden="true" />
        <span className="rounded-full bg-app-surface-muted px-2.5 py-1 font-bold text-app-text">
          {group.toStatusDisplayName}
        </span>
        <span className="ml-1 text-xs text-app-text-subtle">
          · {ACTOR_REQUIREMENT_LABELS[group.actorRequirement]}
        </span>
      </div>

      <ul className="mt-3 flex flex-wrap gap-2">
        {group.bindings.map((binding) => (
          <li
            key={binding.bindingId}
            className={`flex items-center gap-2 rounded-xl px-3 py-1.5 text-xs font-bold ring-1 ring-inset ${binding.isActive
              ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 ring-emerald-200 dark:ring-emerald-800/70'
              : 'bg-app-surface-strong text-app-text-muted ring-app-border'}`}
          >
            {binding.isProtected ? <ShieldCheck className="size-3.5" aria-hidden="true" /> : null}
            {binding.actorRoleName}
            {!binding.isActive ? <span className="font-normal">(pasif)</span> : null}
            {binding.isActive && !binding.isProtected ? (
              <button
                type="button"
                disabled={unbindPending}
                onClick={() => onUnbind(binding.bindingId)}
                className="ml-1 rounded-full text-rose-700 underline decoration-dotted hover:text-rose-900 disabled:cursor-not-allowed disabled:opacity-50 dark:text-rose-300 dark:hover:text-rose-200"
              >
                Kaldır
              </button>
            ) : null}
          </li>
        ))}
      </ul>

      {group.templateBindingId !== null && bindableRoles.length > 0 ? (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <select
            value={selectedRoleId}
            onChange={(event) => setSelectedRoleId(event.target.value ? Number(event.target.value) : '')}
            className="min-h-9 rounded-lg border border-app-border bg-app-surface px-2 text-xs text-app-text outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            aria-label={`${group.actionDisplayName} geçişine rol bağla`}
          >
            <option value="">Rol seçin…</option>
            {bindableRoles.map((role) => (
              <option key={role.id} value={role.id}>{role.name}</option>
            ))}
          </select>
          <button
            type="button"
            disabled={selectedRoleId === '' || bindPending}
            onClick={() => {
              if (selectedRoleId === '') return
              onBind(selectedRoleId)
              setSelectedRoleId('')
            }}
            className="min-h-9 rounded-lg bg-brand-700 px-3 text-xs font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Bağla
          </button>
        </div>
      ) : null}
    </div>
  )
}
