function SkeletonBlock({ className }: { className: string }) {
  return <span className={`block animate-pulse rounded-lg bg-app-surface-strong motion-reduce:animate-none ${className}`} aria-hidden="true" />
}

export function RoutePageSkeleton({ label = 'Sayfa yükleniyor' }: { label?: string }) {
  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-5" role="status" aria-label={label}>
      <span className="sr-only">{label}…</span>
      <div className="space-y-3" aria-hidden="true">
        <SkeletonBlock className="h-8 w-56 max-w-[70%]" />
        <SkeletonBlock className="h-4 w-96 max-w-full" />
      </div>
      <div className="rounded-2xl border border-app-border bg-app-surface p-5 sm:p-6" aria-hidden="true">
        <SkeletonBlock className="h-11 w-full" />
        <div className="mt-6 space-y-3">
          {Array.from({ length: 5 }, (_, index) => (
            <div key={index} className="flex items-center gap-4 rounded-xl border border-app-border-subtle p-4">
              <SkeletonBlock className="size-10 shrink-0" />
              <div className="min-w-0 flex-1 space-y-2">
                <SkeletonBlock className="h-4 w-2/3" />
                <SkeletonBlock className="h-3 w-1/3" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export function ListLoadingSkeleton({
  label,
  rows = 5,
}: {
  label: string
  rows?: number
}) {
  return (
    <div className="space-y-3 p-4 sm:p-5" role="status" aria-label={label}>
      <span className="sr-only">{label}…</span>
      <div aria-hidden="true" className="space-y-3">
        {Array.from({ length: rows }, (_, index) => (
          <div key={index} className="flex items-center gap-4 rounded-xl border border-app-border-subtle p-4">
            <SkeletonBlock className="size-10 shrink-0" />
            <div className="min-w-0 flex-1 space-y-2">
              <SkeletonBlock className="h-4 w-3/5" />
              <SkeletonBlock className="h-3 w-2/5" />
            </div>
            <SkeletonBlock className="hidden h-7 w-24 sm:block" />
          </div>
        ))}
      </div>
    </div>
  )
}

export function DetailLoadingSkeleton({ label = 'Kayıt yükleniyor' }: { label?: string }) {
  return (
    <div className="mx-auto w-full max-w-[1400px] space-y-5" role="status" aria-label={label}>
      <span className="sr-only">{label}…</span>
      <div className="space-y-3" aria-hidden="true">
        <SkeletonBlock className="h-5 w-28" />
        <SkeletonBlock className="h-9 w-3/5" />
        <SkeletonBlock className="h-4 w-64" />
      </div>
      <div className="rounded-xl border border-app-border bg-app-surface p-6" aria-hidden="true">
        <SkeletonBlock className="h-5 w-36" />
        <div className="mt-5 space-y-3">
          <SkeletonBlock className="h-4 w-full" />
          <SkeletonBlock className="h-4 w-5/6" />
          <SkeletonBlock className="h-4 w-2/3" />
        </div>
        <div className="mt-8 border-t border-app-border-subtle pt-6">
          <SkeletonBlock className="h-5 w-28" />
          <SkeletonBlock className="mt-4 h-16 w-full" />
        </div>
      </div>
    </div>
  )
}
