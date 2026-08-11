import { RotateCcw } from 'lucide-react'

export function CategoryLoadError({ id, onRetry }: { id: string; onRetry: () => void }) {
  return (
    <p id={id} className="mt-2 flex items-center gap-2 text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
      Kategoriler yüklenemedi.
      <button
        type="button"
        onClick={onRetry}
        className="inline-flex items-center gap-1 rounded-md text-brand-700 hover:text-brand-900 focus-visible:outline-2 focus-visible:outline-brand-500 dark:text-brand-300 dark:hover:text-brand-100"
      >
        <RotateCcw className="size-3" aria-hidden="true" />
        Tekrar dene
      </button>
    </p>
  )
}
