import { useRef, useState } from 'react'
import { CheckCircle2, Info, ListTodo, Wrench, X, XCircle } from 'lucide-react'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { usePerformSubtaskAction, useSubtasks } from '../../hooks/useSubtasks'
import { useToast } from '../../context/toastState'
import type { Subtask, SubtaskActionCode } from '../../types/subtask'
import type { WorkflowRecord } from '../../types/record'
import { isSubtaskResolved } from './subtaskStatus'
import { SubtaskStatusBadge } from './SubtaskStatusBadge'

type RowAction = {
  action: SubtaskActionCode
  label: string
  tone: 'primary' | 'success' | 'danger'
  commentRequired: boolean
}

const approvalPolicyLabel: Record<'UNANIMOUS' | 'MAJORITY', string> = {
  UNANIMOUS: 'Tümü onaylanmalı',
  MAJORITY: 'Çoğunluk yeterli',
}

function actionsFor(status: Subtask['status']): RowAction[] {
  if (status === 'DEGERLENDIRME') {
    return [{ action: 'DEGERLENDIRMEYI_TAMAMLA', label: 'Değerlendirmeyi Tamamla', tone: 'primary', commentRequired: false }]
  }
  if (status === 'ISLEM') {
    return [{ action: 'ISLEMI_TAMAMLA', label: 'İşlemi Tamamla', tone: 'primary', commentRequired: false }]
  }
  if (status === 'ONAY') {
    return [
      { action: 'ONAYLA', label: 'Onayla', tone: 'success', commentRequired: false },
      { action: 'REDDET', label: 'Reddet', tone: 'danger', commentRequired: true },
    ]
  }
  return []
}

/**
 * Parent/Subtask alt akışı (bkz. docs/PARENT_SUBTASK_GOREV_DAGILIMI.md).
 * Görsel şablon RecordFilesPanel'e yakın ama bilerek `<details>` ile
 * gizlenmiyor - "Parent'ın hangi alt görevleri beklediği görülebilmeli"
 * kabul kriterine göre her zaman açık.
 */
export function RecordSubtasksPanel({
  recordId,
  currentUserId,
  parentStatus,
}: {
  recordId: string
  currentUserId: string
  parentStatus: WorkflowRecord['status']
}) {
  const subtasksQuery = useSubtasks(recordId)
  const performAction = usePerformSubtaskAction(recordId)
  const { showToast } = useToast()
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const [activeRow, setActiveRow] = useState<{ subtask: Subtask; rowAction: RowAction } | null>(null)
  const [comment, setComment] = useState('')

  const subtasks = subtasksQuery.data?.subtasks ?? []
  if (subtasksQuery.isPending || subtasksQuery.isError || subtasks.length === 0) return null

  const requiredApprovals = subtasksQuery.data?.requiredApprovals ?? null
  const approvalPolicy = subtasksQuery.data?.approvalPolicy ?? null
  const resolvedCount = subtasks.filter((item) => isSubtaskResolved(item.status)).length
  // Backend, Parent'la yalniz alt-gorev-atamasi uzerinden iliskisi olan bir aktore
  // (ornegin bir Calisan'a) sadece kendi alt gorevini ve politika alanlarini null
  // dondurur (bkz. SubtaskQueryService.list). Diger alt gorevlerin sayisini,
  // atandigi kisileri veya durumlarini bu aktore hic gostermiyoruz.
  const isRestrictedToOwnSubtask = approvalPolicy === null

  const openRowAction = (subtask: Subtask, rowAction: RowAction) => {
    setComment('')
    setActiveRow({ subtask, rowAction })
  }
  const closeDialog = () => {
    setActiveRow(null)
    setComment('')
  }

  const confirmRowAction = () => runMutation(async () => {
    if (!activeRow) return
    const normalizedComment = comment.trim()
    if (activeRow.rowAction.commentRequired && !normalizedComment) {
      showToast({ title: 'Bu işlem için açıklama zorunludur', tone: 'error' })
      return
    }
    try {
      await performAction.mutateAsync({
        subtaskId: activeRow.subtask.id,
        request: {
          action: activeRow.rowAction.action,
          ...(normalizedComment ? { comment: normalizedComment } : {}),
        },
      })
      showToast({ title: 'Alt görev güncellendi', description: activeRow.subtask.title, tone: 'success' })
      closeDialog()
    } catch (error) {
      showToast({
        title: 'İşlem tamamlanamadı',
        description: error instanceof Error ? error.message : 'Alt görev işlemi sırasında bir hata oluştu.',
        tone: 'error',
      })
    }
  })

  return (
    <section aria-labelledby="record-subtasks-title" className="rounded-xl border border-app-border bg-app-surface px-4 py-4 sm:px-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="record-subtasks-title" className="flex items-center gap-2 text-base font-bold text-app-text">
            <ListTodo className="size-4.5 shrink-0" aria-hidden="true" />
            Alt Görevler
          </h2>
          {!isRestrictedToOwnSubtask ? (
            <p className="mt-1 text-sm leading-5 text-app-text-muted">
              {subtasks.length} alt görevden {resolvedCount} tanesi sonuçlandı.
              {approvalPolicy ? ` Politika: ${approvalPolicyLabel[approvalPolicy]}${requiredApprovals ? ` (en az ${requiredApprovals} onay)` : ''}.` : ''}
            </p>
          ) : null}
        </div>
      </div>

      <ul className="mt-4 space-y-2" aria-label="Alt görev listesi">
        {subtasks.map((subtask) => {
          const rowActions = subtask.assignedTo === currentUserId ? actionsFor(subtask.status) : []
          return (
            <li key={subtask.id} className="rounded-xl border border-app-border px-3 py-2.5 sm:px-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold text-app-text-emphasis">{subtask.title}</p>
                  <p className="mt-0.5 text-xs text-app-text-subtle">{subtask.assignedToName}</p>
                </div>
                <SubtaskStatusBadge status={subtask.status} />
              </div>
              {subtask.resolutionComment ? (
                <p className="mt-2 rounded-lg bg-app-surface-muted px-3 py-2 text-xs text-app-text-secondary">{subtask.resolutionComment}</p>
              ) : null}
              {rowActions.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-2">
                  {rowActions.map((rowAction) => (
                    <SubtaskRowButton key={rowAction.action} rowAction={rowAction} onClick={() => openRowAction(subtask, rowAction)} />
                  ))}
                </div>
              ) : null}
            </li>
          )
        })}
      </ul>

      {subtasks.length > 0 && resolvedCount === subtasks.length ? (
        <div className="mt-4 flex items-start gap-2.5 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900 dark:border-blue-900/60 dark:bg-blue-950/30 dark:text-blue-200" role="status">
          <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p>
            {isRestrictedToOwnSubtask
              ? 'İşleminiz Başkan Yardımcısının incelemesine sunuldu.'
              : (
                <>
                  Tüm alt görevler sonuçlandı ({subtasks.filter((item) => item.status === 'TAMAMLANDI').length} onaylandı,{' '}
                  {subtasks.filter((item) => item.status === 'REDDEDILDI').length} reddedildi).{' '}
                  {parentStatus === 'KONTROL'
                    ? 'Kayıt Kontrol aşamasında; yukarıdaki işlem panelinden Başkana iletebilirsin.'
                    : 'Kayıt bir sonraki adıma (Kontrol) ilerliyor.'}
                </>
              )}
          </p>
        </div>
      ) : null}

      <SubtaskRowDialog
        row={activeRow}
        comment={comment}
        onCommentChange={setComment}
        onClose={closeDialog}
        onConfirm={confirmRowAction}
        busy={mutationBusy}
      />
    </section>
  )
}

