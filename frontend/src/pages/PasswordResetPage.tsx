import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft, ArrowRight, Check, KeyRound, ShieldCheck } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { resetPassword } from '../api/auth'
import { ApiClientError } from '../api/errors'
import { PasswordField } from '../components/auth/PasswordField'
import { Brand } from '../components/layout/Brand'
import { resetPasswordSchema, type ResetPasswordFormValues } from '../schemas/auth'

type PasswordResetPageProps = {
  token: string
  onPasswordReset: () => void
}

export function PasswordResetPage({ token, onPasswordReset }: PasswordResetPageProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: '',
      newPasswordConfirm: '',
    },
  })

  const submit = handleSubmit(async ({ newPassword }) => {
    try {
      await resetPassword({ token, newPassword })
      onPasswordReset()
    } catch (error) {
      if (error instanceof ApiClientError) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'newPassword') {
            setError('newPassword', { type: 'server', message: fieldError.message })
          }
        }

        if (error.code === 'INVALID_OR_EXPIRED_RESET_TOKEN') {
          setError('root', {
            type: 'server',
            message: 'Bu şifre sıfırlama bağlantısı geçersiz, kullanılmış veya süresi dolmuş.',
          })
          return
        }
      }

      setError('root', {
        type: 'server',
        message: 'Şifreniz sıfırlanamadı. Lütfen tekrar deneyin.',
      })
    }
  })

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-app-canvas px-4 py-8 sm:px-8">
      <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-200/60 blur-3xl dark:bg-brand-900/45" />
      <div className="pointer-events-none absolute -bottom-40 -left-28 size-[28rem] rounded-full bg-blue-100/60 blur-3xl dark:bg-blue-900/40" />

      <section className="relative w-full max-w-2xl rounded-[1.5rem] border border-app-border bg-app-surface p-6 shadow-2xl shadow-slate-900/[0.08] sm:p-9 lg:p-11" aria-labelledby="password-reset-title">
        <Brand />

        <div className="mt-9 flex items-start gap-4">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-900/55 dark:text-brand-200">
            <KeyRound className="size-6" aria-hidden="true" />
          </span>
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.12em] text-brand-600 dark:text-brand-300">Hesap kurtarma</p>
            <h1 id="password-reset-title" className="mt-1 text-3xl font-semibold tracking-[-0.03em] text-app-text sm:text-4xl">
              Yeni şifrenizi belirleyin
            </h1>
            <p className="mt-3 text-sm leading-6 text-app-text-muted">
              Hesabınız için daha önce kullanmadığınız güçlü bir şifre oluşturun.
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
          </ul>
        </div>

        <form className="mt-7 space-y-5" noValidate onSubmit={submit}>
          <PasswordField
            id="reset-new-password"
            label="Yeni şifre"
            autoComplete="new-password"
            registration={register('newPassword')}
            error={errors.newPassword?.message}
          />
          <PasswordField
            id="reset-new-password-confirm"
            label="Yeni şifre tekrar"
            autoComplete="new-password"
            registration={register('newPasswordConfirm')}
            error={errors.newPasswordConfirm?.message}
          />

          {errors.root ? (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
              <p>{errors.root.message}</p>
              {errors.root.message?.includes('bağlantısı') ? (
                <Link to="/sifre-sifirla" className="mt-2 inline-flex items-center gap-1 underline underline-offset-2">
                  Yeni bağlantı iste
                  <ArrowRight className="size-3.5" aria-hidden="true" />
                </Link>
              ) : null}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
          >
            {isSubmitting ? 'Şifre sıfırlanıyor…' : 'Şifreyi sıfırla'}
            {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
          </button>

          <Link
            to="/giris"
            className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-brand-500"
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
            Giriş sayfasına dön
          </Link>
        </form>
      </section>
    </main>
  )
}
