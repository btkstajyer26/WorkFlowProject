import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import {
  ArrowRight,
  CheckCircle2,
  Eye,
  EyeOff,
  FileClock,
  FileText,
  LockKeyhole,
  Mail,
  ShieldCheck,
  UserRound,
  UserRoundPlus,
} from 'lucide-react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import {
  loginSchema,
  registrationSchema,
  type LoginFormValues,
  type RegistrationFormValues,
} from '../schemas/auth'
import { defaultDemoAccount, demoAccounts } from '../mocks/users'
import { readMockRegistrationRequests } from '../mocks/registrationRequests'
import type { AuthUser } from '../types/auth'
import type {
  CreateRegistrationRequestInput,
  RegistrationRequest,
} from '../types/registration'

type AuthMode = 'login' | 'register'

type LoginPageProps = {
  user: AuthUser | null
  onLogin: (user: AuthUser) => void
  onRegister: (
    input: CreateRegistrationRequestInput,
  ) => RegistrationRequest | Promise<RegistrationRequest>
}

export function LoginPage({ user, onLogin, onRegister }: LoginPageProps) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [mode, setMode] = useState<AuthMode>('login')
  const [passwordVisible, setPasswordVisible] = useState(false)
  const [registrationRequest, setRegistrationRequest] = useState<RegistrationRequest | null>(null)
  const {
    register: registerLogin,
    handleSubmit: handleLoginSubmit,
    setError: setLoginError,
    reset: resetLogin,
    formState: { errors: loginErrors, isSubmitting: loginSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: import.meta.env.DEV ? defaultDemoAccount.email : '',
      password: import.meta.env.DEV ? defaultDemoAccount.password : '',
    },
  })
  const {
    register: registerAccount,
    handleSubmit: handleRegistrationSubmit,
    setError: setRegistrationError,
    reset: resetRegistration,
    formState: { errors: registrationErrors, isSubmitting: registrationSubmitting },
  } = useForm<RegistrationFormValues>({
    resolver: zodResolver(registrationSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  if (user) return <Navigate to="/dashboard" replace />

  const reason = searchParams.get('reason')
  const submitLogin = handleLoginSubmit(async (values) => {
    const normalizedEmail = values.email.trim().toLowerCase()
    const storedRegistrationRequest = readMockRegistrationRequests().find(
      (request) => request.email === normalizedEmail,
    )

    if (storedRegistrationRequest?.status === 'PENDING') {
      setLoginError('email', {
        type: 'validate',
        message: 'Kayıt talebiniz sistem yöneticisinin onayını bekliyor.',
      })
      return
    }

    if (storedRegistrationRequest?.status === 'REJECTED') {
      setLoginError('email', {
        type: 'validate',
        message: 'Kayıt talebiniz sistem yöneticisi tarafından reddedildi.',
      })
      return
    }

    const matchingDemoAccount = demoAccounts.find(
      (account) => account.email === normalizedEmail,
    )

    if (matchingDemoAccount && values.password !== matchingDemoAccount.password) {
      setLoginError('password', { type: 'validate', message: 'Demo hesabı için şifre hatalı.' })
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
          email: normalizedEmail,
          role: 'CALISAN',
        }

    onLogin(authenticatedUser)

    const requestedPath = searchParams.get('returnTo')
    const safeReturnTo = requestedPath?.startsWith('/') && !requestedPath.startsWith('//')
      ? requestedPath
      : '/dashboard'
    navigate(safeReturnTo, { replace: true })
  })

  const submitRegistration = handleRegistrationSubmit(async (values) => {
    try {
      await new Promise<void>((resolve) => window.setTimeout(resolve, 350))
      const request = await onRegister({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
      })
      setRegistrationRequest(request)
    } catch (error) {
      setRegistrationError('root', {
        message: error instanceof Error
          ? error.message
          : 'Kayıt talebiniz oluşturulamadı. Lütfen tekrar deneyin.',
      })
    }
  })

  const showRegistration = () => {
    setMode('register')
    setPasswordVisible(false)
    setRegistrationRequest(null)
  }

  const showLogin = () => {
    setMode('login')
    setPasswordVisible(false)
    setRegistrationRequest(null)
    resetRegistration()
    resetLogin({
      email: import.meta.env.DEV ? defaultDemoAccount.email : '',
      password: import.meta.env.DEV ? defaultDemoAccount.password : '',
    })
  }

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

      <section className={`relative min-h-screen overflow-clip px-4 py-8 sm:px-8 lg:px-12 xl:px-16 ${mode === 'register' && !registrationRequest ? 'block' : 'flex items-center justify-center'}`}>
        <div className="pointer-events-none absolute -right-40 -top-40 size-[30rem] rounded-full bg-brand-100/70 dark:bg-brand-900/45 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 left-10 size-[26rem] rounded-full bg-blue-100/55 dark:bg-blue-900/60 blur-3xl" />

        <div className="relative mx-auto w-full max-w-2xl rounded-[1.75rem] border border-white/80 bg-app-surface/90 p-5 shadow-2xl shadow-slate-900/[0.08] backdrop-blur sm:p-8 lg:p-10 xl:p-12">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-3">
              <span className="flex size-11 items-center justify-center rounded-2xl bg-brand-700 text-white shadow-lg shadow-brand-200 dark:shadow-black/20">
                <FileText className="size-6" aria-hidden="true" />
              </span>
              <div>
                <p className="text-lg font-bold text-app-text">EBYS</p>
                <p className="text-[11px] text-app-text-subtle">İş Akışı ve Onay Yönetim Sistemi</p>
              </div>
            </div>
          </div>

          <div className="mx-auto max-w-xl">
            {registrationRequest ? (
              <div className="py-4 text-center" role="status">
                <span className="mx-auto flex size-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:ring-emerald-800/70">
                  <CheckCircle2 className="size-8" aria-hidden="true" />
                </span>
                <h1 className="mt-6 text-3xl font-medium tracking-[-0.03em] text-app-text sm:text-4xl">
                  Kayıt talebiniz alındı
                </h1>
                <p className="mx-auto mt-3 max-w-md text-sm leading-6 text-app-text-muted">
                  <strong className="font-bold text-app-text-emphasis">{registrationRequest.email}</strong> adresi
                  için oluşturduğunuz Çalışan hesabı talebi sistem yöneticisine iletildi.
                </p>
                <span className="mt-6 inline-flex items-center gap-2 rounded-full bg-amber-50 px-3.5 py-2 text-xs font-bold text-amber-800 ring-1 ring-inset ring-amber-200 dark:bg-amber-950/40 dark:text-amber-200 dark:ring-amber-800/70">
                  <span className="size-2 rounded-full bg-amber-500" aria-hidden="true" />
                  Admin onayı bekleniyor
                </span>
                <p className="mx-auto mt-5 max-w-md text-xs leading-5 text-app-text-subtle">
                  Hesabınız onaylandıktan sonra kurumsal e-posta adresiniz ve belirlediğiniz şifreyle giriş yapabilirsiniz.
                </p>
                <button
                  type="button"
                  onClick={showLogin}
                  className="mt-7 flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
                >
                  Giriş ekranına dön
                  <ArrowRight className="size-4" aria-hidden="true" />
                </button>
              </div>
            ) : (
              <>
                <h1 className="text-3xl font-medium tracking-[-0.03em] text-app-text sm:text-4xl">
                  {mode === 'login' ? 'Hesabınıza giriş yapın' : 'Kayıt olun'}
                </h1>
                <p className="mt-3 text-sm leading-6 text-app-text-muted">
                  {mode === 'login'
                    ? 'Devam etmek için kurumsal hesap bilgilerinizi girin.'
                    : 'Çalışan hesabı talebinizi oluşturmak için bilgilerinizi girin.'}
                </p>

                {reason === 'expired' && mode === 'login' ? (
              <p className="mt-5 rounded-xl border border-amber-200 dark:border-amber-800/70 bg-amber-50 dark:bg-amber-950/40 px-4 py-3 text-sm font-semibold text-amber-900 dark:text-amber-200" role="status">
                Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.
              </p>
                ) : null}

                {mode === 'login' ? (
                  <form className="mt-8 space-y-5" noValidate onSubmit={submitLogin}>
                    <label className="block">
                      <span className="mb-2 block text-sm font-bold text-app-text-emphasis">E-posta adresi</span>
                      <span className="relative block">
                        <Mail className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                        <input
                          type="email"
                          {...registerLogin('email')}
                          autoComplete="username"
                          placeholder="ad.soyad@kurum.gov.tr"
                          aria-invalid={Boolean(loginErrors.email)}
                          aria-describedby={loginErrors.email ? 'login-email-error' : undefined}
                          className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                        />
                      </span>
                      {loginErrors.email ? <FieldError id="login-email-error" message={loginErrors.email.message} /> : null}
                    </label>

                    <label className="block">
                      <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Şifre</span>
                      <span className="relative block">
                        <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                        <input
                          type={passwordVisible ? 'text' : 'password'}
                          {...registerLogin('password')}
                          autoComplete="current-password"
                          aria-invalid={Boolean(loginErrors.password)}
                          aria-describedby={loginErrors.password ? 'login-password-error' : undefined}
                          className="h-13 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-12 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                        />
                        <PasswordVisibilityButton visible={passwordVisible} onToggle={() => setPasswordVisible((visible) => !visible)} />
                      </span>
                      {loginErrors.password ? <FieldError id="login-password-error" message={loginErrors.password.message} /> : null}
                    </label>

                    <button
                      type="submit"
                      disabled={loginSubmitting}
                      className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
                    >
                      {loginSubmitting ? 'Giriş yapılıyor…' : 'Giriş Yap'}
                      {loginSubmitting ? null : <ArrowRight className="size-4" aria-hidden="true" />}
                    </button>
                  </form>
                ) : (
                  <form className="mt-7 space-y-4" noValidate onSubmit={submitRegistration}>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <label className="block">
                        <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Ad</span>
                        <span className="relative block">
                          <UserRound className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                          <input
                            {...registerAccount('firstName')}
                            autoComplete="given-name"
                            aria-invalid={Boolean(registrationErrors.firstName)}
                            aria-describedby={registrationErrors.firstName ? 'register-first-name-error' : undefined}
                            className="h-12 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                          />
                        </span>
                        {registrationErrors.firstName ? <FieldError id="register-first-name-error" message={registrationErrors.firstName.message} /> : null}
                      </label>

                      <label className="block">
                        <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Soyad</span>
                        <span className="relative block">
                          <UserRound className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                          <input
                            {...registerAccount('lastName')}
                            autoComplete="family-name"
                            aria-invalid={Boolean(registrationErrors.lastName)}
                            aria-describedby={registrationErrors.lastName ? 'register-last-name-error' : undefined}
                            className="h-12 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                          />
                        </span>
                        {registrationErrors.lastName ? <FieldError id="register-last-name-error" message={registrationErrors.lastName.message} /> : null}
                      </label>
                    </div>

                    <label className="block">
                      <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Kurumsal e-posta adresi</span>
                      <span className="relative block">
                        <Mail className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                        <input
                          type="email"
                          {...registerAccount('email')}
                          autoComplete="email"
                          placeholder="ad.soyad@kurum.gov.tr"
                          aria-invalid={Boolean(registrationErrors.email)}
                          aria-describedby={registrationErrors.email ? 'register-email-error' : undefined}
                          className="h-12 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                        />
                      </span>
                      {registrationErrors.email ? <FieldError id="register-email-error" message={registrationErrors.email.message} /> : null}
                    </label>

                    <div className="grid gap-4 sm:grid-cols-2">
                      <label className="block">
                        <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Şifre</span>
                        <span className="relative block">
                          <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                          <input
                            type={passwordVisible ? 'text' : 'password'}
                            {...registerAccount('password')}
                            autoComplete="new-password"
                            aria-invalid={Boolean(registrationErrors.password)}
                            aria-describedby={registrationErrors.password ? 'register-password-error' : undefined}
                            className="h-12 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-12 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                          />
                          <PasswordVisibilityButton visible={passwordVisible} onToggle={() => setPasswordVisible((visible) => !visible)} />
                        </span>
                        {registrationErrors.password ? <FieldError id="register-password-error" message={registrationErrors.password.message} /> : null}
                      </label>

                      <label className="block">
                        <span className="mb-2 block text-sm font-bold text-app-text-emphasis">Şifre tekrarı</span>
                        <span className="relative block">
                          <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-app-text-faint" aria-hidden="true" />
                          <input
                            type={passwordVisible ? 'text' : 'password'}
                            {...registerAccount('confirmPassword')}
                            autoComplete="new-password"
                            aria-invalid={Boolean(registrationErrors.confirmPassword)}
                            aria-describedby={registrationErrors.confirmPassword ? 'register-confirm-password-error' : undefined}
                            className="h-12 w-full rounded-xl border border-app-border bg-app-surface pl-11 pr-4 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                          />
                        </span>
                        {registrationErrors.confirmPassword ? <FieldError id="register-confirm-password-error" message={registrationErrors.confirmPassword.message} /> : null}
                      </label>
                    </div>

                    <div className="flex gap-3 rounded-xl border border-brand-100 bg-brand-50/70 px-4 py-3 text-xs leading-5 text-brand-800 dark:border-brand-800/60 dark:bg-brand-900/25 dark:text-brand-200">
                      <ShieldCheck className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                      <p>
                        Talebiniz <strong>Çalışan</strong> rolüyle oluşturulur. Hesabınız, sistem yöneticisi onayladıktan sonra kullanıma açılır.
                      </p>
                    </div>

                    {registrationErrors.root ? (
                      <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
                        {registrationErrors.root.message}
                      </p>
                    ) : null}

                    <button
                      type="submit"
                      disabled={registrationSubmitting}
                      className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-brand-700 px-5 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:from-brand-700 hover:to-brand-800 disabled:cursor-wait disabled:opacity-70 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 dark:shadow-black/20"
                    >
                      {registrationSubmitting ? 'Talep gönderiliyor…' : 'Kayıt Talebi Gönder'}
                      {registrationSubmitting ? null : <UserRoundPlus className="size-4" aria-hidden="true" />}
                    </button>
                  </form>
                )}

                <div className="mt-6 flex items-center justify-center gap-1.5 text-center text-xs leading-5 text-app-text-subtle">
                  <span>{mode === 'login' ? 'Henüz hesabınız yok mu?' : 'Zaten hesabınız var mı?'}</span>
                  <button
                    type="button"
                    onClick={mode === 'login' ? showRegistration : showLogin}
                    className="rounded-md px-1 py-0.5 font-bold text-brand-700 transition hover:text-brand-800 hover:underline focus-visible:outline-2 focus-visible:outline-brand-500 dark:text-brand-300"
                  >
                    {mode === 'login' ? 'Kayıt Ol' : 'Giriş Yap'}
                  </button>
                </div>
              </>
            )}
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