function SubtaskRowButton({ rowAction, onClick }: { rowAction: RowAction; onClick: () => void }) {
  const toneClasses: Record<RowAction['tone'], string> = {
    primary: 'bg-brand-700 text-white hover:bg-brand-800 focus-visible:outline-brand-500',
    success: 'bg-emerald-600 text-white hover:bg-emerald-700 focus-visible:outline-emerald-500',
    danger: 'border border-rose-300 text-rose-700 hover:bg-rose-50 focus-visible:outline-rose-500 dark:border-rose-800 dark:text-rose-300 dark:hover:bg-rose-950/40',
  }
  const Icon = rowAction.action === 'ONAYLA' ? CheckCircle2 : rowAction.action === 'REDDET' ? XCircle : Wrench
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex min-h-9 items-center justify-center gap-1.5 rounded-lg px-3 text-xs font-bold transition focus-visible:outline-2 focus-visible:outline-offset-2 ${toneClasses[rowAction.tone]}`}
    >
      <Icon className="size-3.5" aria-hidden="true" />
      {rowAction.label}
    </button>
  )
}

function SubtaskRowDialog({
  row,
  comment,
  onCommentChange,
  onClose,
  onConfirm,
  busy,
}: {
  row: { subtask: Subtask; rowAction: RowAction } | null
  comment: string
  onCommentChange: (value: string) => void
  onClose: () => void
  onConfirm: () => void | Promise<unknown>
  busy: boolean
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const commentRef = useRef<HTMLTextAreaElement>(null)
  useModalDialog({
    open: Boolean(row),
    onClose,
    dialogRef,
    initialFocusRef: row?.rowAction.commentRequired ? commentRef : closeButtonRef,
  })

  if (!row) return null
  const { subtask, rowAction } = row

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
      <section
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="subtask-action-title"
        className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id="subtask-action-title" className="text-lg font-bold text-app-text">{rowAction.label}</h2>
            <p className="mt-2 text-sm leading-6 text-app-text-muted">{subtask.title}</p>
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

        <div className="mt-5">
          <label className="block">
            <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">
              {rowAction.commentRequired ? 'Açıklama *' : 'Açıklama (isteğe bağlı)'}
            </span>
            <textarea
              ref={commentRef}
              value={comment}
              onChange={(event) => onCommentChange(event.target.value)}
              required={rowAction.commentRequired}
              rows={4}
              maxLength={2000}
              placeholder="Açıklamanızı yazın…"
              className={`w-full resize-y rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none placeholder:text-app-text-faint ${rowAction.commentRequired ? 'focus:border-rose-500' : 'focus:border-brand-500'}`}
            />
          </label>
          <p className="mt-1 text-right text-[11px] font-medium text-app-text-subtle">{comment.length}/2000</p>
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
            disabled={busy || (rowAction.commentRequired && !comment.trim())}
            onClick={onConfirm}
            className={`min-h-11 rounded-xl px-4 text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 ${
              rowAction.tone === 'success'
                ? 'bg-emerald-600 hover:bg-emerald-700 focus-visible:outline-emerald-500'
                : rowAction.tone === 'danger'
                  ? 'bg-rose-600 hover:bg-rose-700 focus-visible:outline-rose-500'
                  : 'bg-brand-700 hover:bg-brand-800 focus-visible:outline-brand-500'
            }`}
          >
            İşlemi Onayla
          </button>
        </div>
      </section>
    </div>
  )
}
