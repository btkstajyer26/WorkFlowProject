import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRightLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useAdmin } from '../../context/adminState'
import { useToast } from '../../context/toastState'
import { changeRoleSchema, type ChangeRoleFormValues } from '../../schemas/admin'
import { roleLabels } from '../../types/auth'
import type { ManagedUser } from '../../types/admin'
import { AdminDialog } from './AdminDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { useAdminUserOptions } from '../../hooks/useAdminUserOptions'

export function ChangeRoleDialog({
  user,
  open,
  onClose,
}: {
  user: ManagedUser | null
  open: boolean
  onClose: () => void
}) {
  const { changeUserRole } = useAdmin()
  const userOptions = useAdminUserOptions(open)
  const { showToast } = useToast()
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const [replacementDeputyId, setReplacementDeputyId] = useState('')
  const { register, handleSubmit, reset, watch, setError, formState: { errors } } = useForm<ChangeRoleFormValues>({
    resolver: zodResolver(changeRoleSchema),
    defaultValues: { role: 'CALISAN' },
  })

  useEffect(() => {
    if (user && user.systemKey && user.systemKey !== 'ADMIN') {
      reset({ role: user.systemKey })
      setReplacementDeputyId('')
    }
  }, [reset, user])

  const selectedRole = watch('role')
  const activeDeputy = selectedRole === 'BASKAN_YARDIMCISI'
    ? userOptions.users.find((item) => item.id !== user?.id && item.isActive && item.systemKey === 'BASKAN_YARDIMCISI')
    : undefined
  const leavingDeputy = user?.systemKey === 'BASKAN_YARDIMCISI' && selectedRole !== 'BASKAN_YARDIMCISI'
  const replacementCandidates = userOptions.users.filter((item) => (
    item.id !== user?.id && item.isActive && item.systemKey === 'CALISAN'
  ))

  const submit = handleSubmit((values) => runMutation(async () => {
    if (!user) return
    try {
      if (leavingDeputy && !replacementDeputyId) {
        setError('root', { message: 'Yerine atanacak aktif Çalışanı seçin.' })
        return
      }
      await changeUserRole(
        user.id,
        values.role,
        replacementDeputyId || undefined,
        activeDeputy?.id,
      )
      showToast({
        title: 'Kullanıcı rolü güncellendi',
        description: `${user.firstName} ${user.lastName} artık ${roleLabels[values.role]} rolünde.`,
        tone: 'success',
      })
      onClose()
    } catch (error) {
      setError('root', { message: error instanceof Error ? error.message : 'Rol değiştirilemedi.' })
    }
  }))

  return (
    <AdminDialog
      open={open && Boolean(user)}
      onClose={onClose}
      icon={ArrowRightLeft}
      title="Kullanıcı rolünü değiştir"
      description={user ? `${user.firstName} ${user.lastName} için yeni rolü seçin.` : ''}
    >
      <form className="mt-6" onSubmit={submit}>
        <label className="block">
          <span className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Yeni rol</span>
          <select
            {...register('role')}
            className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          >
            <option value="CALISAN">{roleLabels.CALISAN}</option>
            <option value="BASKAN_YARDIMCISI">{roleLabels.BASKAN_YARDIMCISI}</option>
            <option value="BASKAN">{roleLabels.BASKAN}</option>
            <option value="ADMIN">{roleLabels.ADMIN}</option>
          </select>
        </label>
        {activeDeputy ? (
          <p className="mt-4 rounded-xl border border-amber-200 dark:border-amber-800/70 bg-amber-50 dark:bg-amber-950/40 p-3 text-sm leading-6 text-amber-900 dark:text-amber-200">
            Bu işlem yardımcılık rolünü <strong>{activeDeputy.firstName} {activeDeputy.lastName}</strong> kullanıcısından devralır. Mevcut yardımcı Çalışan rolüne geçirilir.
          </p>
        ) : null}
        {leavingDeputy ? (
          <label className="mt-4 block">
            <span className="mb-1.5 block text-sm font-bold text-app-text-emphasis">Yeni Başkan Yardımcısı</span>
            <select
              value={replacementDeputyId}
              onChange={(event) => setReplacementDeputyId(event.target.value)}
              className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
            >
              <option value="">Aktif bir Çalışan seçin</option>
              {replacementCandidates.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>{candidate.firstName} {candidate.lastName}</option>
              ))}
            </select>
          </label>
        ) : null}
        {userOptions.isPending ? (
          <p className="mt-3 text-sm font-semibold text-app-text-muted" role="status">Rol seçenekleri yükleniyor…</p>
        ) : null}
        {userOptions.isError ? (
          <p className="mt-3 text-sm font-semibold text-rose-700 dark:text-rose-300" role="alert">Rol seçenekleri yüklenemedi. Pencereyi kapatıp tekrar deneyin.</p>
        ) : null}
        {errors.root ? <p className="mt-3 text-sm font-semibold text-rose-700 dark:text-rose-300" role="alert">{errors.root.message}</p> : null}
        <div className="mt-6 grid grid-cols-2 gap-3">
          <button type="button" onClick={onClose} className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary hover:bg-app-surface-muted">Vazgeç</button>
          <button type="submit" disabled={mutationBusy || userOptions.isPending || userOptions.isError} className="min-h-11 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 disabled:opacity-60">Değiştir</button>
        </div>
      </form>
    </AdminDialog>
  )
}
