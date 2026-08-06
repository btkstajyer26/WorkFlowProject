import { UserCheck, UserX } from 'lucide-react'
import { useState } from 'react'
import { useAdmin } from '../../context/adminState'
import type { ManagedUser } from '../../types/admin'
import { AdminDialog } from './AdminDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'

export function AccountStatusDialog({
  user,
  open,
  onClose,
}: {
  user: ManagedUser | null
  open: boolean
  onClose: () => void
}) {
  const { setUserActive } = useAdmin()
  const [error, setError] = useState<string | null>(null)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const willActivate = !user?.isActive
  const Icon = willActivate ? UserCheck : UserX

  const closeDialog = () => {
    setError(null)
    onClose()
  }

  return (
    <AdminDialog
      open={open && Boolean(user)}
      onClose={closeDialog}
      icon={Icon}
      title={willActivate ? 'Hesabı etkinleştir' : 'Hesabı pasifleştir'}
      description={user
        ? `${user.firstName} ${user.lastName} kullanıcısının erişim durumunu değiştirmek üzeresiniz.`
        : ''}
    >
      <div className="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        {willActivate
          ? 'Kullanıcı yeniden giriş yapabilir ve rolünün izin verdiği işlemleri gerçekleştirebilir.'
          : 'Kullanıcı sisteme giriş yapamaz. Backend mevcut access/refresh tokenlarını da geçersiz kılmalıdır.'}
      </div>
      {error ? <p className="mt-3 text-sm font-semibold text-rose-700" role="alert">{error}</p> : null}
      <div className="mt-6 grid grid-cols-2 gap-3">
        <button type="button" onClick={closeDialog} className="min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-700 hover:bg-slate-50">Vazgeç</button>
        <button
          type="button"
          disabled={mutationBusy}
          onClick={() => runMutation(() => {
            if (!user) return
            try {
              setUserActive(user.id, willActivate)
              closeDialog()
            } catch (caught) {
              setError(caught instanceof Error ? caught.message : 'Hesap durumu değiştirilemedi.')
            }
          })}
          className={`min-h-11 rounded-xl px-4 text-sm font-bold text-white disabled:opacity-60 ${willActivate ? 'bg-emerald-700 hover:bg-emerald-800' : 'bg-rose-600 hover:bg-rose-700'}`}
        >
          {willActivate ? 'Etkinleştir' : 'Pasifleştir'}
        </button>
      </div>
    </AdminDialog>
  )
}
