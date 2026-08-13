import { ChevronRight, History, MessageSquareText } from 'lucide-react'
import type { RecordHistoryItem } from '../../types/record'

const dateTimeFormatter = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function RecordNoteDisclosure({ item }: { item: RecordHistoryItem }) {
  return (
    <details className="group rounded-xl border border-app-border bg-app-surface">
      <summary className="flex min-h-16 cursor-pointer list-none items-center gap-3 px-4 py-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 [&::-webkit-details-marker]:hidden sm:px-5">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
          <MessageSquareText className="size-4" aria-hidden="true" />
        </span>
        <div className="min-w-0 flex-1">
          <h2 className="text-base font-bold text-app-text">Son İşlem Notu</h2>
          <p className="mt-0.5 truncate text-sm text-app-text-subtle">{item.actor} · {item.role}</p>
        </div>
        <DisclosureStateLabel />
      </summary>
      <div className="border-t border-app-border-subtle px-4 py-4 sm:px-5">
        <p className="whitespace-pre-wrap text-[15px] leading-6 text-app-text-secondary">{item.note}</p>
        <time className="mt-3 block text-xs text-app-text-subtle">{dateTimeFormatter.format(new Date(item.date))}</time>
      </div>
    </details>
  )
}

export function RecordHistoryDisclosure({ items }: { items: RecordHistoryItem[] }) {
  return (
    <details className="group rounded-xl border border-app-border bg-app-surface">
      <summary className="flex min-h-16 cursor-pointer list-none items-center gap-3 px-4 py-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 [&::-webkit-details-marker]:hidden sm:px-5">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
          <History className="size-4" aria-hidden="true" />
        </span>
        <div className="min-w-0 flex-1 sm:flex sm:items-baseline sm:gap-3">
          <h2 className="text-base font-bold text-app-text">İşlem Geçmişi</h2>
          <p className="mt-0.5 text-sm text-app-text-subtle sm:mt-0">{items.length} hareket</p>
        </div>
        <DisclosureStateLabel />
      </summary>

      <div className="border-t border-app-border-subtle px-4 py-1 sm:px-5">
        {items.length > 0 ? (
          <ol className="divide-y divide-app-border-subtle">
            {items.map((item) => (
              <li key={item.id} className="grid gap-2 py-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:gap-6">
                <div className="min-w-0">
                  <p className="text-[15px] font-bold text-app-text-emphasis">{item.action}</p>
                  <p className="mt-1 text-sm text-app-text-subtle">{item.actor} · {item.role}</p>
                  {item.note ? (
                    <p className="mt-3 border-l-2 border-brand-300 pl-3 text-sm leading-6 text-app-text-muted dark:border-brand-700">
                      {item.note}
                    </p>
                  ) : null}
                </div>
                <time className="text-xs text-app-text-subtle sm:text-right">{dateTimeFormatter.format(new Date(item.date))}</time>
              </li>
            ))}
          </ol>
        ) : (
          <p className="py-4 text-sm text-app-text-subtle">Bu kayıt için henüz işlem bulunmuyor.</p>
        )}
      </div>
    </details>
  )
}

function DisclosureStateLabel() {
  return (
    <>
      <span className="hidden text-xs font-bold text-brand-700 dark:text-brand-300 sm:inline">
        <span className="group-open:hidden">Görüntüle</span>
        <span className="hidden group-open:inline">Gizle</span>
      </span>
      <ChevronRight className="size-4 shrink-0 text-brand-600 transition-transform group-open:rotate-90 dark:text-brand-300" aria-hidden="true" />
    </>
  )
}
