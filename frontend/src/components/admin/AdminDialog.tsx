import { useRef, type ReactNode } from 'react'
import { X, type LucideIcon } from 'lucide-react'
import { useModalDialog } from '../../hooks/useModalDialog'

type AdminDialogProps = {
  open: boolean
  title: string
  description: string
  icon: LucideIcon
  children: ReactNode
  onClose: () => void
}

export function AdminDialog({
  open,
  title,
  description,
  icon: Icon,
  children,
  onClose,
}: AdminDialogProps) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  useModalDialog({ open, onClose, dialogRef, initialFocusRef: closeButtonRef })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-slate-950/35 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
      <section
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="admin-dialog-title"
        aria-describedby="admin-dialog-description"
        className="max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-lg sm:rounded-2xl sm:p-6"
      >
        <div className="flex items-start gap-3">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
            <Icon className="size-5" aria-hidden="true" />
          </span>
          <div className="min-w-0 flex-1">
            <h2 id="admin-dialog-title" className="text-lg font-bold text-app-text">{title}</h2>
            <p id="admin-dialog-description" className="mt-1 text-sm leading-6 text-app-text-muted">{description}</p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
            aria-label="Pencereyi kapat"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        {children}
      </section>
    </div>
  )
}
