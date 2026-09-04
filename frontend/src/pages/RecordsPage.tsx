import { keepPreviousData, useQueries } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Eye,
  FilePenLine,
  RotateCcw,
  Search,
  SlidersHorizontal,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import { CategoryLoadError } from '../components/records/CategoryLoadError'
import { recordStatusMeta } from '../components/records/recordStatus'
import { useCategories } from '../context/categoryState'
import { useDebouncedSearchParam } from '../hooks/useDebouncedSearchParam'
import { searchRecords, type RecordSearchListItem } from '../api/recordSearch'
import { queryKeys } from '../query/queryKeys'
import type { SystemRoleKey } from '../types/auth'
import type { RecordStatus } from '../types/record'
import { ListLoadingSkeleton } from '../components/feedback/LoadingSkeleton'

const viewConfigs: Record<string, { title: string; statuses: RecordStatus[] }> = {
  taslaklar: { title: 'Taslaklarım', statuses: ['TASLAK'] },
  'duzeltme-bekleyenler': { title: 'Düzeltme Bekleyenler', statuses: ['DUZENLEME_BEKLIYOR'] },
  'onay-asamasindakiler': {
    title: 'Onay Aşamasındakiler',
    statuses: ['BSK_YRD_INCELEMESINDE', 'BASKAN_INCELEMESINDE'],
  },
  sonuclananlar: { title: 'Sonuçlananlar', statuses: ['ONAYLANDI', 'REDDEDILDI'] },
  incelenecekler: { title: 'İncelenecekler', statuses: ['BSK_YRD_INCELEMESINDE'] },
  'baskan-incelemesindekiler': {
    title: 'Başkan İncelemesindekiler',
    statuses: ['BASKAN_INCELEMESINDE'],
  },
  'duzeltmede-olanlar': { title: 'Düzeltmede Olanlar', statuses: ['DUZENLEME_BEKLIYOR'] },
  'onay-bekleyenler': { title: 'Onay Bekleyenler', statuses: ['BASKAN_INCELEMESINDE'] },
  onaylananlar: { title: 'Onaylananlar', statuses: ['ONAYLANDI'] },
  reddedilenler: { title: 'Reddedilenler', statuses: ['REDDEDILDI'] },
}

const dateFormatter = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const filterControlClass =
  'h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-secondary outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60'

const pageSizes = [5, 10, 20]

function isValidDateParam(value: string | null) {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const parsed = new Date(`${value}T00:00:00Z`)
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value
}

/**
 * Ad backend'den `createdByFullName` ile gelir. Alan yoksa kimlik gösterilir;
 * işlem geçmişinden ad türetilmez çünkü geçmiş role göre kırpılabilir.
 */
function formatCreatorName(
  createdBy?: string,
  createdByFullName?: string,
) {
  if (createdByFullName?.trim()) return createdByFullName.trim()
  if (!createdBy) return '—'
  return createdBy
}

type RecordListViewItem = {
  id: string
  title: string
  description: string
  categoryId: number
  category: string
  status: RecordStatus
  createdAt: string
  createdBy: string
  createdByFullName?: string
}

