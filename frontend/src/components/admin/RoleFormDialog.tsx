import { zodResolver } from '@hookform/resolvers/zod'
import { ShieldCheck, ShieldPlus } from 'lucide-react'
import { useEffect, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { ApiClientError } from '../../api/errors'
import { useToast } from '../../context/toastState'
import { roleFormSchema, type RoleFormValues } from '../../schemas/admin'
import type { AdminRole } from '../../types/admin'
import { AdminDialog } from './AdminDialog'

type RoleFormDialogProps = {
  open: boolean
  /** null ise yeni rol açılır, dolu ise mevcut rol düzenlenir. */
  role: AdminRole | null
  onClose: () => void
  onSubmit: (values: RoleFormValues) => Promise<unknown>
}

const emptyValues: RoleFormValues = { name: '', description: '', workflowActor: false }

export function RoleFormDialog({ open, role, onClose, onSubmit }: RoleFormDialogProps) {
  const { showToast } = useToast()
  const editing = role !== null
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RoleFormValues>({
    resolver: zodResolver(roleFormSchema),
    defaultValues: emptyValues,
  })

  useEffect(() => {
    if (!open) return
    reset(role
      ? { name: role.name, description: role.description ?? '', workflowActor: role.isWorkflowActor }
      : emptyValues)
  }, [open, reset, role])

  const closeDialog = () => {
    if (isSubmitting) return
    onClose()
  }

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit(values)
      showToast({
        title: editing ? 'Rol güncellendi' : 'Rol oluşturuldu',
        description: `${values.name} kaydedildi.`,
        tone: 'success',
      })
      onClose()
    } catch (caught) {
      if (caught instanceof ApiClientError) {
        let fieldErrorMapped = false
        caught.fieldErrors.forEach((fieldError) => {
          if (fieldError.field !== 'name' && fieldError.field !== 'description') return
          setError(fieldError.field, { type: 'server', message: fieldError.message })
          fieldErrorMapped = true
        })
        if (fieldErrorMapped) return

        // Ad çakışması ve sistem rolü korumaları alan bazlı değil, kural ihlali
        // olarak döner; mesajı olduğu gibi formun üstünde gösteririz.
        setError('root', { type: 'server', message: caught.message })
        return
      }

      setError('root', {
        type: 'server',
        message: 'Rol kaydedilemedi. Lütfen tekrar deneyin.',
      })
    }
  })

  return (
    <AdminDialog
      open={open}
      onClose={closeDialog}
      icon={editing ? ShieldCheck : ShieldPlus}
      title={editing ? 'Rolü düzenle' : 'Yeni rol'}
      description={editing
        ? 'Rolün görünen adını, açıklamasını ve workflow aktörlüğünü güncelleyin.'
        : 'Panelden açılan roller sistem rolü değildir ve kullanıcı sınırı taşımaz.'}
    >
      <form className="mt-6 space-y-4" noValidate onSubmit={submit}>
        <FormField fieldId="role-name" label="Rol adı" error={errors.name?.message} errorId="role-name-error">
          <input
            id="role-name"
            {...register('name')}
            aria-invalid={Boolean(errors.name)}
            aria-describedby={errors.name ? 'role-name-error' : undefined}
            className="min-h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          />
        </FormField>

        <FormField fieldId="role-description" label="Açıklama" error={errors.description?.message} errorId="role-description-error">
          <textarea
            id="role-description"
            rows={3}
            {...register('description')}
            aria-invalid={Boolean(errors.description)}
            aria-describedby={errors.description ? 'role-description-error' : undefined}
            className="w-full rounded-xl border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none transition focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
          />
        </FormField>

        <label className={`flex gap-3 rounded-xl border border-app-border px-4 py-3 text-sm ${role?.isSystem ? 'opacity-60' : ''}`}>
          <input
            type="checkbox"
            {...register('workflowActor')}
            disabled={role?.isSystem}
            className="mt-0.5 size-4 shrink-0 accent-brand-700"
          />
          <span>
            <span className="block font-bold text-app-text-emphasis">İş akışı aktörü</span>
            <span className="mt-0.5 block text-xs leading-5 text-app-text-muted">
              {role?.isSystem
                ? 'Sistem rolünün aktörlüğü değiştirilemez.'
                : 'İşaretlenirse rol, mevcut geçişlere aktör olarak bağlanabilir.'}
            </span>
          </span>
        </label>

        {errors.root ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
            {errors.root.message}
          </p>
        ) : null}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={closeDialog}
            className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted disabled:opacity-60"
          >
            Vazgeç
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 disabled:cursor-wait disabled:opacity-60"
          >
            {isSubmitting ? 'Kaydediliyor…' : editing ? 'Kaydet' : 'Rol Oluştur'}
          </button>
        </div>
      </form>
    </AdminDialog>
  )
}

function FormField({
  fieldId,
  label,
  error,
  errorId,
  children,
}: {
  fieldId: string
  label: string
  error?: string
  errorId: string
  children: ReactNode
}) {
  return (
    <div className="block">
      <label htmlFor={fieldId} className="mb-1.5 block text-sm font-bold text-app-text-emphasis">{label}</label>
      {children}
      {error ? (
        <span id={errorId} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
          {error}
        </span>
      ) : null}
    </div>
  )
}
