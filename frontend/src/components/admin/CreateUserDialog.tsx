import { zodResolver } from '@hookform/resolvers/zod'
import { Check, Copy, UserPlus } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { useAdmin } from '../../context/adminState'
import { createUserSchema, type CreateUserFormValues } from '../../schemas/admin'
import { AdminDialog } from './AdminDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'

export function CreateUserDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { createUser } = useAdmin()
  const [temporaryPassword, setTemporaryPassword] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: { firstName: '', lastName: '', email: '' },
  })

  useEffect(() => {
    if (open) return
    reset()
    setTemporaryPassword(null)
    setCopied(false)
  }, [open, reset])

  const closeDialog = () => {
    reset()
    setTemporaryPassword(null)
    onClose()
  }

  const submit = handleSubmit((values) => runMutation(() => {
    try {
      const result = createUser(values)
      setTemporaryPassword(result.temporaryPassword ?? null)
    } catch (error) {
      setError('root', { message: error instanceof Error ? error.message : 'Hesap oluşturulamadı.' })
    }
  }))

  return (
    <AdminDialog
      open={open}
      onClose={closeDialog}
      icon={UserPlus}
      title={temporaryPassword ? 'Hesap oluşturuldu' : 'Yeni kullanıcı hesabı'}
      description={temporaryPassword
        ? 'Geçici parola yalnızca bu aşamada gösterilir.'
        : 'Kullanıcı kurumsal e-posta adresiyle sisteme eklenir.'}
    >
      {temporaryPassword ? (
        <div className="mt-6">
          <div className="rounded-2xl border border-emerald-200 dark:border-emerald-800/70 bg-emerald-50 dark:bg-emerald-950/40 p-4">
            <p className="text-sm font-bold text-emerald-900 dark:text-emerald-200">Geçici parola</p>
            <div className="mt-2 flex items-center gap-2">
              <code className="min-w-0 flex-1 rounded-xl bg-app-surface px-3 py-2.5 text-sm font-bold text-app-text ring-1 ring-emerald-200 dark:ring-emerald-800/70">
                {temporaryPassword}
              </code>
              <button
                type="button"
                onClick={async () => {
                  await navigator.clipboard.writeText(temporaryPassword)
                  setCopied(true)
                }}
                className="flex size-11 items-center justify-center rounded-xl bg-emerald-700 text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-500"
                aria-label="Geçici parolayı kopyala"
              >
                {copied ? <Check className="size-5" aria-hidden="true" /> : <Copy className="size-5" aria-hidden="true" />}
              </button>
            </div>
            <p className="mt-3 text-xs leading-5 text-emerald-800 dark:text-emerald-200">Kullanıcı ilk girişinde bu parolayı değiştirmek zorunda olacaktır.</p>
          </div>
          <button
            type="button"
            onClick={closeDialog}
            className="mt-5 min-h-11 w-full rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            Tamam
          </button>
        </div>
      ) : (
        <form className="mt-6 space-y-4" noValidate onSubmit={submit}>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Ad" error={errors.firstName?.message}>
              <input {...register('firstName')} autoComplete="given-name" className={inputClass} />
            </FormField>
            <FormField label="Soyad" error={errors.lastName?.message}>
              <input {...register('lastName')} autoComplete="family-name" className={inputClass} />
            </FormField>
          </div>
          <FormField label="Kurumsal e-posta" error={errors.email?.message}>
            <input {...register('email')} type="email" autoComplete="off" placeholder="ad.soyad@kurum.gov.tr" className={inputClass} />
          </FormField>
          <p className="rounded-xl bg-app-surface-muted px-3 py-2.5 text-xs leading-5 text-app-text-muted">
            Yeni hesap Çalışan rolüyle açılır. Gerekirse hesap oluşturulduktan sonra rolü ayrıca değiştirilebilir.
          </p>
          {errors.root ? <p className="text-sm font-semibold text-rose-700 dark:text-rose-300" role="alert">{errors.root.message}</p> : null}
          <div className="grid grid-cols-2 gap-3 pt-2">
            <button type="button" onClick={closeDialog} className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary hover:bg-app-surface-muted">Vazgeç</button>
            <button type="submit" disabled={isSubmitting || mutationBusy} className="min-h-11 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 disabled:opacity-60">Hesap Aç</button>
          </div>
        </form>
      )}
    </AdminDialog>
  )
}

const inputClass = 'min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60'

function FormField({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-bold text-app-text-emphasis">{label}</span>
      {children}
      {error ? <span className="mt-1 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">{error}</span> : null}
    </label>
  )
}
