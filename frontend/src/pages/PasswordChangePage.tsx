import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, Check, Eye, EyeOff, KeyRound, LockKeyhole, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { useForm, type UseFormRegisterReturn } from 'react-hook-form'
import { Link, Navigate } from 'react-router'
import { changePassword } from '../api/auth'
import { ApiClientError } from '../api/errors'
import { Brand } from '../components/layout/Brand'
import { changePasswordSchema, type ChangePasswordFormValues } from '../schemas/auth'
import type { AuthUser } from '../types/auth'

type PasswordChangePageProps = {
  user: AuthUser | null
  onPasswordChanged: () => void
  onUseAnotherAccount: () => void
}

export function PasswordChangePage({ user, onPasswordChanged, onUseAnotherAccount }: PasswordChangePageProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      newPasswordConfirm: '',
    },
  })

  if (!user) return <Navigate to="/giris?returnTo=%2Fsifre-degistir" replace />
  const isRequiredChange = user.mustChangePassword

  const submit = handleSubmit(async ({ currentPassword, newPassword }) => {
    try {
      await changePassword({ currentPassword, newPassword })
      onPasswordChanged()
    } catch (error) {
      if (error instanceof ApiClientError) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'currentPassword' || fieldError.field === 'newPassword') {
            setError(fieldError.field, { type: 'server', message: fieldError.message })
          }
        }

        if (error.status === 401 || error.code === 'INVALID_CREDENTIALS') {
          setError('currentPassword', { type: 'server', message: 'Mevcut şifreniz doğru değil.' })
          return
        }
      }

      setError('root', {
        type: 'server',
        message: 'Şifreniz değiştirilemedi. Lütfen tekrar deneyin.',
      })
    }
  })

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-app-canvas px-4 py-8 sm:px-8">
      <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-200/60 blur-3xl dark:bg-brand-900/45" />
      <div className="pointer-events-none absolute -bottom-40 -left-28 size-[28rem] rounded-full bg-blue-100/60 blur-3xl dark:bg-blue-900/40" />

      <section className="relative w-full max-w-2xl rounded-[1.5rem] border border-app-border bg-app-surface p-6 shadow-2xl shadow-slate-900/[0.08] sm:p-9 lg:p-11" aria-labelledby="password-change-title">
        <Brand />

        <div className="mt-9 flex items-start gap-4">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-900/55 dark:text-brand-200">
            <KeyRound className="size-6" aria-hidden="true" />
          </span>
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.12em] text-brand-600 dark:text-brand-300">
              {isRequiredChange ? 'İlk giriş güvenliği' : 'Hesap güvenliği'}
            </p>
            <h1 id="password-change-title" className="mt-1 text-3xl font-semibold tracking-[-0.03em] text-app-text sm:text-4xl">
              Şifrenizi değiştirin
            </h1>
            <p className="mt-3 text-sm leading-6 text-app-text-muted">
              {isRequiredChange
                ? 'Hesabınızı kullanmaya devam etmek için geçici şifrenizi kişisel bir şifreyle yenileyin.'
                : 'Mevcut şifrenizi doğrulayarak hesabınız için yeni bir şifre belirleyin.'}
            </p>
          </div>
        </div>

        <div className="mt-7 rounded-xl border border-app-border bg-app-surface-muted px-4 py-3.5">
          <p className="flex items-center gap-2 text-sm font-bold text-app-text-emphasis">
            <ShieldCheck className="size-4 text-brand-600 dark:text-brand-300" aria-hidden="true" />
            Yeni şifreniz
          </p>
          <ul className="mt-2 grid gap-1.5 text-sm text-app-text-muted sm:grid-cols-2">
            <li className="flex items-center gap-2"><Check className="size-3.5 text-emerald-600" aria-hidden="true" />En az 8 karakter</li>
            <li className="flex items-center gap-2"><Check className="size-3.5 text-emerald-600" aria-hidden="true" />En az bir harf ve bir rakam</li>
            <li className="flex items-center gap-2"><Check className="size-3.5 text-emerald-600" aria-hidden="true" />Mevcut şifrenizden farklı</li>
          </ul>
        </div>

        <form className="mt-7 space-y-5" noValidate onSubmit={submit}>
          <PasswordField
            id="current-password"
            label="Mevcut şifre"
            autoComplete="current-password"
            registration={register('currentPassword')}
            error={errors.currentPassword?.message}
          />
          <PasswordField
            id="new-password"
            label="Yeni şifre"
            autoComplete="new-password"
            registration={register('newPassword')}
            error={errors.newPassword?.message}
          />
          <PasswordField
            id="new-password-confirm"
            label="Yeni şifre tekrar"
            autoComplete="new-password"
            registration={register('newPasswordConfirm')}
            error={errors.newPasswordConfirm?.message}
          />

          {errors.root ? (
            <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
              {errors.root.message}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
          >
            {isSubmitting ? 'Şifre güncelleniyor…' : 'Şifreyi Güncelle'}
            {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
          </button>

          {isRequiredChange ? (
            <button
              type="button"
              onClick={onUseAnotherAccount}
              className="min-h-11 w-full rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-brand-500"
            >
              Farklı hesapla giriş yap
            </button>
          ) : (
            <Link
              to="/profil"
              className="flex min-h-11 w-full items-center justify-center rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-brand-500"
            >
              Profile dön
            </Link>
          )}
        </form>
      </section>
    </main>
  )
}

function PasswordField({
  id,
  label,
  autoComplete,
  registration,
  error,
}: {
  id: string
  label: string
  autoComplete: 'current-password' | 'new-password'
  registration: UseFormRegisterReturn
  error?: string
}) {
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
