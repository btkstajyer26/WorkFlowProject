import {
  ArrowUpRight,
  CheckCircle2,
  Clock3,
  FilePenLine,
  Files,
  type LucideIcon,
} from 'lucide-react'
import { Link } from 'react-router'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import { useWorkflow } from '../context/workflowState'
import type { AuthUser, UserRole } from '../types/auth'
import type { RecordStatus } from '../types/record'

type DashboardCard = {
  label: string
  statuses: RecordStatus[]
  tone: string
  icon: LucideIcon
  view: string
}

const dashboardCards: Record<UserRole, DashboardCard[]> = {
  CALISAN: [
    { label: 'Taslaklarım', statuses: ['TASLAK'], tone: 'text-brand-600 bg-brand-50', icon: Files, view: 'taslaklar' },
    { label: 'Düzeltme bekleyen', statuses: ['DUZENLEME_BEKLIYOR'], tone: 'text-amber-600 bg-amber-50', icon: FilePenLine, view: 'duzeltme-bekleyenler' },
    { label: 'Onay aşamasında', statuses: ['BSK_YRD_INCELEMESINDE', 'BASKAN_INCELEMESINDE'], tone: 'text-blue-600 bg-blue-50', icon: Clock3, view: 'onay-asamasindakiler' },
    { label: 'Sonuçlananlar', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-emerald-600 bg-emerald-50', icon: CheckCircle2, view: 'sonuclananlar' },
  ],
  BASKAN_YARDIMCISI: [
    { label: 'İncelenecekler', statuses: ['BSK_YRD_INCELEMESINDE'], tone: 'text-blue-600 bg-blue-50', icon: Files, view: 'incelenecekler' },
    { label: 'Başkan incelemesinde', statuses: ['BASKAN_INCELEMESINDE'], tone: 'text-amber-600 bg-amber-50', icon: Clock3, view: 'baskan-incelemesindekiler' },
    { label: 'Düzeltmede olanlar', statuses: ['DUZENLEME_BEKLIYOR'], tone: 'text-brand-600 bg-brand-50', icon: FilePenLine, view: 'duzeltmede-olanlar' },
    { label: 'Sonuçlananlar', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-emerald-600 bg-emerald-50', icon: CheckCircle2, view: 'sonuclananlar' },
  ],
  BASKAN: [
    { label: 'Onay bekleyenler', statuses: ['BASKAN_INCELEMESINDE'], tone: 'text-amber-600 bg-amber-50', icon: Clock3, view: 'onay-bekleyenler' },
    { label: 'Onaylananlar', statuses: ['ONAYLANDI'], tone: 'text-emerald-600 bg-emerald-50', icon: CheckCircle2, view: 'onaylananlar' },
    { label: 'Reddedilenler', statuses: ['REDDEDILDI'], tone: 'text-rose-600 bg-rose-50', icon: FilePenLine, view: 'reddedilenler' },
    { label: 'Toplam sonuçlanan', statuses: ['ONAYLANDI', 'REDDEDILDI'], tone: 'text-brand-600 bg-brand-50', icon: Files, view: 'sonuclananlar' },
  ],
  ADMIN: [],
}

export function DashboardPage({ user }: { user: AuthUser }) {
  const { visibleRecords } = useWorkflow()
  const cards = dashboardCards[user.role]
  const recentRecords = [...visibleRecords]
    .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
    .slice(0, 3)

  return (
    <div className="space-y-5">
      <header>
        <p className="text-sm font-semibold text-brand-600">Dashboard</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
          Hoş geldiniz, {user.firstName} <span aria-hidden="true">👋</span>
        </h1>
        <p className="mt-2 text-sm text-slate-600 sm:text-base">
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
              className="group rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 sm:p-5"
            >
              <div className={`flex size-10 items-center justify-center rounded-xl ${card.tone}`}>
                <Icon className="size-5" aria-hidden="true" />
              </div>
              <p className="mt-4 text-xs font-semibold text-slate-500 sm:text-sm">{card.label}</p>
              <div className="mt-1 flex items-end justify-between gap-2">
                <p className="text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
                  {visibleRecords.filter((record) => card.statuses.includes(record.status)).length}
                </p>
                <ArrowUpRight className="size-4 text-slate-300 transition group-hover:text-brand-500" aria-hidden="true" />
              </div>
            </Link>
          )
        })}
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4 sm:px-6">
          <div>
            <h2 className="font-bold text-slate-900">Son Kayıtlar</h2>
            <p className="mt-0.5 text-xs text-slate-500">Erişim kapsamınızdaki en güncel hareketler</p>
          </div>
          <Link
            to="/kayitlar"
            className="text-xs font-bold text-brand-600 transition hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            Tümünü gör
          </Link>
        </div>
        <div className="divide-y divide-slate-100">
          {recentRecords.map((record, index) => (
            <div key={record.id} className="flex items-center gap-3 px-4 py-4 transition hover:bg-slate-50/70 sm:px-6">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-xs font-bold text-slate-500">
                {index + 1}
              </div>
              <div className="min-w-0 flex-1">
                <Link
                  to={`/kayitlar/${record.id}`}
                  className="block truncate text-sm font-semibold text-slate-900 transition hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                >
                  {record.title}
                </Link>
                <p className="mt-1 text-xs text-slate-500">{record.recordNumber}</p>
              </div>
              <span className="hidden sm:inline-flex"><RecordStatusBadge status={record.status} /></span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
