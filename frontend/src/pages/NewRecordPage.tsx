import { FilePlus2 } from 'lucide-react'

export function NewRecordPage() {
  return (
    <div className="space-y-5">
      <header>
        <p className="text-sm font-semibold text-brand-600">Kayıtlar</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">Yeni Kayıt</h1>
        <p className="mt-2 text-sm text-slate-500">Kayıt formu ayrı bir çalışma panelinde açıldı.</p>
      </header>
      <section className="flex min-h-80 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm">
        <div className="flex size-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
          <FilePlus2 className="size-6" aria-hidden="true" />
        </div>
        <h2 className="mt-4 font-bold text-slate-900">Yeni kayıt hazırlanıyor</h2>
        <p className="mt-1 max-w-sm text-sm leading-6 text-slate-500">
          Formu küçülttüğünüzde bu sayfadan ayrılmadan kayıtlarınızı inceleyebilirsiniz.
        </p>
      </section>
    </div>
  )
}
