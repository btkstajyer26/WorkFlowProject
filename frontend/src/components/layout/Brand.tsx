type BrandProps = {
  tone?: 'default' | 'inverse'
  size?: 'default' | 'large'
}

export function Brand({ tone = 'default', size = 'default' }: BrandProps) {
  const inverse = tone === 'inverse'
  const large = size === 'large'

  return (
    <div className="flex min-w-0 items-center gap-3">
      <img
        src="/ebys-logo.png"
        alt=""
        aria-hidden="true"
        width={512}
        height={512}
        className={`${large ? 'size-16' : 'size-12'} shrink-0 object-contain drop-shadow-lg`}
      />
      <div className="min-w-0">
        <p className={`${large ? 'text-2xl font-extrabold' : 'text-lg font-bold'} tracking-tight ${inverse ? 'text-white' : 'text-app-text-strong'}`}>
          EBYS
        </p>
        <p className={`truncate font-medium ${large ? 'text-sm' : 'text-[11px]'} ${inverse ? 'text-violet-100/80' : 'text-app-text-subtle'}`}>
          İş Akışı ve Onay Yönetim Sistemi
        </p>
      </div>
    </div>
  )
}
