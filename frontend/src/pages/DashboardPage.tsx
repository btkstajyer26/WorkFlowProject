import {
  ArrowUpRight,
  CheckCircle2,
  Clock3,
  FilePenLine,
  Files,
  type LucideIcon,
} from 'lucide-react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { searchRecords } from '../api/recordSearch'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import { useCategories } from '../context/categoryState'
import { queryKeys } from '../query/queryKeys'
import type { AuthUser, SystemRoleKey } from '../types/auth'
import type { RecordStatus } from '../types/record'

type DashboardCard = {
  label: string
  statuses: RecordStatus[]
  tone: string
  icon: LucideIcon
  view: string
}

const dashboardCards: Record<SystemRoleKey, DashboardCard[]> = {
  CALISAN: [
    { label: 'Taslaklarım', statuses: ['TASLAK'], tone: 'text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-900/30', icon: Files, view: 'taslaklar' },
    { label: 'Düzeltme bekleyen', statuses: ['DUZENLEME_BEKLIYOR'], tone: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40', icon: FilePenLine, view: 'duzeltme-bekleyenler' },
    { label: 'Onay aşamasında', statuses: ['BSK_YRD_INCELEMESINDE', 'BASKAN_INCELEMESINDE'], tone: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40', icon: Clock3, view: 'onay-asamasindakiler' },
    { label: 'Sonuçlananlar', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40', icon: CheckCircle2, view: 'sonuclananlar' },
  ],
  BASKAN_YARDIMCISI: [
    { label: 'İncelenecekler', statuses: ['BSK_YRD_INCELEMESINDE'], tone: 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40', icon: Files, view: 'incelenecekler' },
    { label: 'Başkan incelemesinde', statuses: ['BASKAN_INCELEMESINDE'], tone: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40', icon: Clock3, view: 'baskan-incelemesindekiler' },
    { label: 'Düzeltmede olanlar', statuses: ['DUZENLEME_BEKLIYOR'], tone: 'text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-900/30', icon: FilePenLine, view: 'duzeltmede-olanlar' },
    { label: 'Sonuçlananlar', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40', icon: CheckCircle2, view: 'sonuclananlar' },
  ],
  BASKAN: [
    { label: 'Onay bekleyenler', statuses: ['BASKAN_INCELEMESINDE'], tone: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40', icon: Clock3, view: 'onay-bekleyenler' },
    { label: 'Onaylananlar', statuses: ['ONAYLANDI'], tone: 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40', icon: CheckCircle2, view: 'onaylananlar' },
    { label: 'Reddedilenler', statuses: ['REDDEDILDI'], tone: 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40', icon: FilePenLine, view: 'reddedilenler' },
    { label: 'Toplam sonuçlanan', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-900/30', icon: Files, view: 'sonuclananlar' },
  ],
  ADMIN: [],
}

export function DashboardPage({ user }: { user: AuthUser }) {
  const { categories, status: categoryStatus } = useCategories()
  const cards = user.systemKey ? dashboardCards[user.systemKey] : []
  const categoryRevision = categories.map((category) => `${category.id}:${category.name}`).join('|')
  const dashboardStatuses = [...new Set(cards.flatMap((card) => card.statuses))]
  const countQueries = useQueries({
    queries: dashboardStatuses.map((status) => ({
      queryKey: queryKeys.records.list({ scope: 'dashboard-count', status, categoryRevision }),
      queryFn: () => searchRecords({ status, page: 0, size: 1 }, categories),
      enabled: categoryStatus === 'ready',
      refetchInterval: 30_000,
    })),
  })
  const recentRecordsQuery = useQuery({
    queryKey: queryKeys.records.list({ scope: 'dashboard-recent', categoryRevision }),
    queryFn: () => searchRecords({ page: 0, size: 3 }, categories),
    enabled: categoryStatus === 'ready',
    refetchInterval: 30_000,
  })
  const serverCountByStatus = new Map(
    dashboardStatuses.map((status, index) => [status, countQueries[index]?.data?.totalElements ?? 0]),
  )
  const dashboardPending = categoryStatus === 'loading' || countQueries.some((query) => query.isPending)
  const dashboardError = categoryStatus === 'error' || countQueries.some((query) => query.isError) || recentRecordsQuery.isError
  const recentRecords = recentRecordsQuery.data?.content ?? []

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">
          Hoş geldiniz, {user.firstName} {user.lastName} <span aria-hidden="true">👋</span>
        </h1>
        <p className="mt-2 text-sm text-app-text-muted sm:text-base">
          Kayıt süreçlerinizi ve bekleyen işlemlerinizi buradan takip edebilirsiniz.
        </p>
      </header>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="Kayıt özeti">
        {cards.map((card) => {
          const Icon = card.icon
          return (
            <Link
              key={card.label}
              to={`/kayitlar?gorunum=${card.view}`}
              className="group rounded-2xl border border-app-border bg-app-surface p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-200 dark:hover:border-brand-700/60 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:p-5"
            >
              <div className={`flex size-10 items-center justify-center rounded-xl ${card.tone}`}>
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <p className="mt-4 text-xs font-semibold text-app-text-subtle sm:text-sm">{card.label}</p>
              <div className="mt-1 flex items-end justify-between gap-2">
                <p className="text-2xl font-bold tracking-tight text-app-text sm:text-3xl">
                  {dashboardPending
                    ? '—'
                    : card.statuses.reduce((total, status) => total + (serverCountByStatus.get(status) ?? 0), 0)}
                </p>
                <ArrowUpRight className="size-4 text-app-text-disabled transition group-hover:text-brand-500" aria-hidden="true" />
              </div>
            </Link>
          )
        })}
      </section>

      {dashboardError ? (
        <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-800 dark:border-rose-900/70 dark:bg-rose-950/40 dark:text-rose-200" role="alert">
          Kayıt özeti yenilenemedi. Kayıtlar sayfasından güncel listeye erişebilirsiniz.
        </p>
      ) : null}

      <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
        <div className="flex items-center justify-between border-b border-app-border-subtle px-4 py-4 sm:px-6">
          <div>
            <h2 className="font-bold text-app-text-strong">Son Kayıtlar</h2>
          </div>
          <Link
            to="/kayitlar"
            className="text-xs font-bold text-brand-600 dark:text-brand-400 transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            Tümünü gör
          </Link>
        </div>
        <div className="divide-y divide-app-border-subtle">
          {recentRecords.map((record, index) => (
            <Link
              key={record.id}
              to={`/kayitlar/${record.id}`}
              className="group flex items-center gap-3 px-4 py-4 transition hover:bg-app-surface-muted/70 focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-brand-500 sm:px-6"
            >
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-app-surface-strong text-xs font-bold text-app-text-subtle">
                {index + 1}
              </div>
              <div className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-app-text-strong transition group-hover:text-brand-700 dark:group-hover:text-brand-300">
                  {record.title}
                </span>
              </div>
              <span className="hidden sm:inline-flex"><RecordStatusBadge status={record.status} /></span>
            </Link>
          ))}
          {!recentRecordsQuery.isPending && recentRecords.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-app-text-subtle sm:px-6">Henüz görüntülenecek kayıt yok.</p>
          ) : null}
        </div>
      </section>
    </div>
  )
}
