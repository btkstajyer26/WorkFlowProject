import { ArrowLeft, FileQuestion, House, ShieldAlert } from 'lucide-react'
import { Link, useNavigate } from 'react-router'

export function ErrorStatePage({ type }: { type: '403' | '404' }) {
  const navigate = useNavigate()
  const forbidden = type === '403'
  const Icon = forbidden ? ShieldAlert : FileQuestion

  return (
    <section className="flex min-h-[calc(100vh-13rem)] items-center justify-center py-8">
      <div className="w-full max-w-xl rounded-3xl border border-app-border bg-app-surface p-7 text-center shadow-sm sm:p-10">
        <span className={`mx-auto flex size-16 items-center justify-center rounded-2xl ${forbidden ? 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300' : 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300'}`}>
          <Icon className="size-8" aria-hidden="true" />
        </span>
        <p className="mt-6 text-sm font-black tracking-[0.2em] text-brand-700 dark:text-brand-300">{type}</p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">
          {forbidden ? 'Bu sayfayı görüntüleme yetkiniz yok' : 'Aradığınız sayfa bulunamadı'}
        </h1>
        <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-app-text-muted">
          {forbidden
            ? 'Bu kayıt veya işlem mevcut rolünüzün yetki kapsamı dışında olabilir.'
            : 'Adres değiştirilmiş, kayıt kaldırılmış veya bağlantı hatalı olabilir.'}
        </p>
        <div className="mt-7 grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
            Önceki Sayfaya Dön
          </button>
          <Link
            to="/dashboard"
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <House className="size-4" aria-hidden="true" />
            Dashboard’a Dön
          </Link>
        </div>
      </div>
    </section>
  )
}
