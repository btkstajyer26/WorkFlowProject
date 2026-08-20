import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft, ArrowRight, Mail, MailCheck, RotateCcw } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { requestPasswordReset, verifyPasswordResetCode } from '../api/auth'
import { ApiClientError } from '../api/errors'
import { Brand } from '../components/layout/Brand'
import {
  forgotPasswordSchema,
  resetCodeSchema,
  type ForgotPasswordFormValues,
  type ResetCodeFormValues,
} from '../schemas/auth'
import type { AuthUser } from '../types/auth'

type ForgotPasswordPageProps = {
  user: AuthUser | null
}

export function ForgotPasswordPage({ user }: ForgotPasswordPageProps) {
  // Kodun gönderildiği adres ikinci adımda da gerekiyor: doğrulama ucu kodu
  // hesapla eşleştirebilmek için e-postayı ister.
  const [pendingEmail, setPendingEmail] = useState<string | null>(null)

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-app-canvas px-4 py-8 sm:px-8">
      <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-200/60 blur-3xl dark:bg-brand-900/45" />
      <div className="pointer-events-none absolute -bottom-40 -left-28 size-[28rem] rounded-full bg-blue-100/60 blur-3xl dark:bg-blue-900/40" />

      <section className="relative w-full max-w-xl rounded-[1.5rem] border border-app-border bg-app-surface p-6 shadow-2xl shadow-slate-900/[0.08] sm:p-9 lg:p-11" aria-labelledby="forgot-password-title">
        <Brand />

        {pendingEmail === null ? (
          <EmailStep onCodeSent={setPendingEmail} />
        ) : (
          <CodeStep email={pendingEmail} onChangeEmail={() => setPendingEmail(null)} />
        )}
      </section>
    </main>
  )
}

function EmailStep({ onCodeSent }: { onCodeSent: (email: string) => void }) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  const submit = handleSubmit(async ({ email }) => {
    const normalizedEmail = email.trim().toLowerCase()
    try {
      await requestPasswordReset({ email: normalizedEmail })
      onCodeSent(normalizedEmail)
    } catch {
      setError('root', {
        type: 'server',
        message: 'Doğrulama kodu gönderilemedi. Lütfen biraz sonra tekrar deneyin.',
      })
    }
  })

  return (
    <>
      <div className="mt-9">
        <p className="text-xs font-extrabold uppercase tracking-[0.12em] text-brand-600 dark:text-brand-300">Hesap kurtarma</p>
        <h1 id="forgot-password-title" className="mt-1 text-3xl font-semibold tracking-[-0.03em] text-app-text sm:text-4xl">
          Şifrenizi sıfırlayın
        </h1>
        <p className="mt-3 text-sm leading-6 text-app-text-muted">
          Hesabınıza bağlı e-posta adresini yazın. Şifrenizi yenileyebilmeniz için 6 haneli bir doğrulama kodu gönderelim.
        </p>
      </div>

      <form className="mt-8 space-y-5" noValidate onSubmit={submit}>
        <label className="block">
          <span className="mb-2 block text-sm font-bold text-app-text-emphasis">E-posta adresi</span>
          <span className="relative block">
            <Mail className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
            <input
              type="email"
              {...register('email')}
              autoComplete="email"
              placeholder="ad.soyad@kurum.gov.tr"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'forgot-password-email-error' : undefined}
              className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            />
          </span>
          {errors.email ? (
            <span id="forgot-password-email-error" className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
              {errors.email.message}
            </span>
          ) : null}
        </label>

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
          {isSubmitting ? 'Kod gönderiliyor…' : 'Doğrulama kodu gönder'}
          {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
        </button>

        <BackToLoginLink />
      </form>
    </>
  )
}

function CodeStep({ email, onChangeEmail }: { email: string; onChangeEmail: () => void }) {
  const navigate = useNavigate()
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetCodeFormValues>({
    resolver: zodResolver(resetCodeSchema),
    defaultValues: { code: '' },
  })

  const submit = handleSubmit(async ({ code }) => {
    try {
      const { resetToken } = await verifyPasswordResetCode({ email, code: code.trim() })
      // Anahtar adres çubuğunda kalmasın diye geçmişte iz bırakılmaz.
      navigate(`/sifre-degistir?token=${encodeURIComponent(resetToken)}`, { replace: true })
    } catch (error) {
      if (error instanceof ApiClientError && error.code === 'INVALID_OR_EXPIRED_RESET_CODE') {
        setError('code', {
          type: 'server',
          message: 'Kod geçersiz veya süresi dolmuş. Yeni bir kod isteyebilirsiniz.',
        })
        return
      }

      setError('root', {
        type: 'server',
        message: 'Kod doğrulanamadı. Lütfen tekrar deneyin.',
      })
    }
  })

  const resend = async () => {
    setResendState('sending')
    try {
      await requestPasswordReset({ email })
      setResendState('sent')
    } catch {
      setResendState('idle')
      setError('root', {
        type: 'server',
        message: 'Yeni kod gönderilemedi. Lütfen biraz sonra tekrar deneyin.',
      })
    }
  }

  return (
    <>
      <div className="mt-9">
        <span className="flex size-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300">
          <MailCheck className="size-6" aria-hidden="true" />
        </span>
        <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.12em] text-brand-600 dark:text-brand-300">Hesap kurtarma</p>
        <h1 id="forgot-password-title" className="mt-1 text-3xl font-semibold tracking-[-0.03em] text-app-text sm:text-4xl">
          E-postanızı kontrol edin
        </h1>
        <p className="mt-3 text-sm leading-6 text-app-text-muted">
          Bu adresle eşleşen bir hesap varsa <b className="font-semibold text-app-text-emphasis">{email}</b> adresine
          6 haneli bir doğrulama kodu gönderildi. Kod kısa bir süre için ve yalnızca bir kez geçerlidir.
        </p>
      </div>

      <form className="mt-8 space-y-5" noValidate onSubmit={submit}>
        <label className="block">
          <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Doğrulama kodu</span>
          <input
            type="text"
            {...register('code')}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            aria-invalid={Boolean(errors.code)}
            aria-describedby={errors.code ? 'reset-code-error' : undefined}
            className="h-13 w-full rounded-xl border border-app-border bg-app-surface px-4 text-center font-mono text-2xl tracking-[0.5em] text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          />
          {errors.code ? (
            <span id="reset-code-error" className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
              {errors.code.message}
            </span>
          ) : null}
        </label>

        {errors.root ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
            {errors.root.message}
          </p>
        ) : null}

        {resendState === 'sent' ? (
          <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800 dark:border-emerald-800/70 dark:bg-emerald-950/40 dark:text-emerald-200" role="status">
            Yeni kod gönderildi. E-postanız birkaç dakika içinde ulaşmazsa gereksiz klasörünü kontrol edin.
          </p>
        ) : null}

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
        >
          {isSubmitting ? 'Kod doğrulanıyor…' : 'Kodu doğrula'}
          {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
        </button>

        <button
          type="button"
          onClick={resend}
          disabled={resendState === 'sending'}
          className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-brand-500"
        >
          <RotateCcw className="size-4" aria-hidden="true" />
          {resendState === 'sending' ? 'Kod gönderiliyor…' : 'Kodu tekrar gönder'}
        </button>

        <button
          type="button"
          onClick={onChangeEmail}
          className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-brand-500"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Farklı bir e-posta adresi kullan
        </button>

        <BackToLoginLink />
      </form>
    </>
  )
}

function BackToLoginLink() {
  return (
    <Link
      to="/giris"
      className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-brand-500"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Giriş sayfasına dön
    </Link>
  )
}
