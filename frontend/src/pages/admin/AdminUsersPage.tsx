import { ChevronLeft, ChevronRight, Search, ShieldAlert, UserPlus } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { AccountStatusDialog } from '../../components/admin/AccountStatusDialog'
import { ChangeRoleDialog } from '../../components/admin/ChangeRoleDialog'
import { CreateUserDialog } from '../../components/admin/CreateUserDialog'
import { useAdmin } from '../../context/adminState'
import { useDebouncedSearchParam } from '../../hooks/useDebouncedSearchParam'
import { roleLabels, type UserRole } from '../../types/auth'
import type { ManagedUser } from '../../types/admin'

const pageSize = 6
const roleValues: UserRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN', 'ADMIN']

export function AdminUsersPage() {
  const { users } = useAdmin()
  const [searchParams, setSearchParams] = useSearchParams()
  const [createOpen, setCreateOpen] = useState(false)
  const [roleUser, setRoleUser] = useState<ManagedUser | null>(null)
  const [statusUser, setStatusUser] = useState<ManagedUser | null>(null)
  const query = searchParams.get('q')?.trim().toLocaleLowerCase('tr-TR') ?? ''
  const [searchInput, setSearchInput] = useDebouncedSearchParam(searchParams, setSearchParams)
  const roleParam = searchParams.get('rol')
  const role = roleValues.includes(roleParam as UserRole) ? roleParam as UserRole : ''
  const statusParam = searchParams.get('durum')
  const status = statusParam === 'aktif' || statusParam === 'pasif'
    ? statusParam
    : ''
  const rawPage = Number(searchParams.get('sayfa'))
  const requestedPage = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1

  const filteredUsers = useMemo(() => users.filter((user) => {
    const searchable = `${user.firstName} ${user.lastName} ${user.email}`.toLocaleLowerCase('tr-TR')
    return (!query || searchable.includes(query))
      && (!role || user.role === role)
      && (!status || user.isActive === (status === 'aktif'))
  }), [query, role, status, users])
  const pageCount = Math.max(1, Math.ceil(filteredUsers.length / pageSize))
  const currentPage = Math.min(requestedPage, pageCount)
  const visibleUsers = filteredUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize)

  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    if (roleParam && !role) next.delete('rol')
    if (statusParam && !status) next.delete('durum')
    if (!Number.isInteger(rawPage) || rawPage <= 1) next.delete('sayfa')
    else if (rawPage > pageCount) {
      if (pageCount <= 1) next.delete('sayfa')
      else next.set('sayfa', String(pageCount))
    }
    if (next.toString() !== searchParams.toString()) setSearchParams(next, { replace: true })
  }, [pageCount, rawPage, role, roleParam, searchParams, setSearchParams, status, statusParam])

  const updateFilter = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('sayfa')
    setSearchParams(next)
  }

  const setPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page <= 1) next.delete('sayfa')
    else next.set('sayfa', String(page))
    setSearchParams(next)
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-brand-600 dark:text-brand-400">Sistem Yönetimi</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Kullanıcılar</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Hesap açın, iş akışı rolünü değiştirin veya erişimi kapatın.</p>
        </div>
        <button onClick={() => setCreateOpen(true)} type="button" className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
          <UserPlus className="size-4" aria-hidden="true" />
          Yeni Hesap
        </button>
      </header>

      <section className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm" aria-label="Kullanıcı filtreleri">
        <div className="grid gap-3 lg:grid-cols-[minmax(16rem,1fr)_14rem_12rem]">
          <label className="relative block">
            <span className="sr-only">Kullanıcı ara</span>
            <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
            <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Ad, soyad veya e-posta ara" className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface pl-10 pr-3 text-sm text-app-text-strong outline-none placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60" />
          </label>
          <select aria-label="Role göre filtrele" value={role} onChange={(event) => updateFilter('rol', event.target.value)} className="min-h-11 rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60">
            <option value="">Tüm roller</option>
            {roleValues.map((value) => <option key={value} value={value}>{roleLabels[value]}</option>)}
          </select>
          <select aria-label="Hesap durumuna göre filtrele" value={status} onChange={(event) => updateFilter('durum', event.target.value)} className="min-h-11 rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60">
            <option value="">Tüm durumlar</option>
            <option value="aktif">Aktif</option>
            <option value="pasif">Pasif</option>
          </select>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle sm:px-6">
          {filteredUsers.length} kullanıcı bulundu
        </div>
        {visibleUsers.length ? (
          <>
            <div className="hidden overflow-x-auto md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-subtle">
                  <tr><th className="px-6 py-3">Kullanıcı</th><th className="px-4 py-3">Rol</th><th className="px-4 py-3">Durum</th><th className="px-6 py-3 text-right">İşlemler</th></tr>
                </thead>
                <tbody className="divide-y divide-app-border-subtle">
                  {visibleUsers.map((user) => <UserTableRow key={user.id} user={user} onRole={() => setRoleUser(user)} onStatus={() => setStatusUser(user)} />)}
                </tbody>
              </table>
            </div>
            <div className="divide-y divide-app-border-subtle md:hidden">
              {visibleUsers.map((user) => <UserCard key={user.id} user={user} onRole={() => setRoleUser(user)} onStatus={() => setStatusUser(user)} />)}
            </div>
          </>
        ) : (
          <div className="px-5 py-14 text-center">
            <Search className="mx-auto size-8 text-app-text-disabled" aria-hidden="true" />
            <h2 className="mt-3 font-bold text-app-text-strong">Kullanıcı bulunamadı</h2>
            <p className="mt-1 text-sm text-app-text-subtle">Filtreleri değiştirerek tekrar deneyin.</p>
          </div>
        )}
        <Pagination page={currentPage} pageCount={pageCount} onPage={setPage} />
      </section>

      <CreateUserDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <ChangeRoleDialog user={roleUser} open={Boolean(roleUser)} onClose={() => setRoleUser(null)} />
      <AccountStatusDialog user={statusUser} open={Boolean(statusUser)} onClose={() => setStatusUser(null)} />
    </div>
  )
}

