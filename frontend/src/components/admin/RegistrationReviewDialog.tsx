import { CheckCircle2, ClipboardCheck, LockKeyhole, UserX } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useAdmin } from '../../context/adminState'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { roleLabels, type WorkflowRole } from '../../types/auth'
import type { RegistrationRequest } from '../../types/registration'
import { AdminDialog } from './AdminDialog'

const approvableRoles: WorkflowRole[] = ['CALISAN', 'BASKAN_YARDIMCISI', 'BASKAN']

export function RegistrationReviewDialog({
  request,
  open,
  onClose,
}: {
  request: RegistrationRequest | null
  open: boolean
  onClose: () => void
}) {
  const { users, approveRegistration, rejectRegistration } = useAdmin()
  const [role, setRole] = useState<WorkflowRole>('CALISAN')
  const [error, setError] = useState<string | null>(null)
  const { busy, run } = useSingleFlight()

  useEffect(() => {
    setRole('CALISAN')
    setError(null)
  }, [request])

  const activeDeputy = role === 'BASKAN_YARDIMCISI'
    ? users.find((user) => user.isActive && user.role === 'BASKAN_YARDIMCISI')
    : undefined

  const decide = (decision: 'approve' | 'reject') => {
    void run(() => {
      if (!request) return
      try {
        if (decision === 'approve') approveRegistration(request.id, role)
        else rejectRegistration(request.id)
        onClose()
      } catch (actionError) {
        setError(actionError instanceof Error ? actionError.message : 'Kayıt talebi işlenemedi.')
      }
    })
  }

  return (
    <AdminDialog
      open={open && Boolean(request)}
      onClose={onClose}
      icon={ClipboardCheck}
      title="Kayıt talebini incele"
      description="Başvuru bilgilerini kontrol edip hesabı onaylayın veya talebi reddedin."
    >
      {request ? (
        <div className="mt-6">
          <dl className="grid gap-3 rounded-2xl border border-app-border bg-app-surface-muted p-4 sm:grid-cols-2">
            <RequestDetail label="Ad" value={request.firstName} />
            <RequestDetail label="Soyad" value={request.lastName} />
            <RequestDetail label="Kurumsal e-posta" value={request.email} wide />
            <RequestDetail label="Başvuru tarihi" value={formatDateTime(request.createdAt)} wide />
          </dl>

          <div className="mt-4 flex gap-3 rounded-xl border border-app-border bg-app-surface px-4 py-3">
            <LockKeyhole className="mt-0.5 size-4 shrink-0 text-app-text-faint" aria-hidden="true" />
            <div>
              <p className="text-xs font-bold text-app-text-emphasis">Şifre korumalıdır</p>
              <p className="mt-0.5 text-xs leading-5 text-app-text-subtle">
                Kullanıcının belirlediği şifre güvenlik nedeniyle görüntülenemez veya değiştirilemez.
              </p>
            </div>
          </div>

          <label className="mt-5 block">
            <span className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Onaylanacak rol</span>
            <select
              aria-label="Onaylanacak rol"
              value={role}
              onChange={(event) => setRole(event.target.value as WorkflowRole)}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            >
              {approvableRoles.map((value) => (
                <option key={value} value={value}>{roleLabels[value]}</option>
              ))}
            </select>
            <p className="mt-1.5 text-xs leading-5 text-app-text-subtle">
              Başvurular varsayılan olarak Çalışan rolüyle açılır. Admin rolü başvuru onayından verilemez.
            </p>
          </label>

          {activeDeputy ? (
            <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm leading-6 text-amber-900 dark:border-amber-800/70 dark:bg-amber-950/40 dark:text-amber-200">
              Onaylandığında mevcut Başkan Yardımcısı <strong>{activeDeputy.firstName} {activeDeputy.lastName}</strong> Çalışan rolüne geçirilecektir.
            </p>
          ) : null}

          {error ? (
            <p className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
              {error}
            </p>
          ) : null}

          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              disabled={busy}
              onClick={() => decide('reject')}
              className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 text-sm font-bold text-rose-700 transition hover:bg-rose-100 disabled:opacity-60 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300 dark:hover:bg-rose-900/60"
            >
              <UserX className="size-4" aria-hidden="true" />
              Talebi Reddet
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => decide('approve')}
              className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:opacity-60"
            >
              <CheckCircle2 className="size-4" aria-hidden="true" />
              Onayla ve Hesabı Aç
            </button>
          </div>
        </div>
      ) : null}
    </AdminDialog>
  )
}

function RequestDetail({
  label,
  value,
  wide = false,
}: {
  label: string
  value: string
  wide?: boolean
}) {
  return (
    <div className={wide ? 'sm:col-span-2' : undefined}>
      <dt className="text-[11px] font-bold uppercase tracking-wide text-app-text-subtle">{label}</dt>
      <dd className="mt-1 break-words text-sm font-semibold text-app-text-strong">{value}</dd>
    </div>
  )
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(value))
}
