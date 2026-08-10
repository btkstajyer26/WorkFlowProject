import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRightLeft } from 'lucide-react'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useAdmin } from '../../context/adminState'
import { useToast } from '../../context/toastState'
import { changeRoleSchema, type ChangeRoleFormValues } from '../../schemas/admin'
import { roleLabels } from '../../types/auth'
import type { ManagedUser } from '../../types/admin'
import { AdminDialog } from './AdminDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'

export function ChangeRoleDialog({
  user,
  open,
  onClose,
}: {
  user: ManagedUser | null
  open: boolean
  onClose: () => void
}) {
  const { users, changeUserRole } = useAdmin()
  const { showToast } = useToast()
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const { register, handleSubmit, reset, watch, setError, formState: { errors } } = useForm<ChangeRoleFormValues>({
    resolver: zodResolver(changeRoleSchema),
    defaultValues: { role: 'CALISAN' },
  })

  useEffect(() => {
    if (user && user.role !== 'ADMIN') reset({ role: user.role })
  }, [reset, user])

  const selectedRole = watch('role')
  const activeDeputy = selectedRole === 'BASKAN_YARDIMCISI'
    ? users.find((item) => item.id !== user?.id && item.isActive && item.role === 'BASKAN_YARDIMCISI')
    : undefined

  const submit = handleSubmit((values) => runMutation(() => {
    if (!user) return
    try {
      changeUserRole(user.id, values.role)
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
        {errors.root ? <p className="mt-3 text-sm font-semibold text-rose-700 dark:text-rose-300" role="alert">{errors.root.message}</p> : null}
        <div className="mt-6 grid grid-cols-2 gap-3">
          <button type="button" onClick={onClose} className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary hover:bg-app-surface-muted">Vazgeç</button>
          <button type="submit" disabled={mutationBusy} className="min-h-11 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white hover:bg-brand-800 disabled:opacity-60">Değiştir</button>
        </div>
      </form>
    </AdminDialog>
  )
}
