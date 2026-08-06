import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import {
  ArrowRight,
  Eye,
  EyeOff,
  FileClock,
  FileText,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from 'lucide-react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { loginSchema, type LoginFormValues } from '../schemas/auth'
import { defaultDemoAccount, demoAccounts } from '../mocks/users'
import type { AuthUser } from '../types/auth'

export function LoginPage({ user, onLogin }: { user: AuthUser | null; onLogin: (user: AuthUser) => void }) {
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
  const submitLogin = handleSubmit(async (values) => {
    const matchingDemoAccount = demoAccounts.find(
      (account) => account.email === values.email.trim().toLowerCase(),
    )

    if (matchingDemoAccount && values.password !== matchingDemoAccount.password) {
      setError('password', { type: 'validate', message: 'Demo hesabı için şifre hatalı.' })
      return
    }

    await new Promise<void>((resolve) => window.setTimeout(resolve, 350))

    const authenticatedUser: AuthUser = matchingDemoAccount
      ? {
          id: matchingDemoAccount.id,
          firstName: matchingDemoAccount.firstName,
          lastName: matchingDemoAccount.lastName,
          email: matchingDemoAccount.email,
          role: matchingDemoAccount.role,
        }
      : {
          id: 'user-preview-001',
          firstName: 'John',
          lastName: 'Doe',
          email: values.email.trim().toLowerCase(),
          role: 'CALISAN',
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

        <div className="relative flex items-center gap-3">
          <span className="flex size-13 items-center justify-center rounded-2xl border border-white/25 bg-white/5 shadow-lg shadow-black/10">
            <FileText className="size-7" aria-hidden="true" />
          </span>
          <div>
            <p className="text-2xl font-extrabold tracking-tight">EBYS</p>
            <p className="text-sm text-violet-100/80">İş Akışı ve Onay Yönetim Sistemi</p>
          </div>
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

      <section className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8 sm:px-8 lg:px-12 xl:px-16">
        <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-100/70 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 left-10 size-[26rem] rounded-full bg-blue-100/55 blur-3xl" />

        <div className="relative w-full max-w-2xl rounded-[1.75rem] border border-white/80 bg-white/90 p-5 shadow-2xl shadow-slate-900/[0.08] backdrop-blur sm:p-8 lg:p-10 xl:p-12">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-3">
              <span className="flex size-11 items-center justify-center rounded-2xl bg-brand-700 text-white shadow-lg shadow-brand-200">
                <FileText className="size-6" aria-hidden="true" />
              </span>
              <div>
                <p className="text-lg font-bold text-slate-950">EBYS</p>
                <p className="text-[11px] text-slate-500">İş Akışı ve Onay Yönetim Sistemi</p>
              </div>
            </div>
          </div>

          <div className="mx-auto max-w-xl">
            <h1 className="text-3xl font-medium tracking-[-0.03em] text-slate-950 sm:text-4xl">Hesabınıza giriş yapın</h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">Devam etmek için kurumsal hesap bilgilerinizi girin.</p>

            {reason === 'expired' ? (
              <p className="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-900" role="status">
                Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.
              </p>
            ) : null}

            <form
              className="mt-8 space-y-5"
              noValidate
              onSubmit={submitLogin}
            >
              <label className="block">
                <span className="mb-2 block text-sm font-bold text-slate-800">E-posta adresi</span>
                <span className="relative block">
                  <Mail className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-slate-400" aria-hidden="true" />
                  <input
                    type="email"
                    {...register('email')}
                    autoComplete="username"
                    placeholder="ad.soyad@kurum.gov.tr"
                    aria-invalid={Boolean(errors.email)}
                    aria-describedby={errors.email ? 'login-email-error' : undefined}
                    className="h-13 w-full rounded-xl border border-slate-200 bg-white pl-11 pr-4 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                  />
                </span>
                {errors.email ? <FieldError id="login-email-error" message={errors.email.message} /> : null}
              </label>

              <label className="block">
                <span className="mb-2 block text-sm font-bold text-slate-800">Şifre</span>
                <span className="relative block">
                  <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-slate-400" aria-hidden="true" />
                  <input
                    type={passwordVisible ? 'text' : 'password'}
                    {...register('password')}
                    autoComplete="current-password"
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? 'login-password-error' : undefined}
                    className="h-13 w-full rounded-xl border border-slate-200 bg-white pl-11 pr-12 text-sm text-slate-950 outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                  />
                  <button
                    type="button"
                    onClick={() => setPasswordVisible((visible) => !visible)}
                    className="absolute right-2 top-1/2 flex size-9 -translate-y-1/2 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-brand-500"
                    aria-label={passwordVisible ? 'Şifreyi gizle' : 'Şifreyi göster'}
                  >
                    {passwordVisible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
                  </button>
                </span>
                {errors.password ? <FieldError id="login-password-error" message={errors.password.message} /> : null}
              </label>

              <button
                type="submit"
                disabled={isSubmitting}
                className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                {isSubmitting ? 'Giriş yapılıyor…' : 'Giriş Yap'}
                {isSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
              </button>
            </form>
          </div>

          <p className="mt-6 text-center text-xs leading-5 text-slate-500">
            Hesabınız bulunmuyorsa sistem yöneticinizle iletişime geçin.
          </p>
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
    <span id={id} className="mt-1.5 block text-xs font-semibold text-rose-700" role="alert">
      {message}
    </span>
  )
}
