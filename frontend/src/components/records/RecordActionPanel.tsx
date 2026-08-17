import { useRef, useState } from 'react'
import {
  ArrowLeftRight,
  CheckCircle2,
  FilePenLine,
  Send,
  X,
  XCircle,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import type { WorkflowActionRequest } from '../../api/generated/data-contracts'
import { useWorkflow } from '../../context/workflowState'
import { useToast } from '../../context/toastState'
import { getDemoUserByRole } from '../../mocks/users'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { useRecordWorkflowAction } from '../../hooks/useRecordWorkflowAction'
import type { UserRole } from '../../types/auth'
import type { WorkflowRecord } from '../../types/record'

type ReviewAction = 'submit' | 'forward' | 'return' | 'approve' | 'reject'

const actionCopy: Record<ReviewAction, { title: string; description: string; confirmLabel: string }> = {
  submit: {
    title: 'Başkan Yardımcısına gönder',
    description: 'Kayıt inceleme akışına alınacak ve gönderildikten sonra düzenlenemeyecek.',
    confirmLabel: 'İncelemeye Gönder',
  },
  forward: {
    title: 'Başkana ilet',
    description: 'Kayıt Başkanın nihai inceleme kuyruğuna gönderilecek.',
    confirmLabel: 'Başkana İlet',
  },
  return: {
    title: 'Kaydı geri gönder',
    description: 'Kaydın yeniden düzenlenebilmesi için geri gönderme açıklaması zorunludur.',
    confirmLabel: 'Geri Gönder',
  },
  approve: {
    title: 'Kaydı onayla',
    description: 'Onay sonrasında süreç tamamlanacak ve kayıt kilitlenecek.',
    confirmLabel: 'Onayla',
  },
  reject: {
    title: 'Kaydı reddet',
    description: 'Ret açıklaması zorunludur. İşlem sonrasında süreç sonlanacak ve kayıt kilitlenecek.',
    confirmLabel: 'Reddet',
  },
}

const commentCopy: Record<Exclude<ReviewAction, 'submit'>, { label: string; placeholder: string }> = {
  forward: {
    label: 'İletme açıklaması (isteğe bağlı)',
    placeholder: 'Başkana iletmek istediğiniz değerlendirmeyi yazın…',
  },
  return: {
    label: 'Geri gönderme açıklaması *',
    placeholder: 'Eksik veya düzeltilmesi gereken alanları açıklayın…',
  },
  approve: {
    label: 'Onay açıklaması (isteğe bağlı)',
    placeholder: 'Onaya ilişkin kısa bir açıklama ekleyin…',
  },
  reject: {
    label: 'Ret açıklaması *',
    placeholder: 'Kaydın neden reddedildiğini açıklayın…',
  },
}

export function RecordActionPanel({
  record,
  role,
  source = 'mock',
}: {
  record: WorkflowRecord
  role: UserRole
  source?: 'mock' | 'backend'
}) {
  const { applyAction } = useWorkflow()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const workflowMutation = useRecordWorkflowAction(record.id)
  const [comment, setComment] = useState('')
  const [returnTarget, setReturnTarget] = useState<'CALISAN' | 'BASKAN_YARDIMCISI'>('CALISAN')
  const [activeAction, setActiveAction] = useState<ReviewAction | null>(null)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()

  const employeeCanEdit = source === 'mock' && role === 'CALISAN' && (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR')
  const viceChairCanReview = role === 'BASKAN_YARDIMCISI' && record.status === 'BSK_YRD_INCELEMESINDE'
  const chairCanReview = role === 'BASKAN' && record.status === 'BASKAN_INCELEMESINDE'

  const openAction = (action: ReviewAction) => {
    setComment('')
    setActiveAction(action)
  }

  if (!employeeCanEdit && !viceChairCanReview && !chairCanReview) return null

  const completeAction = () => runMutation(async () => {
    if (!activeAction) return

    try {
      if (source === 'backend') {
        if (activeAction === 'submit') throw new Error('Bu gönderim akışı henüz backend sözleşmesinde netleşmedi.')
        const normalizedComment = comment.trim()
        const request: WorkflowActionRequest = {
          action: activeAction === 'forward'
            ? 'BASKANA_ILET'
            : activeAction === 'return'
              ? returnTarget === 'CALISAN'
                ? 'CALISANA_GERI_GONDER'
                : 'BASKAN_YARDIMCISINA_GERI_GONDER'
              : activeAction === 'approve'
                ? 'ONAYLA'
                : 'REDDET',
          ...(normalizedComment ? { comment: normalizedComment } : {}),
        }
        await workflowMutation.mutateAsync(request)
      } else {
        const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
        const chair = getDemoUserByRole('BASKAN')
        const workflowInput = activeAction === 'submit'
          ? {
              action: record.status === 'TASLAK' ? 'GONDER' as const : 'TEKRAR_GONDER' as const,
              targetUser: deputy,
              comment,
            }
          : activeAction === 'forward'
            ? { action: 'BASKANA_ILET' as const, targetUser: chair, comment }
            : activeAction === 'return'
              ? returnTarget === 'CALISAN'
                ? { action: 'CALISANA_GERI_GONDER' as const, comment }
                : { action: 'BASKAN_YARDIMCISINA_GERI_GONDER' as const, comment, targetUser: deputy }
              : activeAction === 'approve'
                ? { action: 'ONAYLA' as const, comment }
                : { action: 'REDDET' as const, comment }
        applyAction(record.id, workflowInput)
      }
      const successCopy: Record<ReviewAction, string> = {
        submit: record.status === 'TASLAK' ? 'Kayıt incelemeye gönderildi' : 'Kayıt yeniden incelemeye gönderildi',
        forward: 'Kayıt Başkana iletildi',
        return: returnTarget === 'CALISAN'
          ? 'Kayıt Çalışana geri gönderildi'
          : 'Kayıt Başkan Yardımcısına geri gönderildi',
        approve: 'Kayıt onaylandı',
        reject: 'Kayıt reddedildi',
      }
      showToast({ title: successCopy[activeAction], tone: 'success' })
      setActiveAction(null)
      setComment('')
      if (source === 'backend') navigate('/kayitlar')
    } catch (caughtError) {
      showToast({
        title: 'İşlem tamamlanamadı',
        description: caughtError instanceof Error ? caughtError.message : 'Kayıt işlemi sırasında bir hata oluştu.',
        tone: 'error',
      })
    }
  })

  const closeActionDialog = () => {
    setActiveAction(null)
    setComment('')
  }

  if (employeeCanEdit) {
    return (
      <section aria-labelledby="record-actions-title" className="rounded-xl border border-app-border bg-app-surface px-4 py-4 sm:flex sm:items-center sm:justify-between sm:gap-6 sm:px-5">
        <div className="min-w-0">
          <h2 id="record-actions-title" className="text-base font-bold text-app-text">Kayıt İşlemleri</h2>
          <p className="mt-1 text-sm leading-5 text-app-text-muted">
            {record.status === 'TASLAK'
              ? 'Taslağınıza devam edin veya kaydı incelemeye gönderin.'
              : 'İstenen düzenlemeleri tamamlayarak kaydı yeniden gönderin.'}
          </p>
        </div>

        <div className="mt-4 flex flex-col gap-2 sm:mt-0 sm:shrink-0 sm:flex-row sm:justify-end">
          <Link
            to={`/kayitlar/${record.id}/duzenle`}
            className="flex min-h-10 items-center justify-center gap-2 rounded-lg border border-app-border px-4 text-[15px] font-bold text-app-text-secondary transition hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <FilePenLine className="size-4" aria-hidden="true" />
            Düzenlemeye Devam Et
          </Link>
          <button
            type="button"
            onClick={() => openAction('submit')}
            className="flex min-h-10 items-center justify-center gap-2 rounded-lg bg-brand-700 px-4 text-[15px] font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Send className="size-4" aria-hidden="true" />
            {record.status === 'TASLAK' ? 'İncelemeye Gönder' : 'Yeniden Gönder'}
          </button>
        </div>

        <ActionDialog
          action={activeAction}
          comment={comment}
          returnTarget={returnTarget}
          role={role}
          onCommentChange={setComment}
          onTargetChange={setReturnTarget}
          onClose={closeActionDialog}
          onConfirm={completeAction}
          busy={mutationBusy}
        />
      </section>
    )
  }

  return (
    <section aria-labelledby="record-decision-title" className="rounded-xl border border-app-border bg-app-surface px-4 py-4 sm:flex sm:items-center sm:justify-between sm:gap-6 sm:px-5">
      <div className="min-w-0">
        <h2 id="record-decision-title" className="text-base font-bold text-app-text">Karar</h2>
        <p className="mt-1 text-sm leading-5 text-app-text-muted">Kaydı inceleyip uygun süreç işlemini seçin.</p>
      </div>

      <div className="mt-4 flex flex-col gap-2 sm:mt-0 sm:shrink-0 sm:flex-row sm:flex-wrap sm:justify-end">
        <button
          type="button"
          onClick={() => openAction('return')}
          className="flex min-h-10 items-center justify-center gap-2 rounded-lg border border-app-border px-4 text-[15px] font-bold text-app-text-secondary transition hover:bg-app-surface-muted hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
        >
          <ArrowLeftRight className="size-4" aria-hidden="true" />
          Geri Gönder
        </button>

        {viceChairCanReview ? (
          <button
            type="button"
            onClick={() => openAction('forward')}
            className="flex min-h-10 items-center justify-center gap-2 rounded-lg bg-brand-700 px-4 text-[15px] font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Send className="size-4" aria-hidden="true" />
            Başkana İlet
          </button>
        ) : null}

        {chairCanReview ? (
          <>
            <button
              type="button"
              onClick={() => openAction('reject')}
              className="flex min-h-10 items-center justify-center gap-2 rounded-lg border border-rose-300 px-4 text-[15px] font-bold text-rose-700 transition hover:bg-rose-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500 dark:border-rose-800 dark:text-rose-300 dark:hover:bg-rose-950/40"
            >
              <XCircle className="size-4" aria-hidden="true" />
              Reddet
            </button>
            <button
              type="button"
              onClick={() => openAction('approve')}
              className="flex min-h-10 items-center justify-center gap-2 rounded-lg bg-brand-700 px-4 text-[15px] font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
            >
              <CheckCircle2 className="size-4" aria-hidden="true" />
              Onayla
            </button>
          </>
        ) : null}
      </div>

      <ActionDialog
        action={activeAction}
        comment={comment}
        returnTarget={returnTarget}
        role={role}
        onCommentChange={setComment}
        onTargetChange={setReturnTarget}
        onClose={closeActionDialog}
        onConfirm={completeAction}
        busy={mutationBusy}
      />
    </section>
  )
}

function ActionDialog({
  action,
  comment,
  returnTarget,
  role,
  onCommentChange,
  onTargetChange,
  onClose,
  onConfirm,
  busy,
}: {
  action: ReviewAction | null
  comment: string
  returnTarget: 'CALISAN' | 'BASKAN_YARDIMCISI'
  role: UserRole
  onCommentChange: (value: string) => void
  onTargetChange: (value: 'CALISAN' | 'BASKAN_YARDIMCISI') => void
  onClose: () => void
  onConfirm: () => void | Promise<unknown>
  busy: boolean
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const commentRef = useRef<HTMLTextAreaElement>(null)
  useModalDialog({
    open: Boolean(action),
    onClose,
    dialogRef,
    initialFocusRef: action && action !== 'submit' ? commentRef : closeButtonRef,
  })

  if (!action) return null
  const copy = actionCopy[action]
  const isReturn = action === 'return'
  const commentRequired = isReturn || action === 'reject'
  const commentFieldCopy = action === 'submit' ? null : commentCopy[action]

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
            <h2 id="record-action-title" className="text-lg font-bold text-app-text">{copy.title}</h2>
            <p className="mt-2 text-sm leading-6 text-app-text-muted">{copy.description}</p>
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
          {isReturn && role === 'BASKAN' ? (
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Geri gönderilecek kişi</span>
                <select
                  value={returnTarget}
                  onChange={(event) => onTargetChange(event.target.value as 'CALISAN' | 'BASKAN_YARDIMCISI')}
                  className="h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-500"
                >
                  <option value="CALISAN">Çalışan</option>
                  <option value="BASKAN_YARDIMCISI">Başkan Yardımcısı</option>
                </select>
              </label>
          ) : null}
          {commentFieldCopy ? (
            <>
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">
                  {commentFieldCopy.label}
                </span>
                <textarea
                  ref={commentRef}
                  value={comment}
                  onChange={(event) => onCommentChange(event.target.value)}
                  required={commentRequired}
                  rows={4}
                  maxLength={2000}
                  placeholder={commentFieldCopy.placeholder}
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
            disabled={busy || (commentRequired && !comment.trim())}
            onClick={onConfirm}
            className={`min-h-11 rounded-xl px-4 text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 ${
              action === 'approve'
                ? 'bg-emerald-600 hover:bg-emerald-700 focus-visible:outline-emerald-500'
                : action === 'return' || action === 'reject'
                  ? 'bg-rose-600 hover:bg-rose-700 focus-visible:outline-rose-500'
                  : 'bg-brand-700 hover:bg-brand-800 focus-visible:outline-brand-500'
            }`}
          >
            {copy.confirmLabel}
          </button>
        </div>
      </section>
    </div>
  )
}
