import { useRef, useState } from 'react'
import type { LucideIcon } from 'lucide-react'
import {
  ArrowLeftRight,
  CheckCircle2,
  FilePenLine,
  Send,
  X,
  XCircle,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { useToast } from '../../context/toastState'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import {
  useAvailableWorkflowActions,
  useRecordWorkflowAction,
  useWorkflowTargetDepartments,
} from '../../hooks/useRecordWorkflowAction'
import type { AvailableWorkflowAction } from '../../api/workflow'
import type { AuthUser } from '../../types/auth'
import type { WorkflowRecord } from '../../types/record'

type ActionTone = 'primary' | 'success' | 'danger' | 'secondary'

const actionPresentation: Record<string, { icon: LucideIcon; tone: ActionTone }> = {
  GONDER: { icon: Send, tone: 'primary' },
  TEKRAR_GONDER: { icon: Send, tone: 'primary' },
  BASKANA_ILET: { icon: Send, tone: 'primary' },
  DEPARTMANA_GONDER: { icon: Send, tone: 'primary' },
  CALISANA_GERI_GONDER: { icon: ArrowLeftRight, tone: 'secondary' },
  BASKAN_YARDIMCISINA_GERI_GONDER: { icon: ArrowLeftRight, tone: 'secondary' },
  ONAYLA: { icon: CheckCircle2, tone: 'success' },
  REDDET: { icon: XCircle, tone: 'danger' },
}
const defaultPresentation: { icon: LucideIcon; tone: ActionTone } = { icon: Send, tone: 'secondary' }

const buttonToneClasses: Record<ActionTone, string> = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800 focus-visible:outline-brand-500',
  success: 'bg-emerald-600 text-white hover:bg-emerald-700 focus-visible:outline-emerald-500',
  danger: 'border border-rose-300 text-rose-700 hover:bg-rose-50 focus-visible:outline-rose-500 dark:border-rose-800 dark:text-rose-300 dark:hover:bg-rose-950/40',
  secondary: 'border border-app-border text-app-text-secondary hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-brand-500',
}

/**
 * B10/WEB-1: hangi düğmelerin gösterileceğine sunucu karar verir
 * (`available-actions`, APP-9). Burada rol adına veya `systemKey`'e bağlı
 * hiçbir koşul yoktur; dinamik rol de kendi yetkili aksiyonlarını aynı
 * yoldan görür. Bu liste yalnız görünürlük içindir - `performWorkflowAction`
 * yetkiyi sunucuda ayrıca doğrular, düğmenin gizlenmesi tek başına
 * yetkilendirme sayılmaz.
 */
