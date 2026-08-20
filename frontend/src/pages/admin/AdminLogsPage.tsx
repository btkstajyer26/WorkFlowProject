import { Search, UsersRound } from 'lucide-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useSearchParams } from 'react-router'
import { listAdminAuditLogs } from '../../api/admin'
import { queryKeys } from '../../query/queryKeys'
import { ListLoadingSkeleton } from '../../components/feedback/LoadingSkeleton'
import type { AdminLogType } from '../../types/admin'

const pageSize = 8

export function AdminLogsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const rawPage = Number(searchParams.get('sayfa'))
  const requestedPage = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1
  const logType: AdminLogType = searchParams.get('tur') === 'RECORD' ? 'RECORD' : 'USER'
  const serverQuery = { page: requestedPage - 1, size: pageSize, type: logType }
  const logsQuery = useQuery({
    queryKey: queryKeys.admin.auditLogs.list(serverQuery),
    queryFn: () => listAdminAuditLogs(serverQuery),
    placeholderData: keepPreviousData,
  })
  const pageCount = Math.max(1, logsQuery.data?.totalPages ?? 1)
  const currentPage = Math.min(requestedPage, pageCount)
  const visibleLogs = logsQuery.data?.content ?? []
  const totalLogCount = logsQuery.data?.totalElements ?? 0

  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    next.delete('q')
    if (!Number.isInteger(rawPage) || rawPage <= 1) next.delete('sayfa')
    else if (!logsQuery.isPending && rawPage > pageCount) {
      if (pageCount <= 1) next.delete('sayfa')
      else next.set('sayfa', String(pageCount))
    }
    if (next.toString() !== searchParams.toString()) setSearchParams(next, { replace: true })
  }, [logsQuery.isPending, pageCount, rawPage, searchParams, setSearchParams])

  const setPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page <= 1) next.delete('sayfa')
    else next.set('sayfa', String(page))
    setSearchParams(next)
  }

  const setLogType = (type: AdminLogType) => {
    const next = new URLSearchParams(searchParams)
    next.delete('sayfa')
    if (type === 'USER') next.delete('tur')
    else next.set('tur', type)
    setSearchParams(next)
  }

  return (
    <div className="space-y-5">
      <header>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">İşlem Kayıtları</h1>
          <p className="mt-2 text-sm leading-6 text-app-text-muted">Hesap işlemlerini, giriş/çıkışları ve API isteklerini (saat, hata kodu, işlemi yapan) inceleyin.</p>
        </div>
      </header>

      <div className="flex flex-wrap gap-2" role="tablist" aria-label="Log türü">
        <button type="button" onClick={() => setLogType('USER')} className={`min-h-10 rounded-lg border px-4 text-xs font-bold ${logType === 'USER' ? 'border-brand-400 bg-brand-50 text-brand-800 dark:bg-brand-900/40 dark:text-brand-200' : 'border-app-border text-app-text-secondary hover:bg-app-surface-muted'}`}>Kullanıcı işlemleri</button>
        <button type="button" onClick={() => setLogType('RECORD')} className={`min-h-10 rounded-lg border px-4 text-xs font-bold ${logType === 'RECORD' ? 'border-brand-400 bg-brand-50 text-brand-800 dark:bg-brand-900/40 dark:text-brand-200' : 'border-app-border text-app-text-secondary hover:bg-app-surface-muted'}`}>Evrak ve admin işlemleri</button>
      </div>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        <div className="border-b border-app-border-subtle px-4 py-3 text-xs font-semibold text-app-text-subtle sm:px-6">{totalLogCount} işlem kaydı bulundu</div>
        {logsQuery.isPending ? (
          <ListLoadingSkeleton label="İşlem kayıtları yükleniyor" rows={pageSize} />
        ) : logsQuery.isError ? (
          <div className="px-5 py-14 text-center" role="alert">
            <h2 className="font-bold text-app-text-strong">İşlem kayıtları yüklenemedi</h2>
            <button type="button" onClick={() => void logsQuery.refetch()} className="mt-4 min-h-10 rounded-lg border border-app-border px-4 text-xs font-bold text-app-text-secondary hover:bg-app-surface-muted">Tekrar dene</button>
          </div>
        ) : visibleLogs.length ? (
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
                    {log.httpStatus != null ? (
                      <p className="mt-1 text-xs text-app-text-subtle">
                        HTTP {log.httpStatus}{log.errorCode ? ` · ${log.errorCode}` : ''}{log.httpMethod && log.requestPath ? ` · ${log.httpMethod} ${log.requestPath}` : ''}
                      </p>
                    ) : null}
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
