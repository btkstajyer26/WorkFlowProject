import { Search, UsersRound } from 'lucide-react'
import { useEffect, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import { useAdmin } from '../../context/adminState'
import { useDebouncedSearchParam } from '../../hooks/useDebouncedSearchParam'

const pageSize = 8

export function AdminLogsPage() {
  const { logs } = useAdmin()
  const [searchParams, setSearchParams] = useSearchParams()
  const query = searchParams.get('q')?.trim().toLocaleLowerCase('tr-TR') ?? ''
  const [searchInput, setSearchInput] = useDebouncedSearchParam(searchParams, setSearchParams)
  const rawPage = Number(searchParams.get('sayfa'))
  const requestedPage = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1
  const filteredLogs = useMemo(() => logs.filter((log) => {
    const searchable = `${log.actionLabel} ${log.actor} ${log.target} ${log.description} ${log.recordNumber ?? ''}`.toLocaleLowerCase('tr-TR')
    return !query || searchable.includes(query)
  }), [logs, query])
  const pageCount = Math.max(1, Math.ceil(filteredLogs.length / pageSize))
  const currentPage = Math.min(requestedPage, pageCount)
  const visibleLogs = filteredLogs.slice((currentPage - 1) * pageSize, currentPage * pageSize)

  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    if (!Number.isInteger(rawPage) || rawPage <= 1) next.delete('sayfa')
    else if (rawPage > pageCount) {
      if (pageCount <= 1) next.delete('sayfa')
      else next.set('sayfa', String(pageCount))
    }
    if (next.toString() !== searchParams.toString()) setSearchParams(next, { replace: true })
  }, [pageCount, rawPage, searchParams, setSearchParams])

  const setPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page <= 1) next.delete('sayfa')
    else next.set('sayfa', String(page))
    setSearchParams(next)
  }

  return (
    <div className="space-y-5">
      <header>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">İşlem Kayıtları</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Hesap oluşturma, rol ve erişim durumu değişikliklerini inceleyin.</p>
        </div>
      </header>

      <section className="rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm" aria-label="Log filtreleri">
        <div>
          <label className="relative block">
            <span className="sr-only">İşlem kaydı ara</span>
            <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
            <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Kullanıcı veya işlem ara" className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface pl-10 pr-3 text-sm text-app-text-strong outline-none placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60" />
          </label>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle sm:px-6">{filteredLogs.length} işlem kaydı bulundu</div>
        {visibleLogs.length ? (
          <div className="divide-y divide-app-border-subtle">
            {visibleLogs.map((log) => (
              <article key={log.id} className="grid gap-3 px-4 py-4 sm:px-6 lg:grid-cols-[minmax(0,1fr)_12rem] lg:items-start">
                <div className="flex min-w-0 gap-3">
                  <span className="mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
                    <UsersRound className="size-[18px]" aria-hidden="true" />
                  </span>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-sm font-bold text-app-text">{log.actionLabel}</h2>
                      <span className="rounded-full bg-app-surface-strong px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-app-text-muted">Kullanıcı</span>
                    </div>
                    <p className="mt-1 text-sm font-semibold text-app-text-secondary">{log.target}</p>
                    <p className="mt-1 text-xs leading-5 text-app-text-subtle">{log.description}</p>
                    <p className="mt-2 text-xs text-app-text-subtle">İşlemi yapan: <strong className="text-app-text-secondary">{log.actor}</strong></p>
                  </div>
                </div>
                <time className="text-xs text-app-text-subtle lg:text-right">{formatDateTime(log.createdAt)}</time>
              </article>
            ))}
          </div>
        ) : (
          <div className="px-5 py-14 text-center"><Search className="mx-auto size-8 text-app-text-disabled" /><h2 className="mt-3 font-bold text-app-text-strong">İşlem kaydı bulunamadı</h2><p className="mt-1 text-sm text-app-text-subtle">Filtreleri değiştirerek tekrar deneyin.</p></div>
        )}
        <div className="flex items-center justify-between border-t border-app-border-subtle px-4 py-3 sm:px-6">
          <p className="text-xs text-app-text-subtle">Sayfa {currentPage} / {pageCount}</p>
          <div className="flex gap-2">
            <button type="button" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)} className="min-h-9 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary disabled:opacity-40">Önceki</button>
            <button type="button" disabled={currentPage >= pageCount} onClick={() => setPage(currentPage + 1)} className="min-h-9 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary disabled:opacity-40">Sonraki</button>
          </div>
        </div>
      </section>
    </div>
  )
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
