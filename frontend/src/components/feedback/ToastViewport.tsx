import {
  CheckCircle2,
  CircleAlert,
  Info,
  TriangleAlert,
  X,
  type LucideIcon,
} from 'lucide-react'
import type { ToastItem, ToastTone } from '../../context/toastState'

const toneStyles: Record<ToastTone, {
  Icon: LucideIcon
  icon: string
  accent: string
}> = {
  success: {
    Icon: CheckCircle2,
    icon: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300',
    accent: 'bg-emerald-500',
  },
  error: {
    Icon: CircleAlert,
    icon: 'bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300',
    accent: 'bg-rose-500',
  },
  warning: {
    Icon: TriangleAlert,
    icon: 'bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300',
    accent: 'bg-amber-500',
  },
  info: {
    Icon: Info,
    icon: 'bg-brand-50 text-brand-700 dark:bg-brand-950/60 dark:text-brand-300',
    accent: 'bg-brand-500',
  },
}

export function ToastViewport({
  toasts,
  onDismiss,
}: {
  toasts: ToastItem[]
  onDismiss: (toastId: string) => void
}) {
  if (toasts.length === 0) return null

  return (
    <div
      className="pointer-events-none fixed inset-x-4 top-4 z-[100] flex flex-col items-end gap-2 sm:left-auto sm:right-5 sm:w-full sm:max-w-sm"
      aria-label="Bildirimler"
    >
      {toasts.map((toast) => {
        const { Icon, icon, accent } = toneStyles[toast.tone]
        return (
          <article
            key={toast.id}
            className="pointer-events-auto relative w-full overflow-hidden rounded-2xl border border-app-border bg-app-surface p-4 shadow-2xl shadow-slate-950/15"
            role={toast.tone === 'error' ? 'alert' : 'status'}
          >
            <span className={`absolute inset-y-0 left-0 w-1 ${accent}`} aria-hidden="true" />
            <div className="flex items-start gap-3">
              <span className={`flex size-9 shrink-0 items-center justify-center rounded-xl ${icon}`}>
                <Icon className="size-4.5" aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1 pt-0.5">
                <p className="text-sm font-bold text-app-text-emphasis">{toast.title}</p>
                {toast.description ? (
                  <p className="mt-1 text-xs leading-5 text-app-text-subtle">{toast.description}</p>
                ) : null}
              </div>
              <button
                type="button"
                onClick={() => onDismiss(toast.id)}
                className="flex size-8 shrink-0 items-center justify-center rounded-lg text-app-text-faint transition hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                aria-label="Bildirimi kapat"
              >
                <X className="size-4" aria-hidden="true" />
              </button>
            </div>
          </article>
        )
      })}
    </div>
  )
}
