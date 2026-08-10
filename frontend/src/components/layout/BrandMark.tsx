export function BrandMark({ className = 'size-11' }: { className?: string }) {
  return (
    <img
      src="/brand/ebys-logo.png"
      alt=""
      aria-hidden="true"
      className={`shrink-0 object-contain drop-shadow-sm ${className}`}
    />
  )
}
