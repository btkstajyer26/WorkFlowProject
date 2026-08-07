import { BadgeCheck, Building2, Info, Mail, ShieldCheck, UserRound } from 'lucide-react'
import { roleLabels, type AuthUser } from '../types/auth'

function getInitials(user: AuthUser) {
  return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toLocaleUpperCase('tr-TR')
}

export function ProfilePage({ user }: { user: AuthUser }) {
  const fullName = `${user.firstName} ${user.lastName}`

  return (
    <div className="space-y-5">
      <header>
        <p className="text-sm font-semibold text-brand-600 dark:text-brand-400">Hesabım</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">Profil</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-app-text-subtle">
          Kurumsal hesabınıza bağlı temel kullanıcı ve yetki bilgilerinizi görüntüleyin.
        </p>
      </header>

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <section className="overflow-hidden rounded-2xl border border-app-border bg-app-surface shadow-sm">
          <div className="h-28 bg-gradient-to-r from-brand-800 via-brand-700 to-violet-500 sm:h-36" />
          <div className="px-5 pb-6 sm:px-7 sm:pb-8">
            <div className="-mt-12 flex flex-col gap-4 sm:-mt-14 sm:flex-row sm:items-end sm:justify-between">
              <div className="flex items-start gap-4">
                <div className="flex size-24 shrink-0 items-center justify-center rounded-3xl border-4 border-white bg-brand-50 dark:bg-brand-900/30 text-2xl font-extrabold text-brand-700 dark:text-brand-300 shadow-md sm:size-28">
                  {getInitials(user)}
                </div>
                <div className="min-w-0 pt-[3.75rem] sm:pt-16">
                  <h2 className="truncate text-xl font-bold text-app-text sm:text-2xl">{fullName}</h2>
                  <div className="mt-1 flex items-center gap-1.5 text-sm font-medium text-app-text-subtle">
                    <Building2 className="size-4" aria-hidden="true" />
                    Kurumsal kullanıcı
                  </div>
                </div>
              </div>
              <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-emerald-50 dark:bg-emerald-950/40 px-3 py-1.5 text-xs font-bold text-emerald-700 dark:text-emerald-300 ring-1 ring-inset ring-emerald-200 dark:ring-emerald-800/70">
                <span className="size-2 rounded-full bg-emerald-500" aria-hidden="true" />
                Aktif hesap
              </span>
            </div>

            <div className="mt-7 grid gap-3 sm:grid-cols-2">
              <ProfileField
                icon={UserRound}
                label="Ad soyad"
                value={fullName}
              />
              <ProfileField
                icon={Mail}
                label="E-posta adresi"
                value={user.email}
              />
              <ProfileField
                icon={ShieldCheck}
                label="Sistem rolü"
                value={roleLabels[user.role]}
              />
              <ProfileField
                icon={BadgeCheck}
                label="Hesap türü"
                value="Kurumsal hesap"
              />
            </div>
          </div>
        </section>

        <aside className="space-y-5">
          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
            <div className="flex size-11 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
              <ShieldCheck className="size-5" aria-hidden="true" />
            </div>
            <h2 className="mt-4 text-lg font-bold text-app-text">Yetki ve güvenlik</h2>
            <p className="mt-2 text-sm leading-6 text-app-text-muted">
              Ekran ve işlem yetkileriniz sistem rolünüze göre belirlenir. Rolünüzü veya hesap bilgilerinizi
              bu ekrandan değiştiremezsiniz.
            </p>
            <div className="mt-5 rounded-xl bg-app-surface-muted p-4">
              <p className="text-xs font-bold uppercase tracking-[0.12em] text-app-text-subtle">Mevcut yetki</p>
              <p className="mt-1.5 font-bold text-app-text-strong">{roleLabels[user.role]}</p>
            </div>
          </section>

          <section className="flex gap-3 rounded-2xl border border-blue-100 dark:border-blue-900/70 bg-blue-50/70 dark:bg-blue-950/40 p-4 text-blue-950 dark:text-blue-100">
            <Info className="mt-0.5 size-5 shrink-0 text-blue-600 dark:text-blue-400" aria-hidden="true" />
            <div>
              <h2 className="text-sm font-bold">Bilgilerinizde hata mı var?</h2>
              <p className="mt-1 text-sm leading-6 text-blue-800 dark:text-blue-200">
                Profil bilgileri kurum yöneticisi tarafından yönetilir. Güncelleme için yöneticinizle iletişime geçin.
              </p>
            </div>
          </section>
        </aside>
      </div>
    </div>
  )
}

type ProfileFieldProps = {
  icon: typeof UserRound
  label: string
  value: string
}

function ProfileField({ icon: Icon, label, value }: ProfileFieldProps) {
  return (
    <div className="flex min-w-0 items-center gap-3 rounded-xl border border-app-border bg-app-surface-muted/70 p-4">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-app-surface text-brand-700 dark:text-brand-300 shadow-sm ring-1 ring-app-border">
        <Icon className="size-[18px]" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-semibold text-app-text-subtle">{label}</p>
        <p className="mt-0.5 truncate text-sm font-bold text-app-text-strong" title={value}>{value}</p>
      </div>
    </div>
  )
}
