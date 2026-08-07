import { FilePlus2 } from 'lucide-react'

export function NewRecordPage() {
  return (
    <div className="space-y-5">
      <header>
        <p className="text-sm font-semibold text-brand-600 dark:text-brand-400">Kayıtlar</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Yeni Kayıt</h1>
        <p className="mt-2 text-sm text-app-text-subtle">Kayıt formu ayrı bir çalışma panelinde açıldı.</p>
      </header>
      <section className="flex min-h-80 flex-col items-center justify-center rounded-2xl border border-app-border bg-app-surface p-6 text-center shadow-sm">
        <div className="flex size-14 items-center justify-center rounded-2xl bg-brand-50 dark:bg-brand-900/30 text-brand-600 dark:text-brand-400">
          <FilePlus2 className="size-6" aria-hidden="true" />
        </div>
        <h2 className="mt-4 font-bold text-app-text-strong">Yeni kayıt hazırlanıyor</h2>
        <p className="mt-1 max-w-sm text-sm leading-6 text-app-text-subtle">
          Formu küçülttüğünüzde bu sayfadan ayrılmadan kayıtlarınızı inceleyebilirsiniz.
        </p>
      </section>
    </div>
  )
}
