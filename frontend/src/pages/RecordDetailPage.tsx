import {
  ArrowLeft,
  CalendarDays,
  Download,
  FileText,
  FolderOpen,
  History,
  type LucideIcon,
  UserRound,
} from 'lucide-react'
import { Link, Navigate, useParams } from 'react-router'
import { RecordActionPanel } from '../components/records/RecordActionPanel'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import { useWorkflow } from '../context/workflowState'
import type { UserRole } from '../types/auth'

const dateTimeFormatter = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function RecordDetailPage({ role }: { role: UserRole }) {
  const { recordId } = useParams()
  const { records, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === recordId)

  if (!record) return <Navigate to={records.some((item) => item.id === recordId) ? '/403' : '/404'} replace />

  return (
    <div className="space-y-5">
      <Link
        to="/kayitlar"
        className="inline-flex items-center gap-2 text-sm font-bold text-app-text-muted transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Kayıtlara dön
      </Link>

      <header className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <p className="text-sm font-bold text-brand-600 dark:text-brand-400">{record.recordNumber}</p>
            <h1 className="mt-2 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">{record.title}</h1>
            <p className="mt-3 text-sm text-app-text-muted">Son işlem: {record.lastAction}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <RecordStatusBadge status={record.status} />
          </div>
        </div>

        <div className="mt-6 grid gap-3 border-t border-app-border-subtle pt-5 sm:grid-cols-2 xl:grid-cols-4">
          <DetailMeta icon={FolderOpen} label="Kategori" value={record.category} />
          <DetailMeta icon={UserRound} label="Oluşturan" value={record.createdBy} />
          <DetailMeta icon={UserRound} label="Atanan" value={record.assignedTo ?? 'İşlem tamamlandı'} />
          <DetailMeta icon={CalendarDays} label="Oluşturulma" value={dateTimeFormatter.format(new Date(record.createdAt))} />
        </div>
      </header>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.65fr)]">
        <div className="space-y-5">
          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
                <FileText className="size-5" aria-hidden="true" />
              </span>
              <h2 className="font-bold text-app-text">Kayıt Açıklaması</h2>
            </div>
            <p className="mt-5 whitespace-pre-line text-sm leading-7 text-app-text-secondary">{record.description}</p>
          </section>

          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="flex size-10 items-center justify-center rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300">
                  <FolderOpen className="size-5" aria-hidden="true" />
                </span>
                <div>
                  <h2 className="font-bold text-app-text">Ek Dosyalar</h2>
                  <p className="mt-0.5 text-xs text-app-text-subtle">{record.attachments.length} dosya</p>
                </div>
              </div>
            </div>

            {record.attachments.length > 0 ? (
              <ul className="mt-5 space-y-2">
                {record.attachments.map((attachment) => (
                  <li key={attachment.id} className="flex items-center gap-3 rounded-xl border border-app-border p-3">
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-app-surface-muted text-app-text-subtle">
                      <FileText className="size-4" aria-hidden="true" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-bold text-app-text-emphasis">{attachment.name}</p>
                      <p className="mt-0.5 text-xs text-app-text-subtle">{attachment.size}</p>
                    </div>
                    <button
                      type="button"
                      className="flex size-10 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                      aria-label={`${attachment.name} dosyasını indir`}
                    >
                      <Download className="size-4" aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-5 rounded-xl border border-dashed border-app-border bg-app-surface-muted px-4 py-6 text-center text-sm text-app-text-subtle">
                Bu kayda eklenmiş dosya bulunmuyor.
              </p>
            )}
          </section>
        </div>

        <div className="space-y-5">
          <RecordActionPanel record={record} role={role} />

          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300">
                <History className="size-5" aria-hidden="true" />
              </span>
              <div>
                <h2 className="font-bold text-app-text">İşlem Geçmişi</h2>
                <p className="mt-0.5 text-xs text-app-text-subtle">Kaydın tüm hareketleri</p>
              </div>
            </div>

            <ol className="mt-6 space-y-0">
              {[...record.history].reverse().map((item, index) => (
                <li key={item.id} className="relative flex gap-3 pb-6 last:pb-0">
                  {index < record.history.length - 1 ? <span className="absolute left-[7px] top-5 h-[calc(100%-8px)] w-px bg-app-surface-selected" /> : null}
                  <span className="relative mt-1.5 size-3.5 shrink-0 rounded-full border-[3px] border-white bg-brand-500 ring-1 ring-brand-200 dark:ring-brand-700/60" />
                  <div className="min-w-0">
                    <p className="text-sm font-bold text-app-text-emphasis">{item.action}</p>
                    <p className="mt-1 text-xs text-app-text-subtle">{item.actor} · {item.role}</p>
                    {item.note ? <p className="mt-2 rounded-lg bg-app-surface-muted px-3 py-2 text-xs leading-5 text-app-text-muted">{item.note}</p> : null}
                    <time className="mt-2 block text-[11px] font-medium text-app-text-subtle">{dateTimeFormatter.format(new Date(item.date))}</time>
                  </div>
                </li>
              ))}
            </ol>
          </section>
        </div>
      </div>
    </div>
  )
}

function DetailMeta({
  icon: Icon,
  label,
  value,
}: {
  icon: LucideIcon
  label: string
  value: string
}) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-app-surface-muted p-3">
      <Icon className="mt-0.5 size-4 shrink-0 text-app-text-faint" aria-hidden="true" />
      <div className="min-w-0">
        <p className="text-[11px] font-bold uppercase tracking-wide text-app-text-subtle">{label}</p>
        <p className="mt-1 text-xs font-bold leading-5 text-app-text-emphasis">{value}</p>
      </div>
    </div>
  )
}
