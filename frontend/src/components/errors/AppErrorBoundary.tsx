import { Component, type ErrorInfo, type ReactNode } from 'react'
import { RefreshCw, TriangleAlert } from 'lucide-react'

type AppErrorBoundaryProps = { children: ReactNode }
type AppErrorBoundaryState = { hasError: boolean }

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = { hasError: false }

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Beklenmeyen arayüz hatası', error, errorInfo)
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <main className="flex min-h-screen items-center justify-center bg-app-surface-muted p-4">
        <section className="w-full max-w-lg rounded-3xl border border-app-border bg-app-surface p-7 text-center shadow-sm sm:p-10">
          <span className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300">
            <TriangleAlert className="size-8" aria-hidden="true" />
          </span>
          <p className="mt-6 text-sm font-black tracking-[0.2em] text-rose-700 dark:text-rose-300">BEKLENMEYEN HATA</p>
          <h1 className="mt-2 text-2xl font-bold tracking-tight text-app-text">Sayfa görüntülenirken bir sorun oluştu</h1>
          <p className="mt-3 text-sm leading-6 text-app-text-muted">
            Çalışmanız sunucuya gönderilmediyse form verilerinizi kontrol edin ve sayfayı yeniden yükleyin.
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-7 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-5 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <RefreshCw className="size-4" aria-hidden="true" />
            Sayfayı Yenile
          </button>
        </section>
      </main>
    )
  }
}
