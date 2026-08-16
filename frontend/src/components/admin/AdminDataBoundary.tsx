import { RefreshCw } from 'lucide-react'
import type { ReactNode } from 'react'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { useAdmin } from '../../context/adminState'

export function AdminDataBoundary({ children }: { children: ReactNode }) {
  const { loadStatus, retryLoad } = useAdmin()
  const { busy, run } = useSingleFlight()

  if (loadStatus === 'loading') {
    return (
      <div className="flex min-h-72 items-center justify-center" role="status">
        <p className="text-sm font-semibold text-app-text-muted">Yönetim bilgileri yükleniyor…</p>
      </div>
    )
  }

  if (loadStatus === 'error') {
    return (
      <section className="mx-auto max-w-xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center dark:border-rose-900/60 dark:bg-rose-950/30" role="alert">
        <h1 className="text-lg font-bold text-rose-900 dark:text-rose-100">Yönetim bilgileri yüklenemedi</h1>
        <p className="mt-2 text-sm leading-6 text-rose-800 dark:text-rose-200">Bağlantıyı kontrol edip tekrar deneyin.</p>
        <button
          type="button"
          disabled={busy}
          onClick={() => run(() => retryLoad().catch(() => undefined))}
          className="mt-5 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-rose-700 px-4 text-sm font-bold text-white hover:bg-rose-800 disabled:opacity-60"
        >
          <RefreshCw className="size-4" aria-hidden="true" />
          {busy ? 'Tekrar deneniyor…' : 'Tekrar dene'}
        </button>
      </section>
    )
  }

  return children
}