function canEditRecord(systemKey: SystemRoleKey | null, record: RecordListViewItem) {
  return systemKey === 'CALISAN' && (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR')
}

function toRecordListViewItem(record: RecordSearchListItem): RecordListViewItem {
  return {
    id: record.id,
    title: record.title,
    description: record.description,
    categoryId: record.category.id,
    category: record.category.name,
    status: record.status,
    createdBy: record.createdBy,
    createdByFullName: record.createdByFullName,
    createdAt: record.createdAt,
  }
}

export function RecordsPage({ systemKey }: { systemKey: SystemRoleKey | null }) {
  const { categories, status: categoryStatus, reloadCategories } = useCategories()
  const [searchParams, setSearchParams] = useSearchParams()
  const rawView = searchParams.get('gorunum')
  const view = rawView && rawView in viewConfigs ? rawView : null
  const viewConfig = view ? viewConfigs[view] : undefined
  const title = viewConfig?.title ?? (systemKey === 'CALISAN' ? 'Tüm Kayıtlarım' : 'Tüm Kayıtlar')
  const [filtersOpen, setFiltersOpen] = useState(false)

  const search = searchParams.get('q') ?? ''
  const [searchInput, setSearchInput] = useDebouncedSearchParam(searchParams, setSearchParams)
  const creator = searchParams.get('olusturan')?.trim() ?? ''
  const [creatorInput, setCreatorInput] = useDebouncedSearchParam(searchParams, setSearchParams, 'olusturan')
  const categoryParam = searchParams.get('kategori')
  const parsedCategoryId = Number(categoryParam)
  const categoryId: number | 'ALL' = Number.isInteger(parsedCategoryId) && categories.some((item) => item.id === parsedCategoryId)
    ? parsedCategoryId
    : 'ALL'
  const statusParam = searchParams.get('durum')
  const parsedStatus: RecordStatus | 'ALL' =
    statusParam && statusParam in recordStatusMeta ? (statusParam as RecordStatus) : 'ALL'
  const canRefineViewByStatus = !viewConfig || viewConfig.statuses.length > 1
  const status: RecordStatus | 'ALL' =
    parsedStatus !== 'ALL' && canRefineViewByStatus && (!viewConfig || viewConfig.statuses.includes(parsedStatus))
      ? parsedStatus
      : 'ALL'
  const availableStatuses = viewConfig?.statuses ?? (Object.keys(recordStatusMeta) as RecordStatus[])
  const lockedStatus = viewConfig?.statuses.length === 1 ? viewConfig.statuses[0] : null
  const dateFromParam = searchParams.get('baslangic')
  const dateToParam = searchParams.get('bitis')
  const dateFrom = isValidDateParam(dateFromParam) ? dateFromParam! : ''
  const dateTo = isValidDateParam(dateToParam) ? dateToParam! : ''
  const dateRangeInvalid = Boolean(dateFrom && dateTo && dateFrom > dateTo)
  const pageParam = Number(searchParams.get('sayfa'))
  const page = Number.isInteger(pageParam) && pageParam > 0 ? pageParam : 1
  const pageSizeParam = Number(searchParams.get('boyut'))
  const pageSize = pageSizes.includes(pageSizeParam) ? pageSizeParam : 10
  const categoryRevision = categories.map((category) => `${category.id}:${category.name}`).join('|')
  const requestedStatuses: Array<RecordStatus | undefined> = status !== 'ALL'
    ? [status]
    : viewConfig ? viewConfig.statuses : [undefined]
  const groupedStatusQuery = requestedStatuses.length > 1
  const serverQuerySize = groupedStatusQuery ? page * pageSize : pageSize
  const serverQueries = useQueries({
    queries: requestedStatuses.map((requestedStatus) => {
      const query = {
        q: search.trim() || undefined,
        status: requestedStatus,
        categoryId: categoryId === 'ALL' ? undefined : categoryId,
        createdFrom: dateFrom || undefined,
        createdTo: dateTo || undefined,
        creator: creator || undefined,
        page: groupedStatusQuery ? 0 : page - 1,
        size: serverQuerySize,
      }

      return {
        queryKey: queryKeys.records.list({ ...query, categoryRevision }),
        queryFn: () => searchRecords(query, categories),
        enabled: categoryStatus === 'ready' && !dateRangeInvalid,
        placeholderData: keepPreviousData,
        refetchInterval: 30_000,
      }
    }),
  })

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams)
    if (rawView && !viewConfig) nextParams.delete('gorunum')
    if (categoryStatus === 'ready' && categoryParam && categoryId === 'ALL') nextParams.delete('kategori')
    if (statusParam && status === 'ALL') nextParams.delete('durum')
    if (dateFromParam && !dateFrom) nextParams.delete('baslangic')
    if (dateToParam && !dateTo) nextParams.delete('bitis')
    if (!Number.isInteger(pageParam) || pageParam < 1 || pageParam === 1) nextParams.delete('sayfa')
    if (!pageSizes.includes(pageSizeParam) || pageSizeParam === 10) nextParams.delete('boyut')

    if (nextParams.toString() !== searchParams.toString()) setSearchParams(nextParams, { replace: true })
  }, [categoryId, categoryParam, categoryStatus, dateFrom, dateFromParam, dateTo, dateToParam, pageParam, pageSizeParam, rawView, searchParams, setSearchParams, status, statusParam, viewConfig])

  const updateQuery = (updates: Record<string, string | null>, resetPage = true) => {
    const nextParams = new URLSearchParams(searchParams)

    Object.entries(updates).forEach(([key, value]) => {
      if (value) nextParams.set(key, value)
      else nextParams.delete(key)
    })

    if (resetPage) nextParams.delete('sayfa')
    setSearchParams(nextParams, { replace: true })
  }

  const serverRecords = serverQueries
    .flatMap((query) => query.data?.content ?? [])
    .map(toRecordListViewItem)
    .toSorted((left, right) => right.createdAt.localeCompare(left.createdAt))
  const serverTotalElements = serverQueries.reduce(
    (total, query) => total + (query.data?.totalElements ?? 0),
    0,
  )
  const totalRecordCount = serverTotalElements
  const totalPages = Math.max(1, Math.ceil(totalRecordCount / pageSize))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * pageSize
  const visibleRecords: RecordListViewItem[] = groupedStatusQuery
    ? serverRecords.slice(pageStart, pageStart + pageSize)
    : serverRecords

  const recordsPending = categoryStatus !== 'error' && serverQueries.some((query) => query.isPending)
  const recordsError = categoryStatus === 'error' || serverQueries.some((query) => query.isError)
  const activeFilterCount = [categoryId !== 'ALL', status !== 'ALL', Boolean(dateFrom), Boolean(dateTo), Boolean(creator)].filter(Boolean).length

  useEffect(() => {
    if (recordsPending) return
    if (page <= totalPages) return
    const nextParams = new URLSearchParams(searchParams)
    if (totalPages <= 1) nextParams.delete('sayfa')
    else nextParams.set('sayfa', String(totalPages))
    setSearchParams(nextParams, { replace: true })
  }, [page, recordsPending, searchParams, setSearchParams, totalPages])

  const resetFilters = () => {
    setSearchInput('')
    setCreatorInput('')
    updateQuery({ q: null, kategori: null, durum: null, baslangic: null, bitis: null, olusturan: null })
  }

  return (
    <div className="space-y-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">{title}</h1>
          <p className="mt-2 text-sm text-app-text-muted">Erişim kapsamınızdaki kayıtları arayın, filtreleyin ve inceleyin.</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-app-text-muted">
          <span className="flex size-8 items-center justify-center rounded-lg bg-brand-50 dark:bg-brand-900/30 font-bold text-brand-700 dark:text-brand-300">
            {totalRecordCount}
          </span>
          kayıt bulundu
        </div>
      </header>

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm" aria-label="Kayıt listesi">
        <div className="border-b border-app-border p-4 sm:p-5">
          <div className="flex flex-col gap-3 sm:flex-row">
            <label className="relative min-w-0 flex-1">
              <span className="sr-only">Başlık veya içerikle ara</span>
              <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
              <input
                type="search"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Başlık veya içerikte ara..."
                className="h-11 w-full rounded-xl border border-app-border bg-app-surface pl-10 pr-4 text-sm text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
              />
            </label>
            <button
              type="button"
              onClick={() => setFiltersOpen((current) => !current)}
              className="flex h-11 items-center justify-center gap-2 rounded-xl border border-app-border bg-app-surface px-4 text-sm font-bold text-app-text-secondary transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 md:hidden"
              aria-expanded={filtersOpen}
            >
              <SlidersHorizontal className="size-4" aria-hidden="true" />
              Filtreler
              {activeFilterCount > 0 ? (
                <span className="flex size-5 items-center justify-center rounded-full bg-brand-600 text-[10px] text-white">{activeFilterCount}</span>
              ) : null}
            </button>
          </div>

          <div className={`${filtersOpen ? 'grid' : 'hidden'} mt-3 gap-3 md:grid md:grid-cols-2 xl:grid-cols-[1fr_1fr_1fr_1fr_1fr_auto]`}>
            <div>
              <label htmlFor="record-filter-category" className="mb-1.5 block text-xs font-bold text-app-text-muted">Kategori</label>
              <select
                id="record-filter-category"
                aria-label="Kategori"
                value={categoryId}
                onChange={(event) => updateQuery({ kategori: event.target.value === 'ALL' ? null : event.target.value })}
                disabled={categoryStatus !== 'ready'}
                aria-describedby={categoryStatus === 'error' ? 'record-filter-category-load-error' : undefined}
                className={`${filterControlClass} disabled:cursor-wait disabled:bg-app-surface-strong`}
              >
                <option value="ALL">
                  {categoryStatus === 'loading' ? 'Kategoriler yükleniyor…' : 'Tüm kategoriler'}
                </option>
                {categories.map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </select>
              {categoryStatus === 'error' ? (
                <CategoryLoadError id="record-filter-category-load-error" onRetry={reloadCategories} />
              ) : null}
            </div>
            <label>
              <span className="mb-1.5 block text-xs font-bold text-app-text-muted">Durum</span>
              <select
                aria-label="Durum"
                value={lockedStatus ?? status}
                onChange={(event) => updateQuery({ durum: event.target.value === 'ALL' ? null : event.target.value })}
                disabled={Boolean(lockedStatus)}
                className={`${filterControlClass} disabled:cursor-not-allowed disabled:bg-app-surface-strong disabled:text-app-text-subtle`}
              >
                {lockedStatus ? null : <option value="ALL">{viewConfig ? 'Tüm ilgili durumlar' : 'Tüm durumlar'}</option>}
                {availableStatuses.map((value) => (
                  <option key={value} value={value}>{recordStatusMeta[value].label}</option>
                ))}
              </select>
              {lockedStatus ? <span className="mt-1 block text-[11px] text-app-text-subtle">Bu görünümde durum sabittir.</span> : null}
            </label>
            <label>
              <span className="mb-1.5 block text-xs font-bold text-app-text-muted">Oluşturulma başlangıcı</span>
              <div className="relative">
                <CalendarDays className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(event) => updateQuery({ baslangic: event.target.value || null })}
                  max={dateTo || undefined}
                  aria-invalid={dateRangeInvalid}
                  aria-describedby={dateRangeInvalid ? 'record-date-range-error' : undefined}
                  className={`${filterControlClass} pl-9`}
                />
              </div>
            </label>
            <label>
              <span className="mb-1.5 block text-xs font-bold text-app-text-muted">Oluşturulma bitişi</span>
              <div className="relative">
                <CalendarDays className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                <input
                  type="date"
                  value={dateTo}
                  onChange={(event) => updateQuery({ bitis: event.target.value || null })}
                  min={dateFrom || undefined}
                  aria-invalid={dateRangeInvalid}
                  aria-describedby={dateRangeInvalid ? 'record-date-range-error' : undefined}
                  className={`${filterControlClass} pl-9`}
                />
              </div>
            </label>
            <label>
              <span className="mb-1.5 block text-xs font-bold text-app-text-muted">Oluşturan kişi</span>
              <input
                type="search"
                value={creatorInput}
                onChange={(event) => setCreatorInput(event.target.value)}
                placeholder="Ad veya soyad"
                aria-label="Oluşturan kişi"
                className={filterControlClass}
              />
            </label>
            <button
              type="button"
              onClick={resetFilters}
              className="flex h-11 items-center justify-center gap-2 self-start rounded-xl border border-app-border px-3 text-xs font-bold text-app-text-muted transition hover:bg-app-surface-muted hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 md:mt-[22px]"
            >
              <RotateCcw className="size-4" aria-hidden="true" />
              Temizle
            </button>
            {dateRangeInvalid ? (
              <p id="record-date-range-error" className="text-xs font-semibold text-rose-700 dark:text-rose-300 md:col-span-2 xl:col-span-5" role="alert">
                Oluşturulma başlangıcı bitiş tarihinden sonra olamaz.
              </p>
            ) : null}
          </div>
        </div>

        {recordsPending ? (
          <ListLoadingSkeleton label="Kayıtlar yükleniyor" rows={pageSize > 5 ? 6 : pageSize} />
        ) : recordsError ? (
          <div className="flex min-h-72 flex-col items-center justify-center px-6 py-12 text-center" role="alert">
            <h2 className="font-bold text-app-text-strong">Kayıtlar yüklenemedi</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-app-text-muted">Backend bağlantısını kontrol edip yeniden deneyin.</p>
            <button
              type="button"
              onClick={() => void Promise.all(serverQueries.map((query) => query.refetch()))}
              className="mt-4 text-sm font-bold text-brand-700 hover:text-brand-800 dark:text-brand-300 dark:hover:text-brand-200"
            >
              Tekrar dene
            </button>
          </div>
        ) : visibleRecords.length > 0 ? (
          <>
            <div className="hidden overflow-x-auto lg:block">
              <table className="w-full min-w-[840px] table-fixed border-collapse text-left">
                <thead className="bg-app-surface-muted/80 text-xs font-bold text-app-text-subtle">
                  <tr>
                    <th className="w-[26%] px-5 py-3.5">Kayıt</th>
                    <th className="w-[15%] px-4 py-3.5">Kategori</th>
                    <th className="w-[18%] px-4 py-3.5">Durum</th>
                    <th className="w-[16%] px-4 py-3.5">Oluşturulma</th>
                    <th className="w-[17%] px-4 py-3.5">Oluşturan</th>
                    <th className="w-[8%] px-5 py-3.5 text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-app-border-subtle">
                  {visibleRecords.map((record) => (
                    <tr key={record.id} className="group transition-colors hover:bg-brand-50/35 dark:hover:bg-brand-900/20">
                      <td className="px-5 py-4">
                        <Link to={`/kayitlar/${record.id}`} className="block truncate font-bold text-app-text-strong transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
                          {record.title}
                        </Link>
                      </td>
                      <td className="truncate px-4 py-4 text-sm font-medium text-app-text-muted">{record.category}</td>
                      <td className="px-4 py-4"><RecordStatusBadge status={record.status} /></td>
                      <td className="whitespace-nowrap px-4 py-4 text-xs font-medium text-app-text-subtle">{dateFormatter.format(new Date(record.createdAt))}</td>
                      <td className="truncate whitespace-nowrap px-4 py-4 text-xs font-medium text-app-text-secondary">{formatCreatorName(record.createdBy, record.createdByFullName)}</td>
                      <td className="px-5 py-4 text-right">
                        <Link
                          to={canEditRecord(systemKey, record) ? `/kayitlar/${record.id}/duzenle` : `/kayitlar/${record.id}`}
                          className="inline-flex size-10 items-center justify-center rounded-xl border border-app-border text-app-text-subtle transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:bg-app-surface hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                          aria-label={`${record.title} kaydını ${canEditRecord(systemKey, record) ? 'düzenle' : 'görüntüle'}`}
                        >
                          {canEditRecord(systemKey, record) ? <FilePenLine className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="divide-y divide-app-border-subtle lg:hidden">
              {visibleRecords.map((record) => (
                <article key={record.id} className="p-4 sm:p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h2 className="text-sm font-bold leading-5 text-app-text">{record.title}</h2>
                    </div>
                    <RecordStatusBadge status={record.status} />
                  </div>
                  <dl className="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-app-surface-muted p-3 text-xs sm:grid-cols-3">
                    <div>
                      <dt className="font-semibold text-app-text-subtle">Kategori</dt>
                      <dd className="mt-1 font-bold text-app-text-emphasis">{record.category}</dd>
                    </div>
                    <div>
                      <dt className="font-semibold text-app-text-subtle">Oluşturan</dt>
                      <dd className="mt-1 font-bold text-app-text-emphasis">{formatCreatorName(record.createdBy, record.createdByFullName)}</dd>
                    </div>
                    <div>
                      <dt className="font-semibold text-app-text-subtle">Oluşturulma</dt>
                      <dd className="mt-1 font-bold text-app-text-emphasis">{dateFormatter.format(new Date(record.createdAt))}</dd>
                    </div>
                  </dl>
                  <Link
                    to={canEditRecord(systemKey, record) ? `/kayitlar/${record.id}/duzenle` : `/kayitlar/${record.id}`}
                    className="mt-4 flex min-h-10 w-full items-center justify-center gap-2 rounded-xl border border-app-border text-xs font-bold text-app-text-secondary transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                  >
                    {canEditRecord(systemKey, record) ? <FilePenLine className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
                    {canEditRecord(systemKey, record) ? 'Düzenlemeye Devam Et' : 'Kaydı Görüntüle'}
                  </Link>
                </article>
              ))}
            </div>
          </>
        ) : (
          <div className="flex min-h-72 flex-col items-center justify-center px-6 py-12 text-center">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-brand-50 dark:bg-brand-900/30 text-brand-600 dark:text-brand-400">
              <Search className="size-5" aria-hidden="true" />
            </div>
            <h2 className="mt-4 font-bold text-app-text-strong">Eşleşen kayıt bulunamadı</h2>
            <p className="mt-1 max-w-sm text-sm leading-6 text-app-text-muted">Arama ifadenizi veya seçtiğiniz filtreleri değiştirerek tekrar deneyin.</p>
            <button type="button" onClick={resetFilters} className="mt-4 text-sm font-bold text-brand-700 dark:text-brand-300 hover:text-brand-800 dark:hover:text-brand-200">Filtreleri temizle</button>
          </div>
        )}

        <footer className="flex flex-col gap-3 border-t border-app-border bg-app-surface-muted/60 px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <p className="text-xs font-medium text-app-text-subtle">
            {totalRecordCount === 0 ? 0 : pageStart + 1}–{Math.min(pageStart + visibleRecords.length, totalRecordCount)} / {totalRecordCount} kayıt
          </p>
          <div className="flex items-center justify-between gap-3 sm:justify-end">
            <label className="flex items-center gap-2 text-xs font-semibold text-app-text-muted">
              <span className="hidden sm:inline">Sayfa başına</span>
              <select
                value={pageSize}
                onChange={(event) => updateQuery({ boyut: event.target.value === '10' ? null : event.target.value })}
                className="h-9 rounded-lg border border-app-border bg-app-surface px-2 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100 dark:focus:ring-brand-800/60"
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
              </select>
            </label>
            <nav className="flex items-center gap-1" aria-label="Sayfalama">
              <button
                type="button"
                onClick={() => updateQuery({ sayfa: currentPage - 1 === 1 ? null : String(currentPage - 1) }, false)}
                disabled={currentPage === 1}
                className="flex size-9 items-center justify-center rounded-lg border border-app-border bg-app-surface text-app-text-muted transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:text-brand-700 dark:hover:text-brand-300 disabled:cursor-not-allowed disabled:opacity-40"
                aria-label="Önceki sayfa"
              >
                <ChevronLeft className="size-4" aria-hidden="true" />
              </button>
              <span className="min-w-16 text-center text-xs font-bold text-app-text-secondary">{currentPage} / {totalPages}</span>
              <button
                type="button"
                onClick={() => updateQuery({ sayfa: String(Math.min(totalPages, currentPage + 1)) }, false)}
                disabled={currentPage === totalPages}
                className="flex size-9 items-center justify-center rounded-lg border border-app-border bg-app-surface text-app-text-muted transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:text-brand-700 dark:hover:text-brand-300 disabled:cursor-not-allowed disabled:opacity-40"
                aria-label="Sonraki sayfa"
              >
                <ChevronRight className="size-4" aria-hidden="true" />
              </button>
            </nav>
          </div>
        </footer>
      </section>
    </div>
  )
}
