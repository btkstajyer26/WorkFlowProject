import { zodResolver } from '@hookform/resolvers/zod'
import {
  ArrowRight,
  Eye,
  EyeOff,
  FileClock,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { ApiClientError } from '../api/errors'
import { clearAuthSession, startAuthSession } from '../auth/authSession'
import { Brand } from '../components/layout/Brand'
import { defaultDemoAccount, demoAccounts } from '../mocks/users'
import { loginSchema, type LoginFormValues } from '../schemas/auth'
import type { AuthUser } from '../types/auth'

type LoginPageProps = {
  user: AuthUser | null
  onLogin: (user: AuthUser) => void
}

export function LoginPage({ user, onLogin }: LoginPageProps) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [passwordVisible, setPasswordVisible] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: import.meta.env.DEV ? defaultDemoAccount.email : '',
      password: import.meta.env.DEV ? defaultDemoAccount.password : '',
    },
  })

  if (user) return <Navigate to="/dashboard" replace />

  const reason = searchParams.get('reason')
  const submit = handleSubmit(async (values) => {
    const normalizedEmail = values.email.trim().toLowerCase()
    const matchingDemoAccount = demoAccounts.find(
      (account) => account.email === normalizedEmail,
    )

    try {
      await startAuthSession(normalizedEmail, values.password)
    } catch (error) {
      setError('root', {
        type: 'server',
        message: error instanceof ApiClientError && error.status === 401
          ? 'E-posta adresi veya şifre hatalı.'
          : 'Giriş işlemi tamamlanamadı. Lütfen tekrar deneyin.',
      })
      return
    }

    if (!matchingDemoAccount) {
      clearAuthSession()
      setError('root', {
        type: 'server',
        message: 'Kullanıcı profil bilgileri henüz API sözleşmesinde bulunmuyor.',
      })
      return
    }

    const authenticatedUser: AuthUser = {
      id: matchingDemoAccount.id,
      firstName: matchingDemoAccount.firstName,
      lastName: matchingDemoAccount.lastName,
      email: matchingDemoAccount.email,
      role: matchingDemoAccount.role,
    }

    onLogin(authenticatedUser)
    const requestedPath = searchParams.get('returnTo')
    const safeReturnTo = requestedPath?.startsWith('/') && !requestedPath.startsWith('//')
      ? requestedPath
      : '/dashboard'
    navigate(safeReturnTo, { replace: true })
  })

  return (
    <main className="min-h-screen bg-[#f7f7fc] lg:grid lg:grid-cols-[minmax(0,0.82fr)_minmax(38rem,1.18fr)]">
      <section className="relative hidden min-h-screen overflow-hidden bg-[radial-gradient(circle_at_82%_40%,#39217a_0%,#241153_34%,#170d3d_68%,#11092e_100%)] px-12 py-11 text-white lg:flex lg:flex-col lg:justify-between xl:px-16 xl:py-12">
        <div className="pointer-events-none absolute -bottom-40 right-[-10rem] size-[34rem] rounded-full border border-white/10" />
        <div className="pointer-events-none absolute -bottom-64 right-[-4rem] size-[34rem] rounded-full border border-white/10" />
        <div className="pointer-events-none absolute right-8 top-1/3 size-64 rounded-full bg-brand-500/15 blur-3xl" />

        <div className="relative">
          <Brand tone="inverse" size="large" />
        </div>

        <div className="relative max-w-xl">
          <span className="inline-flex rounded-full border border-white/20 bg-white/5 px-4 py-2 text-xs font-extrabold tracking-[0.12em] text-violet-100">
            GÜVENLİ BELGE YÖNETİMİ
          </span>
          <h2 className="mt-7 max-w-lg text-5xl font-light leading-[1.08] tracking-[-0.04em] xl:text-6xl">
            İşlerinizi tek bir akışta yönetin.
          </h2>
          <p className="mt-6 max-w-lg text-base leading-8 text-violet-100/80">
            Kayıt oluşturma, inceleme ve nihai onay süreçlerini rolünüze özel panelden güvenle takip edin.
          </p>

          <div className="mt-8 flex flex-wrap gap-3 text-sm font-semibold text-violet-50">
            <FeaturePill icon={FileClock} text="Rol bazlı iş akışları" />
            <FeaturePill icon={ShieldCheck} text="Güvenli oturum yönetimi" />
          </div>
        </div>

        <p className="relative text-xs leading-5 text-violet-200/65">
          Kurum içi kayıt süreçleri için izlenebilir ve güvenli çalışma alanı
        </p>
      </section>

      <section className="relative flex min-h-screen items-center justify-center overflow-clip px-4 py-8 sm:px-8 lg:px-12 xl:px-16">
        <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-100/70 dark:bg-brand-900/45 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 left-10 size-[26rem] rounded-full bg-blue-100/55 dark:bg-blue-900/60 blur-3xl" />

        <div className="relative mx-auto w-full max-w-2xl rounded-[1.75rem] border border-white/80 bg-app-surface/90 p-5 shadow-2xl shadow-slate-900/[0.08] backdrop-blur sm:p-8 lg:p-10 xl:p-12">
          <div className="mb-8 lg:hidden">
            <Brand />
          </div>

          <div className="mx-auto max-w-xl">
            <h1 className="text-3xl font-medium tracking-[-0.03em] text-app-text sm:text-4xl">
              Hesabınıza giriş yapın
            </h1>
            <p className="mt-3 text-sm leading-6 text-app-text-muted">
              Devam etmek için kurumsal hesap bilgilerinizi girin.
            </p>

            {reason === 'expired' ? (
              <p className="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-900 dark:border-amber-800/70 dark:bg-amber-950/40 dark:text-amber-200" role="status">
                Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.
              </p>
            ) : null}

            <form className="mt-8 space-y-5" noValidate onSubmit={submit}>
              <label className="block">
                <span className="mb-2 block text-sm font-bold text-app-text-emphasis">E-posta adresi</span>
                <span className="relative block">
                  <Mail className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                  <input
                    type="email"
                    {...register('email')}
                    autoComplete="username"
                    placeholder="ad.soyad@kurum.gov.tr"
                    aria-invalid={Boolean(errors.email)}
                    aria-describedby={errors.email ? 'login-email-error' : undefined}
                    className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                  />
                </span>
                {errors.email ? <FieldError id="login-email-error" message={errors.email.message} /> : null}
              </label>

              <label className="block">
                <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Şifre</span>
                <span className="relative block">
                  <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                  <input
                    type={passwordVisible ? 'text' : 'password'}
                    {...register('password')}
                    autoComplete="current-password"
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? 'login-password-error' : undefined}
                    className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-12 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                  />
                  <PasswordVisibilityButton visible={passwordVisible} onToggle={() => setPasswordVisible((visible) => !visible)} />
                </span>
                {errors.password ? <FieldError id="login-password-error" message={errors.password.message} /> : null}
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
                {isSubmitting ? 'Giriş yapılıyor…' : 'Giriş Yap'}
                {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
              </button>
            </form>

            <div className="mt-6 flex gap-3 rounded-xl border border-app-border bg-app-surface-muted px-4 py-3 text-xs leading-5 text-app-text-muted">
              <ShieldCheck className="mt-0.5 size-4 shrink-0 text-brand-600 dark:text-brand-300" aria-hidden="true" />
              <p>
                Hesabınız kurumunuzun sistem yöneticisi tarafından oluşturulur. Hesabınız yoksa sistem yöneticinizle iletişime geçin.
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}

function FeaturePill({ icon: Icon, text }: { icon: typeof ShieldCheck; text: string }) {
  return (
    <span className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/15 bg-white/[0.06] px-3.5">
      <Icon className="size-4" aria-hidden="true" />
      {text}
    </span>
  )
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return (
    <span id={id} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
      {message}
    </span>
  )
}

function PasswordVisibilityButton({
  visible,
  onToggle,
}: {
  visible: boolean
  onToggle: () => void
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className="absolute right-2 top-1/2 flex size-9 -translate-y-1/2 items-center justify-center rounded-lg text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
      aria-label={visible ? 'Şifreyi gizle' : 'Şifreyi göster'}
    >
      {visible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
    </button>
  )
}
