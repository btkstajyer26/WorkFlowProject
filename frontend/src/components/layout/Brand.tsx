import { FileText } from 'lucide-react'

export function Brand() {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-200/70 dark:shadow-black/20">
        <FileText aria-hidden="true" className="size-6" strokeWidth={2.2} />
      </div>
      <div className="min-w-0">
        <p className="text-lg font-bold tracking-tight text-app-text-strong">EBYS</p>
        <p className="truncate text-[11px] font-medium text-app-text-subtle">
          İş Akışı ve Onay Yönetim Sistemi
        </p>
      </div>
    </div>
  )
}
