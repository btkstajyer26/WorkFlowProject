import { Building2, Plus, UserMinus, UserPlus } from 'lucide-react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  addDepartmentMember,
  createDepartment,
  listDepartmentMembers,
  listDepartments,
  removeDepartmentMember,
  updateDepartment,
} from '../../api/departments'
import { listAllAdminUsers } from '../../api/admin'
import { AdminDialog } from '../../components/admin/AdminDialog'
import { useToast } from '../../context/toastState'
import { queryKeys } from '../../query/queryKeys'
import type { AdminDepartment } from '../../types/admin'
import { ApiClientError } from '../../api/errors'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'

/**
 * AP-4 departman ve üyelik yönetimi. `parentDepartmentId` yalnız yapısal
 * bilgidir; akış hedef çözümünde veya otomatik eskalasyonda kullanılmaz -
 * ekran bu izlenimi vermemek için hiyerarşiyi yalnız etiket olarak gösterir.
 */
export function DepartmentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const includeInactive = searchParams.get('pasif') === '1'

  const departmentsQuery = useQuery({
    queryKey: queryKeys.admin.departments.list(includeInactive),
    queryFn: () => listDepartments(includeInactive),
    placeholderData: keepPreviousData,
  })
  const departments = departmentsQuery.data ?? []
  const departmentById = new Map(departments.map((d) => [d.id, d]))

  const requestedId = Number(searchParams.get('departman'))
  const selectedId = departments.some((d) => d.id === requestedId) ? requestedId : departments[0]?.id ?? null
  const selectedDepartment = selectedId !== null ? departmentById.get(selectedId) ?? null : null

  const selectDepartment = (id: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('departman', String(id))
    setSearchParams(next, { replace: true })
  }

  const toggleInactive = (checked: boolean) => {
    const next = new URLSearchParams(searchParams)
    if (checked) next.set('pasif', '1')
    else next.delete('pasif')
    setSearchParams(next, { replace: true })
  }

  const invalidateDepartments = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.departments.all })

  const [createOpen, setCreateOpen] = useState(false)

  const activeMutation = useMutation({
    mutationFn: ({ department, active }: { department: AdminDepartment; active: boolean }) =>
      updateDepartment(department.id, { active }),
    onSuccess: async (department) => {
      await invalidateDepartments()
      showToast({
        title: department.isActive ? 'Departman etkinleştirildi' : 'Departman pasifleştirildi',
        description: `${department.name} güncellendi.`,
        tone: 'success',
      })
    },
    onError: (error) => {
      showToast({
        title: 'Departman güncellenemedi',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Departmanlar</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">
            Organizasyon gruplarını ve üyeliklerini yönetin. Üst departman yalnız yapısal bilgidir; akışlarda
            otomatik yükseltme yapılmaz.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setCreateOpen(true)}
          className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        >
          <Plus className="size-4" aria-hidden="true" />
          Yeni departman
        </button>
      </header>

      <section className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm" aria-label="Departman filtreleri">
        <label className="flex items-center gap-3 text-sm font-semibold text-app-text-secondary">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(event) => toggleInactive(event.target.checked)}
            className="size-4 accent-brand-700"
          />
          Pasif departmanları da göster
        </label>
      </section>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-[320px_1fr]">
        <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Departman listesi">
          <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle">
            {departments.length} departman tanımlı
          </div>
          {departmentsQuery.isPending ? (
            <ListLoadingSkeleton label="Departmanlar yükleniyor" rows={4} />
          ) : departments.length === 0 ? (
            <div className="px-5 py-14 text-center">
              <Building2 className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
              <h2 className="mt-3 font-bold text-app-text-strong">Tanımlı departman bulunamadı</h2>
            </div>
          ) : (
            <ul className="divide-y divide-app-border-subtle">
              {departments.map((department) => (
                <li key={department.id}>
                  <button
                    type="button"
                    onClick={() => selectDepartment(department.id)}
                    aria-pressed={department.id === selectedId}
                    className={`flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition ${department.id === selectedId
                      ? 'bg-brand-50 dark:bg-brand-900/30'
                      : 'hover:bg-app-surface-muted'}`}
                  >
                    <span className={`flex size-8 shrink-0 items-center justify-center rounded-lg ${department.id === selectedId
                      ? 'bg-brand-700 text-white'
                      : 'bg-app-surface-muted text-app-text-muted'}`}>
                      <Building2 className="size-4" aria-hidden="true" />
                    </span>
                    <span className="min-w-0">
                      <span className="block truncate font-bold text-app-text">{department.name}</span>
                      <span className="block truncate text-xs text-app-text-subtle">
                        {department.parentDepartmentId !== null
                          ? `Üst: ${departmentById.get(department.parentDepartmentId)?.name ?? '—'}`
                          : 'Kök departman'}
                        {department.isActive ? '' : ' · Pasif'}
                      </span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {selectedDepartment ? (
          <DepartmentDetail
            department={selectedDepartment}
            departments={departments}
            onToggleActive={(active) => activeMutation.mutate({ department: selectedDepartment, active })}
            toggleBusy={activeMutation.isPending}
          />
        ) : (
          <section className="flex items-center justify-center rounded-2xl border border-app-border bg-app-surface p-14 text-center shadow-sm">
            <div>
              <Building2 className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
              <h2 className="mt-3 font-bold text-app-text-strong">Bir departman seçin</h2>
            </div>
          </section>
        )}
      </div>

      <CreateDepartmentDialog
        open={createOpen}
        departments={departments}
        onClose={() => setCreateOpen(false)}
        onCreated={async (department) => {
          await invalidateDepartments()
          selectDepartment(department.id)
        }}
      />
    </div>
  )
}

function DepartmentDetail({
  department,
  departments,
  onToggleActive,
  toggleBusy,
}: {
  department: AdminDepartment
  departments: AdminDepartment[]
  onToggleActive: (active: boolean) => void
  toggleBusy: boolean
}) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const [name, setName] = useState(department.name)
  const [parentDepartmentId, setParentDepartmentId] = useState<number | ''>(department.parentDepartmentId ?? '')

  // Secili departman degistiginde formu senkronize et.
  const [syncedId, setSyncedId] = useState(department.id)
  if (syncedId !== department.id) {
    setSyncedId(department.id)
    setName(department.name)
    setParentDepartmentId(department.parentDepartmentId ?? '')
  }

  const dirty = name.trim() !== department.name || (parentDepartmentId || null) !== department.parentDepartmentId

  const saveMutation = useMutation({
    mutationFn: () => updateDepartment(department.id, {
      name: name.trim() !== department.name ? name.trim() : undefined,
      parentDepartmentId: parentDepartmentId === '' ? undefined : parentDepartmentId,
      clearParent: parentDepartmentId === '' && department.parentDepartmentId !== null,
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.departments.all })
      showToast({ title: 'Departman güncellendi', description: `${name.trim()} kaydedildi.`, tone: 'success' })
    },
    onError: (error) => {
      showToast({
        title: 'Departman kaydedilemedi',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const parentOptions = departments.filter((d) => d.id !== department.id)

  return (
    <div className="space-y-5">
      <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-bold text-app-text">{department.name}</h2>
          <button
            type="button"
            disabled={toggleBusy}
            onClick={() => onToggleActive(!department.isActive)}
            className={`min-h-9 rounded-lg px-3 text-xs font-bold disabled:cursor-not-allowed disabled:opacity-45 ${department.isActive
              ? 'bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 hover:bg-rose-100 dark:hover:bg-rose-900/60'
              : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60'}`}
          >
            {department.isActive ? 'Pasifleştir' : 'Etkinleştir'}
          </button>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="dept-name" className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Ad</label>
            <input
              id="dept-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            />
          </div>
          <div>
            <label htmlFor="dept-parent" className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Üst departman</label>
            <select
              id="dept-parent"
              value={parentDepartmentId}
              onChange={(event) => setParentDepartmentId(event.target.value ? Number(event.target.value) : '')}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            >
              <option value="">Yok (kök departman)</option>
              {parentOptions.map((option) => (
                <option key={option.id} value={option.id}>{option.name}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-4 flex justify-end">
          <button
            type="button"
            disabled={!dirty || saveMutation.isPending}
            onClick={() => saveMutation.mutate()}
            className="min-h-10 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-45"
          >
            {saveMutation.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </div>
      </section>

      <DepartmentMembersPanel departmentId={department.id} />
    </div>
  )
}

function DepartmentMembersPanel({ departmentId }: { departmentId: number }) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [selectedUserId, setSelectedUserId] = useState('')

  const membersQuery = useQuery({
    queryKey: queryKeys.admin.departments.members(departmentId),
    queryFn: () => listDepartmentMembers(departmentId),
  })
  const usersQuery = useQuery({
    queryKey: queryKeys.admin.users.options,
    queryFn: () => listAllAdminUsers(),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.departments.members(departmentId) })

  const addMutation = useMutation({
    mutationFn: (userId: string) => addDepartmentMember(departmentId, userId),
    onSuccess: async () => {
      await invalidate()
      showToast({ title: 'Üye eklendi', tone: 'success' })
    },
    onError: (error) => {
      showToast({
        title: 'Üye eklenemedi',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (userId: string) => removeDepartmentMember(departmentId, userId),
    onSuccess: async () => {
      await invalidate()
      showToast({ title: 'Üye çıkarıldı', tone: 'success' })
    },
    onError: (error) => {
      showToast({
        title: 'Üye çıkarılamadı',
        description: error instanceof ApiClientError ? error.message : 'Beklenmeyen bir hata oluştu.',
        tone: 'error',
      })
    },
  })

  const memberIds = new Set((membersQuery.data?.members ?? []).map((m) => m.id))
  const addableUsers = (usersQuery.data ?? []).filter((u) => !memberIds.has(u.id))

  return (
    <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
      <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle sm:px-6">
        Üyeler
      </div>
      {membersQuery.isPending ? (
        <ListLoadingSkeleton label="Üyeler yükleniyor" rows={3} />
      ) : (
        <>
          <ul className="divide-y divide-app-border-subtle">
            {(membersQuery.data?.members ?? []).map((member) => (
              <li key={member.id} className="flex items-center justify-between gap-3 px-4 py-3 sm:px-6">
                <span className="min-w-0">
                  <span className="block truncate font-bold text-app-text">{member.firstName} {member.lastName}</span>
                  <span className="block truncate text-xs text-app-text-subtle">{member.email} · {member.roleName}</span>
                </span>
                <button
                  type="button"
                  disabled={removeMutation.isPending}
                  onClick={() => removeMutation.mutate(member.id)}
                  className="flex min-h-9 shrink-0 items-center gap-1.5 rounded-lg border border-app-border px-3 text-xs font-bold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-45 dark:text-rose-300 dark:hover:bg-rose-950/40"
                >
                  <UserMinus className="size-3.5" aria-hidden="true" />
                  Çıkar
                </button>
              </li>
            ))}
            {(membersQuery.data?.members ?? []).length === 0 ? (
              <li className="px-4 py-6 text-center text-sm text-app-text-subtle sm:px-6">Bu departmanda henüz üye yok.</li>
            ) : null}
          </ul>

          <div className="flex flex-wrap items-center gap-2 border-t border-app-border-subtle px-4 py-3 sm:px-6">
            <select
              value={selectedUserId}
              onChange={(event) => setSelectedUserId(event.target.value)}
              className="min-h-9 rounded-lg border border-app-border bg-app-surface px-2 text-xs text-app-text outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
              aria-label="Departmana üye ekle"
            >
              <option value="">Kullanıcı seçin…</option>
              {addableUsers.map((user) => (
                <option key={user.id} value={user.id}>{user.firstName} {user.lastName} ({user.email})</option>
              ))}
            </select>
            <button
              type="button"
              disabled={!selectedUserId || addMutation.isPending}
              onClick={() => {
                if (!selectedUserId) return
                addMutation.mutate(selectedUserId)
                setSelectedUserId('')
              }}
              className="flex min-h-9 items-center gap-1.5 rounded-lg bg-brand-700 px-3 text-xs font-bold text-white transition hover:bg-brand-800 disabled:cursor-not-allowed disabled:opacity-45"
            >
              <UserPlus className="size-3.5" aria-hidden="true" />
              Ekle
            </button>
          </div>
        </>
      )}
    </section>
  )
}

function CreateDepartmentDialog({
  open,
  departments,
  onClose,
  onCreated,
}: {
  open: boolean
  departments: AdminDepartment[]
  onClose: () => void
  onCreated: (department: AdminDepartment) => Promise<void>
}) {
  const { showToast } = useToast()
  const [name, setName] = useState('')
  const [parentDepartmentId, setParentDepartmentId] = useState<number | ''>('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const closeDialog = () => {
    if (submitting) return
    setName('')
    setParentDepartmentId('')
    setError(null)
    onClose()
  }

  const submit = async () => {
    setError(null)
    setSubmitting(true)
    try {
      const department = await createDepartment({
        name,
        parentDepartmentId: parentDepartmentId === '' ? undefined : parentDepartmentId,
      })
      showToast({ title: 'Departman oluşturuldu', description: `${department.name} kaydedildi.`, tone: 'success' })
      await onCreated(department)
      closeDialog()
    } catch (caught) {
      setError(caught instanceof ApiClientError ? caught.message : 'Departman kaydedilemedi. Lütfen tekrar deneyin.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AdminDialog
      open={open}
      onClose={closeDialog}
      icon={Building2}
      title="Yeni departman"
      description="Yeni departman her zaman aktif açılır; üst departman isteğe bağlıdır."
    >
      <form
        className="mt-6 space-y-4"
        noValidate
        onSubmit={(event) => { event.preventDefault(); void submit() }}
      >
        <div>
          <label htmlFor="new-dept-name" className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Departman adı</label>
          <input
            id="new-dept-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
            className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          />
        </div>
        <div>
          <label htmlFor="new-dept-parent" className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Üst departman</label>
          <select
            id="new-dept-parent"
            value={parentDepartmentId}
            onChange={(event) => setParentDepartmentId(event.target.value ? Number(event.target.value) : '')}
            className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          >
            <option value="">Yok (kök departman)</option>
            {departments.map((option) => (
              <option key={option.id} value={option.id}>{option.name}</option>
            ))}
          </select>
        </div>

        {error ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
            {error}
          </p>
        ) : null}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            type="button"
            disabled={submitting}
            onClick={closeDialog}
            className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted disabled:opacity-60"
          >
            Vazgeç
          </button>
          <button
            type="submit"
            disabled={submitting || !name.trim()}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-wait disabled:opacity-60"
          >
            {submitting ? 'Kaydediliyor…' : 'Departman Oluştur'}
          </button>
        </div>
      </form>
    </AdminDialog>
  )
}
