import { Eye, EyeOff, LockKeyhole } from 'lucide-react'
import { useState } from 'react'
import type { UseFormRegisterReturn } from 'react-hook-form'

type PasswordFieldProps = {
  id: string
  label: string
  autoComplete: 'current-password' | 'new-password'
  registration: UseFormRegisterReturn
  error?: string
}

export function PasswordField({ id, label, autoComplete, registration, error }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  const errorId = `${id}-error`

  return (
    <div className="block">
      <label className="mb-2 block text-sm font-bold text-app-text-emphasis" htmlFor={id}>{label}</label>
      <span className="relative block">
        <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          autoComplete={autoComplete}
          {...registration}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-12 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute right-2 top-1/2 flex size-9 -translate-y-1/2 items-center justify-center rounded-lg text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
          aria-label={`${label} ${visible ? 'gizle' : 'göster'}`}
        >
          {visible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
        </button>
      </span>
      {error ? <span id={errorId} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">{error}</span> : null}
    </div>
  )
}