function UserTableRow({ user, onRole, onStatus }: UserActionsProps) {
  return (
    <tr className="hover:bg-app-surface-muted/70">
      <td className="px-6 py-4"><UserIdentity user={user} /></td>
      <td className="px-4 py-4"><RoleBadge role={user.role} /></td>
      <td className="px-4 py-4"><StatusBadge active={user.isActive} /></td>
      <td className="px-6 py-4"><ActionButtons user={user} onRole={onRole} onStatus={onStatus} alignRight /></td>
    </tr>
  )
}

function UserCard({ user, onRole, onStatus }: UserActionsProps) {
  return (
    <article className="p-4">
      <UserIdentity user={user} />
      <div className="mt-3 flex flex-wrap gap-2"><RoleBadge role={user.role} /><StatusBadge active={user.isActive} /></div>
      <ActionButtons user={user} onRole={onRole} onStatus={onStatus} />
    </article>
  )
}

type UserActionsProps = { user: ManagedUser; onRole: () => void; onStatus: () => void }

function UserIdentity({ user }: { user: ManagedUser }) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brand-100 dark:bg-brand-900/45 text-xs font-bold text-brand-700 dark:text-brand-300">{`${user.firstName[0]}${user.lastName[0]}`.toLocaleUpperCase('tr-TR')}</span>
      <div className="min-w-0"><p className="truncate font-bold text-app-text">{user.firstName} {user.lastName}</p><p className="truncate text-xs text-app-text-subtle">{user.email}</p></div>
    </div>
  )
}

function RoleBadge({ role }: { role: UserRole }) {
  return <span className="inline-flex rounded-full bg-brand-50 dark:bg-brand-900/30 px-2.5 py-1 text-xs font-bold text-brand-700 dark:text-brand-300 ring-1 ring-inset ring-brand-100 dark:ring-brand-800/60">{roleLabels[role]}</span>
}

function StatusBadge({ active }: { active: boolean }) {
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${active ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 ring-emerald-200 dark:ring-emerald-800/70' : 'bg-app-surface-strong text-app-text-muted ring-app-border'}`}><span className={`size-1.5 rounded-full ${active ? 'bg-emerald-500' : 'bg-slate-400'}`} />{active ? 'Aktif' : 'Pasif'}</span>
}

function ActionButtons({ user, onRole, onStatus, alignRight = false }: UserActionsProps & { alignRight?: boolean }) {
  if (user.role === 'ADMIN') {
    return <span className={`flex items-center gap-1.5 text-xs font-semibold text-app-text-faint ${alignRight ? 'justify-end' : 'mt-4'}`}><ShieldAlert className="size-4" aria-hidden="true" />Korumalı hesap</span>
  }
  return (
    <div className={`mt-4 flex gap-2 md:mt-0 ${alignRight ? 'justify-end' : ''}`}>
      <button type="button" onClick={onRole} disabled={!user.isActive} className="min-h-9 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary hover:bg-app-surface-muted disabled:cursor-not-allowed disabled:opacity-45">Rolü Değiştir</button>
      <button type="button" onClick={onStatus} className={`min-h-9 rounded-lg px-3 text-xs font-bold ${user.isActive ? 'bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 hover:bg-rose-100 dark:hover:bg-rose-900/60' : 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60'}`}>{user.isActive ? 'Pasifleştir' : 'Etkinleştir'}</button>
    </div>
  )
}

function Pagination({ page, pageCount, onPage }: { page: number; pageCount: number; onPage: (page: number) => void }) {
  return (
    <div className="flex items-center justify-between border-t border-app-border-subtle px-4 py-3 sm:px-6">
      <p className="text-xs text-app-text-subtle">Sayfa {page} / {pageCount}</p>
      <div className="flex gap-2">
        <button type="button" aria-label="Önceki sayfa" disabled={page <= 1} onClick={() => onPage(page - 1)} className="flex size-9 items-center justify-center rounded-lg border border-app-border text-app-text-muted disabled:opacity-40"><ChevronLeft className="size-4" /></button>
        <button type="button" aria-label="Sonraki sayfa" disabled={page >= pageCount} onClick={() => onPage(page + 1)} className="flex size-9 items-center justify-center rounded-lg border border-app-border text-app-text-muted disabled:opacity-40"><ChevronRight className="size-4" /></button>
      </div>
    </div>
  )
}
