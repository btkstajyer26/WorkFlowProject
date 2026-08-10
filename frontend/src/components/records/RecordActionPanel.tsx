import { useRef, useState } from 'react'
import {
  ArrowLeftRight,
  CheckCircle2,
  FilePenLine,
  Send,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react'
import { Link } from 'react-router'
import { useWorkflow } from '../../context/workflowState'
import { useToast } from '../../context/toastState'
import { getDemoUserByRole } from '../../mocks/users'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
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

export function RecordActionPanel({ record, role }: { record: WorkflowRecord; role: UserRole }) {
  const { applyAction } = useWorkflow()
  const { showToast } = useToast()
  const [actionComment, setActionComment] = useState('')
  const [returnReason, setReturnReason] = useState('')
  const [returnTarget, setReturnTarget] = useState<'CALISAN' | 'BASKAN_YARDIMCISI'>('CALISAN')
  const [activeAction, setActiveAction] = useState<ReviewAction | null>(null)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()

  const employeeCanEdit = role === 'CALISAN' && (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR')
  const viceChairCanReview = role === 'BASKAN_YARDIMCISI' && record.status === 'BSK_YRD_INCELEMESINDE'
  const chairCanReview = role === 'BASKAN' && record.status === 'BASKAN_INCELEMESINDE'

  if (!employeeCanEdit && !viceChairCanReview && !chairCanReview) return null

  const completeAction = () => runMutation(() => {
    if (!activeAction) return

    const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')
    const chair = getDemoUserByRole('BASKAN')
    const workflowInput = activeAction === 'submit'
      ? { action: record.status === 'TASLAK' ? 'GONDER' as const : 'TEKRAR_GONDER' as const, targetUser: deputy }
      : activeAction === 'forward'
        ? { action: 'BASKANA_ILET' as const, targetUser: chair, comment: actionComment }
        : activeAction === 'return'
          ? returnTarget === 'CALISAN'
            ? { action: 'CALISANA_GERI_GONDER' as const, comment: returnReason }
            : { action: 'BASKAN_YARDIMCISINA_GERI_GONDER' as const, comment: returnReason, targetUser: deputy }
          : activeAction === 'approve'
            ? { action: 'ONAYLA' as const, comment: actionComment }
            : { action: 'REDDET' as const, comment: actionComment }

    try {
      applyAction(record.id, workflowInput)
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
      setReturnReason('')
      setActionComment('')
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
    setReturnReason('')
    setActionComment('')
  }

  if (employeeCanEdit) {
    return (
      <section className="rounded-2xl border border-brand-200 dark:border-brand-700/60 bg-gradient-to-br from-brand-50 dark:from-brand-900/30 to-app-surface p-5 shadow-sm sm:p-6">
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-app-surface text-brand-700 dark:text-brand-300 shadow-sm ring-1 ring-brand-100 dark:ring-brand-800/60">
            <FilePenLine className="size-5" aria-hidden="true" />
          </span>
          <div>
            <h2 className="font-bold text-app-text">Kayıt İşlemleri</h2>
            <p className="mt-1 text-xs leading-5 text-app-text-muted">
              {record.status === 'TASLAK'
                ? 'Taslağınıza devam edebilir veya incelemeye gönderebilirsiniz.'
                : 'İstenen düzeltmeleri tamamlayıp kaydı yeniden gönderebilirsiniz.'}
            </p>
          </div>
        </div>

        <div className="mt-5 grid gap-2 sm:grid-cols-2 xl:grid-cols-1">
          <Link
            to={`/kayitlar/${record.id}/duzenle`}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-brand-200 dark:border-brand-700/60 bg-app-surface px-4 text-sm font-bold text-brand-700 dark:text-brand-300 transition hover:border-brand-300 dark:hover:border-brand-600 hover:bg-brand-50 dark:hover:bg-brand-900/30 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <FilePenLine className="size-4" aria-hidden="true" />
            Düzenlemeye Devam Et
          </Link>
          <button
            type="button"
            onClick={() => setActiveAction('submit')}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Send className="size-4" aria-hidden="true" />
            {record.status === 'TASLAK' ? 'İncelemeye Gönder' : 'Yeniden Gönder'}
          </button>
        </div>

        <ActionDialog
          action={activeAction}
          comment={actionComment}
          returnReason={returnReason}
          returnTarget={returnTarget}
          role={role}
          onCommentChange={setActionComment}
          onReasonChange={setReturnReason}
          onTargetChange={setReturnTarget}
          onClose={closeActionDialog}
          onConfirm={completeAction}
          busy={mutationBusy}
        />
      </section>
    )
  }

  return (
    <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
      <div className="flex items-start gap-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300">
          <ShieldCheck className="size-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-bold text-app-text">İnceleme İşlemleri</h2>
          <p className="mt-1 text-xs leading-5 text-app-text-muted">Kaydı değerlendirin ve uygun süreç işlemini seçin.</p>
        </div>
      </div>

      <div className="mt-5 grid gap-2">
        <button
          type="button"
          onClick={() => setActiveAction('return')}
          className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 dark:border-rose-800/70 bg-rose-50 dark:bg-rose-950/40 px-4 text-sm font-bold text-rose-700 dark:text-rose-300 transition hover:bg-rose-100 dark:hover:bg-rose-900/60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
        >
          <ArrowLeftRight className="size-4" aria-hidden="true" />
          Geri Gönder
        </button>

        {viceChairCanReview ? (
          <button
            type="button"
            onClick={() => setActiveAction('forward')}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Send className="size-4" aria-hidden="true" />
            Başkana İlet
          </button>
        ) : null}

        {chairCanReview ? (
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setActiveAction('reject')}
              className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 dark:border-rose-800/70 px-3 text-sm font-bold text-rose-700 dark:text-rose-300 transition hover:bg-rose-50 dark:hover:bg-rose-950/40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
            >
              <XCircle className="size-4" aria-hidden="true" />
              Reddet
            </button>
            <button
              type="button"
              onClick={() => setActiveAction('approve')}
              className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-3 text-sm font-bold text-white transition hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-500"
            >
              <CheckCircle2 className="size-4" aria-hidden="true" />
              Onayla
            </button>
          </div>
        ) : null}
      </div>

      <ActionDialog
        action={activeAction}
        comment={actionComment}
        returnReason={returnReason}
        returnTarget={returnTarget}
        role={role}
        onCommentChange={setActionComment}
        onReasonChange={setReturnReason}
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
  returnReason,
  returnTarget,
  role,
  onCommentChange,
  onReasonChange,
  onTargetChange,
  onClose,
  onConfirm,
  busy,
}: {
  action: ReviewAction | null
  comment: string
  returnReason: string
  returnTarget: 'CALISAN' | 'BASKAN_YARDIMCISI'
  role: UserRole
  onCommentChange: (value: string) => void
  onReasonChange: (value: string) => void
  onTargetChange: (value: 'CALISAN' | 'BASKAN_YARDIMCISI') => void
  onClose: () => void
  onConfirm: () => void | Promise<unknown>
  busy: boolean
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const returnReasonRef = useRef<HTMLTextAreaElement>(null)
  const commentRef = useRef<HTMLTextAreaElement>(null)
  useModalDialog({
    open: Boolean(action),
    onClose,
    dialogRef,
    initialFocusRef: action === 'return'
      ? returnReasonRef
      : action && ['forward', 'approve', 'reject'].includes(action)
        ? commentRef
        : closeButtonRef,
  })

  if (!action) return null
  const copy = actionCopy[action]
  const isReturn = action === 'return'
  const hasCommentField = ['forward', 'approve', 'reject'].includes(action)

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

        {isReturn ? (
          <div className="mt-5 space-y-4">
            {role === 'BASKAN' ? (
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Geri gönderilecek kişi</span>
                <select
                  value={returnTarget}
                  onChange={(event) => onTargetChange(event.target.value as 'CALISAN' | 'BASKAN_YARDIMCISI')}
                  className="h-11 w-full rounded-xl border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60"
                >
                  <option value="CALISAN">Çalışan</option>
                  <option value="BASKAN_YARDIMCISI">Başkan Yardımcısı</option>
                </select>
              </label>
            ) : null}
            <label className="block">
              <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Geri gönderme açıklaması *</span>
              <textarea
                ref={returnReasonRef}
                value={returnReason}
                onChange={(event) => onReasonChange(event.target.value)}
                required
                rows={4}
                placeholder="Eksik veya düzeltilmesi gereken alanları açıklayın…"
                className="w-full resize-none rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none placeholder:text-app-text-faint focus:border-rose-400 focus:ring-4 focus:ring-rose-100 dark:focus:ring-rose-900/70"
              />
            </label>
          </div>
        ) : hasCommentField ? (
          <label className="mt-5 block">
            <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">
              {action === 'reject' ? 'Ret açıklaması *' : 'İşlem açıklaması (isteğe bağlı)'}
            </span>
            <textarea
              ref={commentRef}
              value={comment}
              onChange={(event) => onCommentChange(event.target.value)}
              required={action === 'reject'}
              rows={4}
              maxLength={1000}
              placeholder={action === 'reject' ? 'Kaydın neden reddedildiğini açıklayın…' : 'Bu işleme ilişkin kısa bir açıklama ekleyin…'}
              className={`w-full resize-none rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none placeholder:text-app-text-faint focus:ring-4 ${action === 'reject' ? 'focus:border-rose-400 focus:ring-rose-100 dark:focus:ring-rose-900/70' : 'focus:border-brand-400 focus:ring-brand-100 dark:focus:ring-brand-800/60'}`}
            />
          </label>
        ) : null}

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
            disabled={busy || (isReturn && !returnReason.trim()) || (action === 'reject' && !comment.trim())}
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
