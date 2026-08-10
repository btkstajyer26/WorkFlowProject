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
  card: string
}> = {
  success: {
    Icon: CheckCircle2,
    icon: 'bg-emerald-600 text-white dark:bg-emerald-500 dark:text-emerald-950',
    accent: 'bg-emerald-600 dark:bg-emerald-400',
    card: 'border-emerald-300 bg-emerald-50/95 shadow-emerald-950/15 dark:border-emerald-700 dark:bg-emerald-950/95',
  },
  error: {
    Icon: CircleAlert,
    icon: 'bg-rose-600 text-white dark:bg-rose-500 dark:text-rose-950',
    accent: 'bg-rose-600 dark:bg-rose-400',
    card: 'border-rose-300 bg-rose-50/95 shadow-rose-950/15 dark:border-rose-700 dark:bg-rose-950/95',
  },
  warning: {
    Icon: TriangleAlert,
    icon: 'bg-amber-500 text-amber-950 dark:bg-amber-400',
    accent: 'bg-amber-500 dark:bg-amber-400',
    card: 'border-amber-300 bg-amber-50/95 shadow-amber-950/15 dark:border-amber-700 dark:bg-amber-950/95',
  },
  info: {
    Icon: Info,
    icon: 'bg-brand-700 text-white dark:bg-brand-400 dark:text-brand-950',
    accent: 'bg-brand-600 dark:bg-brand-400',
    card: 'border-brand-300 bg-brand-50/95 shadow-brand-950/15 dark:border-brand-700 dark:bg-brand-950/95',
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
      className="pointer-events-none fixed inset-x-4 bottom-4 z-[100] flex flex-col items-end gap-2 sm:left-auto sm:right-5 sm:w-full sm:max-w-sm"
      aria-label="Bildirimler"
    >
      {toasts.map((toast) => {
        const { Icon, icon, accent, card } = toneStyles[toast.tone]
        return (
          <article
            key={toast.id}
            className={`pointer-events-auto relative w-full overflow-hidden rounded-2xl border p-4 shadow-2xl ${card}`}
            role={toast.tone === 'error' ? 'alert' : 'status'}
          >
            <span className={`absolute inset-y-0 left-0 w-1.5 ${accent}`} aria-hidden="true" />
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
                className="flex size-8 shrink-0 items-center justify-center rounded-lg text-app-text-faint transition hover:bg-white/70 hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:hover:bg-white/10"
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