export function RecordActionPanel({
  record,
  user,
}: {
  record: WorkflowRecord
  user: AuthUser
}) {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const workflowMutation = useRecordWorkflowAction(record.id, user)
  const availableActionsQuery = useAvailableWorkflowActions(record.id)
  const [selectedAction, setSelectedAction] = useState<AvailableWorkflowAction | null>(null)
  const [comment, setComment] = useState('')
  const [targetDepartmentId, setTargetDepartmentId] = useState<number | null>(null)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()

  const targetDepartmentsQuery = useWorkflowTargetDepartments(
    record.id,
    Boolean(selectedAction?.targetDepartmentRequired && !selectedAction.targetUserRequired),
  )
  const departments = targetDepartmentsQuery.data ?? []
  const targetUnavailable = Boolean(selectedAction?.targetUserRequired) || Boolean(
    selectedAction?.targetDepartmentRequired && (
      targetDepartmentsQuery.isPending || targetDepartmentsQuery.isFetching ||
      targetDepartmentsQuery.isError || departments.length === 0
    ),
  )

  // Kayıt düzenleme bir workflow aksiyonu değildir (available-actions'ta yer almaz);
  // ayrı, izin tabanlı bir kontroldür - mobil B09/kayitlar/[id]'deki ile aynı desen.
  const canEdit = Boolean(
    user.permissionCodes.includes('RECORD_EDIT') &&
    record.createdById === user.id &&
    (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR'),
  )

  const openAction = (action: AvailableWorkflowAction) => {
    setComment('')
    setTargetDepartmentId(null)
    setSelectedAction(action)
  }

  const closeActionDialog = () => {
    setSelectedAction(null)
    setComment('')
    setTargetDepartmentId(null)
  }

  const actions = availableActionsQuery.data?.actions ?? []

  if (availableActionsQuery.isPending) return null
  if (!canEdit && (availableActionsQuery.isError || actions.length === 0)) return null

  const completeAction = () => runMutation(async () => {
    if (!selectedAction || targetUnavailable) return
    if (selectedAction.targetDepartmentRequired && targetDepartmentId === null) {
      showToast({ title: 'Hedef departman seçin', tone: 'error' })
      return
    }

    const normalizedComment = comment.trim()
    if (selectedAction.commentRequired && !normalizedComment) {
      showToast({ title: 'Bu işlem için açıklama zorunludur', tone: 'error' })
      return
    }

    try {
      await workflowMutation.mutateAsync({
        action: selectedAction.action,
        ...(selectedAction.targetDepartmentRequired && targetDepartmentId !== null
          ? { targetDepartmentId } : {}),
        ...(normalizedComment ? { comment: normalizedComment } : {}),
      })
      showToast({ title: 'İşlem tamamlandı', description: selectedAction.displayName, tone: 'success' })
      closeActionDialog()
      // Kayıt kendisine geri gönderilmediyse (ör. CALISANA_GERI_GONDER, kaydın
      // kendisine değil başka birine döndüğü durumlar hariç) listeye dön.
      navigate('/kayitlar')
    } catch (caughtError) {
      showToast({
        title: 'İşlem tamamlanamadı',
        description: caughtError instanceof Error ? caughtError.message : 'Kayıt işlemi sırasında bir hata oluştu.',
        tone: 'error',
      })
    }
  })

  return (
    <section aria-labelledby="record-actions-title" className="rounded-xl border border-app-border bg-app-surface px-4 py-4 sm:flex sm:items-center sm:justify-between sm:gap-6 sm:px-5">
      <div className="min-w-0">
        <h2 id="record-actions-title" className="text-base font-bold text-app-text">{canEdit ? 'Kayıt İşlemleri' : 'Karar'}</h2>
        <p className="mt-1 text-sm leading-5 text-app-text-muted">
          {canEdit
            ? 'Taslağınıza devam edin veya kaydı incelemeye gönderin.'
            : 'Kaydı inceleyip uygun süreç işlemini seçin.'}
        </p>
      </div>

      <div className="mt-4 flex flex-col gap-2 sm:mt-0 sm:shrink-0 sm:flex-row sm:flex-wrap sm:justify-end">
        {canEdit ? (
          <Link
            to={`/kayitlar/${record.id}/duzenle`}
            className="flex min-h-10 items-center justify-center gap-2 rounded-lg border border-app-border px-4 text-[15px] font-bold text-app-text-secondary transition hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <FilePenLine className="size-4" aria-hidden="true" />
            Düzenlemeye Devam Et
          </Link>
        ) : null}

        {actions.map((action) => {
          const { icon: Icon, tone } = actionPresentation[action.action] ?? defaultPresentation
          return (
            <button
              key={action.action}
              type="button"
              onClick={() => openAction(action)}
              className={`flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 text-[15px] font-bold transition focus-visible:outline-2 focus-visible:outline-offset-2 ${buttonToneClasses[tone]}`}
            >
              <Icon className="size-4" aria-hidden="true" />
              {action.displayName}
            </button>
          )
        })}
      </div>

      <ActionDialog
        action={selectedAction}
        comment={comment}
        targetDepartmentId={targetDepartmentId}
        departments={departments}
        departmentsLoading={targetDepartmentsQuery.isPending || targetDepartmentsQuery.isFetching}
        departmentsError={targetDepartmentsQuery.isError}
        onRetryDepartments={() => void targetDepartmentsQuery.refetch()}
        onCommentChange={setComment}
        onTargetDepartmentChange={setTargetDepartmentId}
        onClose={closeActionDialog}
        onConfirm={completeAction}
        busy={mutationBusy}
        disabled={targetUnavailable}
      />
    </section>
  )
}

function ActionDialog({
  action,
  comment,
  targetDepartmentId,
  departments,
  departmentsLoading,
  departmentsError,
  onRetryDepartments,
  onCommentChange,
  onTargetDepartmentChange,
  onClose,
  onConfirm,
  busy,
  disabled,
}: {
  action: AvailableWorkflowAction | null
  comment: string
  targetDepartmentId: number | null
  departments: { id: number; name: string }[]
  departmentsLoading: boolean
  departmentsError: boolean
  onRetryDepartments: () => void
  onCommentChange: (value: string) => void
  onTargetDepartmentChange: (id: number) => void
  onClose: () => void
  onConfirm: () => void | Promise<unknown>
  busy: boolean
  disabled: boolean
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const commentRef = useRef<HTMLTextAreaElement>(null)
  const willShowCommentField = Boolean(action) && (
    action!.commentRequired || (action!.action !== 'GONDER' && action!.action !== 'TEKRAR_GONDER')
  )
  useModalDialog({
    open: Boolean(action),
    onClose,
    dialogRef,
    initialFocusRef: willShowCommentField ? commentRef : closeButtonRef,
  })

  if (!action) return null
  const { tone } = actionPresentation[action.action] ?? defaultPresentation
  const commentRequired = action.commentRequired
  const showCommentField = willShowCommentField

  const confirmToneClass = tone === 'success'
    ? 'bg-emerald-600 hover:bg-emerald-700 focus-visible:outline-emerald-500'
    : tone === 'danger'
      ? 'bg-rose-600 hover:bg-rose-700 focus-visible:outline-rose-500'
      : 'bg-brand-700 hover:bg-brand-800 focus-visible:outline-brand-500'

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
      <section
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="record-action-title"
        className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id="record-action-title" className="text-lg font-bold text-app-text">{action.displayName}</h2>
            <p className="mt-2 text-sm leading-6 text-app-text-muted">
              {commentRequired ? 'Devam etmek için bir açıklama yazın.' : 'İsterseniz işlem notu ekleyebilirsiniz.'}
            </p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
            aria-label="İşlem penceresini kapat"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <div className="mt-5 space-y-4">
          {action.targetUserRequired ? (
            <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
              Bu işlem için kullanıcı seçimi şu anda desteklenmiyor.
            </p>
          ) : action.targetDepartmentRequired ? (
            <div>
              <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Hedef departman</span>
              {departmentsLoading ? (
                <p className="text-sm text-app-text-subtle">Departmanlar yükleniyor…</p>
              ) : departmentsError ? (
                <div className="space-y-2">
                  <p className="text-sm text-rose-700 dark:text-rose-300">Departmanlar yüklenemedi.</p>
                  <button
                    type="button"
                    onClick={onRetryDepartments}
                    className="min-h-9 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary hover:bg-app-surface-muted"
                  >
                    Tekrar dene
                  </button>
                </div>
              ) : departments.length === 0 ? (
                <p className="text-sm text-app-text-subtle">Gönderilebilecek departman yok.</p>
              ) : (
                <div className="max-h-44 space-y-2 overflow-y-auto">
                  {departments.map((department) => (
                    <button
                      key={department.id}
                      type="button"
                      role="radio"
                      aria-checked={targetDepartmentId === department.id}
                      onClick={() => onTargetDepartmentChange(department.id)}
                      className={`flex min-h-11 w-full items-center rounded-xl border px-4 text-sm font-semibold transition ${targetDepartmentId === department.id
                        ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300'
                        : 'border-app-border bg-app-surface text-app-text-secondary hover:bg-app-surface-muted'}`}
                    >
                      {department.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ) : null}

          {showCommentField ? (
            <>
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">
                  {commentRequired ? 'Açıklama *' : 'İşlem notu (isteğe bağlı)'}
                </span>
                <textarea
                  ref={commentRef}
                  value={comment}
                  onChange={(event) => onCommentChange(event.target.value)}
                  required={commentRequired}
                  rows={4}
                  maxLength={2000}
                  placeholder="Açıklamanızı yazın…"
                  className={`w-full resize-y rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none placeholder:text-app-text-faint ${commentRequired ? 'focus:border-rose-500' : 'focus:border-brand-500'}`}
                />
              </label>
              <p className="text-right text-[11px] font-medium text-app-text-subtle">{comment.length}/2000</p>
            </>
          ) : null}
        </div>

        <div className="mt-6 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={onClose}
            className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            İptal
          </button>
          <button
            type="button"
            disabled={busy || disabled || (commentRequired && !comment.trim())}
            onClick={onConfirm}
            className={`min-h-11 rounded-xl px-4 text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 ${confirmToneClass}`}
          >
            İşlemi Onayla
          </button>
        </div>
      </section>
    </div>
  )
}
