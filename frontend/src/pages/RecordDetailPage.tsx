import {
  ArrowLeft,
  CalendarDays,
  Download,
  FileText,
  FolderOpen,
} from 'lucide-react'
import { Link, Navigate, useParams } from 'react-router'
import { RecordActionPanel } from '../components/records/RecordActionPanel'
import { RecordHistoryDisclosure, RecordNoteDisclosure } from '../components/records/RecordDetailDisclosures'
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

  const history = record.history.toReversed()
  const latestEvent = history[0]
  const latestNotedEvent = latestEvent?.note?.trim() ? latestEvent : undefined

  return (
    <article className="mx-auto max-w-[1400px] space-y-4">
      <Link
        to="/kayitlar"
        className="inline-flex items-center gap-2 text-[15px] font-bold text-app-text-muted transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Kayıtlara dön
      </Link>

      <header className="space-y-3 pb-1 pt-1">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <h1 className="min-w-0 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">{record.title}</h1>
          <RecordStatusBadge status={record.status} />
        </div>

        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-[15px] text-app-text-muted">
          <span className="inline-flex items-center gap-2">
            <FolderOpen className="size-4 text-app-text-faint" aria-hidden="true" />
            {record.category}
          </span>
          <span className="text-app-text-faint" aria-hidden="true">•</span>
          <span className="inline-flex items-center gap-2">
            <CalendarDays className="size-4 text-app-text-faint" aria-hidden="true" />
            {dateTimeFormatter.format(new Date(record.createdAt))}
          </span>
        </div>
      </header>

      <RecordActionPanel record={record} role={role} />

      <section className="rounded-xl border border-app-border bg-app-surface px-5 py-5 sm:px-6 sm:py-6">
        <div>
          <h2 className="text-base font-bold text-app-text">Kayıt Açıklaması</h2>
          <p className="mt-4 whitespace-pre-line text-[15px] leading-7 text-app-text-secondary">{record.description}</p>
        </div>

        <div className="mt-6 border-t border-app-border-subtle pt-6">
          <h2 className="text-base font-bold text-app-text">
            Ek Dosyalar <span className="font-medium text-app-text-subtle">({record.attachments.length})</span>
          </h2>

          {record.attachments.length > 0 ? (
            <ul className="mt-4 space-y-2">
              {record.attachments.map((attachment) => (
                <li key={attachment.id} className="flex items-center gap-3 rounded-lg border border-app-border px-3 py-2.5 sm:px-4">
                  <FileText className="size-4 shrink-0 text-app-text-faint" aria-hidden="true" />
                  <div className="flex min-w-0 flex-1 flex-col gap-0.5 sm:flex-row sm:items-center sm:gap-3">
                    <p className="truncate text-[15px] font-bold text-app-text-emphasis">{attachment.name}</p>
                    <p className="shrink-0 text-sm text-app-text-subtle">{attachment.size}</p>
                  </div>
                  <button
                    type="button"
                    className="flex size-9 shrink-0 items-center justify-center rounded-lg text-app-text-subtle transition hover:bg-brand-50 hover:text-brand-700 dark:hover:bg-brand-900/30 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                    aria-label={`${attachment.name} dosyasını indir`}
                  >
                    <Download className="size-4" aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-4 rounded-lg border border-dashed border-app-border px-4 py-5 text-center text-sm text-app-text-subtle">
              Bu kayda eklenmiş dosya bulunmuyor.
            </p>
          )}
        </div>
      </section>

      {latestNotedEvent ? <RecordNoteDisclosure item={latestNotedEvent} /> : null}
      <RecordHistoryDisclosure items={history} />
    </article>
  )
}
