import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, ShieldCheck, UserPlus } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { ApiClientError } from '../../api/errors'
import { useAdmin } from '../../context/adminState'
import { useToast } from '../../context/toastState'
import { createUserSchema, type CreateUserFormValues } from '../../schemas/admin'
import { AdminDialog } from './AdminDialog'

const formFields = ['firstName', 'lastName', 'email', 'password'] as const

function isFormField(field: string): field is typeof formFields[number] {
  return formFields.some((candidate) => candidate === field)
}

export function CreateUserDialog({
  open,
  onClose,
}: {
  open: boolean
  onClose: () => void
}) {
  const { createUser } = useAdmin()
  const { showToast } = useToast()
  const [passwordVisible, setPasswordVisible] = useState(false)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
    },
  })

  const closeDialog = () => {
    if (isSubmitting) return
    reset()
    setPasswordVisible(false)
    onClose()
  }

  const submit = handleSubmit(async (values) => {
    try {
      const user = await createUser(values)
      showToast({
        title: 'Kullanıcı oluşturuldu',
        description: `${user.firstName} ${user.lastName} Çalışan rolüyle sisteme eklendi.`,
        tone: 'success',
      })
      reset()
      setPasswordVisible(false)
      onClose()
    } catch (caught) {
      if (caught instanceof ApiClientError) {
        let fieldErrorMapped = false
        caught.fieldErrors.forEach((fieldError) => {
          if (!isFormField(fieldError.field)) return
          setError(fieldError.field, { type: 'server', message: fieldError.message })
          fieldErrorMapped = true
        })

        if (caught.status === 409) {
          setError('email', {
            type: 'server',
            message: 'Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var.',
          })
          return
        }

        if (fieldErrorMapped) return
        setError('root', { type: 'server', message: caught.message })
        return
      }

      setError('root', {
        type: 'server',
        message: 'Kullanıcı oluşturulamadı. Lütfen tekrar deneyin.',
      })
    }
  })

  return (
    <AdminDialog
      open={open}
      onClose={closeDialog}
      icon={UserPlus}
      title="Yeni kullanıcı"
      description="Kullanıcının temel bilgilerini ve ilk giriş şifresini belirleyin."
    >
      <form className="mt-6 space-y-4" noValidate onSubmit={submit}>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField fieldId="create-user-first-name" label="Ad" error={errors.firstName?.message} errorId="create-user-first-name-error">
            <input
              id="create-user-first-name"
              {...register('firstName')}
              autoComplete="given-name"
              aria-invalid={Boolean(errors.firstName)}
              aria-describedby={errors.firstName ? 'create-user-first-name-error' : undefined}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            />
          </FormField>
          <FormField fieldId="create-user-last-name" label="Soyad" error={errors.lastName?.message} errorId="create-user-last-name-error">
            <input
              id="create-user-last-name"
              {...register('lastName')}
              autoComplete="family-name"
              aria-invalid={Boolean(errors.lastName)}
              aria-describedby={errors.lastName ? 'create-user-last-name-error' : undefined}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            />
          </FormField>
        </div>

        <FormField fieldId="create-user-email" label="E-posta adresi" error={errors.email?.message} errorId="create-user-email-error">
          <input
            id="create-user-email"
            type="email"
            {...register('email')}
            autoComplete="email"
            placeholder="ad.soyad@kurum.gov.tr"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'create-user-email-error' : undefined}
            className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          />
        </FormField>

        <FormField fieldId="create-user-password" label="İlk giriş şifresi" error={errors.password?.message} errorId="create-user-password-error">
          <span className="relative block">
            <input
              id="create-user-password"
              type={passwordVisible ? 'text' : 'password'}
              {...register('password')}
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'create-user-password-error' : 'create-user-password-hint'}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 pr-12 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            />
            <button
              type="button"
              onClick={() => setPasswordVisible((visible) => !visible)}
              className="absolute right-1 top-1/2 flex size-9 -translate-y-1/2 items-center justify-center rounded-lg text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
              aria-label={passwordVisible ? 'Şifreyi gizle' : 'Şifreyi göster'}
            >
              {passwordVisible
                ? <EyeOff className="size-4" aria-hidden="true" />
                : <Eye className="size-4" aria-hidden="true" />}
            </button>
          </span>
          {!errors.password ? (
            <span id="create-user-password-hint" className="mt-1.5 block text-xs text-app-text-subtle">
              En az 6 karakter olmalıdır.
            </span>
          ) : null}
        </FormField>

        <div className="flex gap-3 rounded-xl border border-brand-100 bg-brand-50/70 px-4 py-3 text-xs leading-5 text-brand-800 dark:border-brand-800/60 dark:bg-brand-900/25 dark:text-brand-200">
          <ShieldCheck className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p>
            Hesap otomatik olarak <strong>Çalışan</strong> rolüyle açılır. Rol değişikliği hesap oluşturulduktan sonra ayrı yapılır.
          </p>
        </div>

        {errors.root ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
            {errors.root.message}
          </p>
        ) : null}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={closeDialog}
            className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted disabled:opacity-60"
          >
            Vazgeç
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-wait disabled:opacity-60"
          >
            {isSubmitting ? 'Oluşturuluyor…' : 'Kullanıcı Oluştur'}
          </button>
        </div>
      </form>
    </AdminDialog>
  )
}

function FormField({
  fieldId,
  label,
  error,
  errorId,
  children,
}: {
  fieldId: string
  label: string
  error?: string
  errorId: string
  children: ReactNode
}) {
  return (
    <div className="block">
      <label htmlFor={fieldId} className="mb-1.5 block text-sm font-bold text-app-text-emphasis">{label}</label>
      {children}
      {error ? (
        <span id={errorId} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
          {error}
        </span>
      ) : null}
    </div>
  )
}
