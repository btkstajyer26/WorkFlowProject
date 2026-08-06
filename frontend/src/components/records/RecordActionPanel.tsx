import { useRef, useState } from 'react'
import {
  ArrowLeftRight,
  CheckCircle2,
  FilePenLine,
  MessageSquareText,
  Send,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-react'
import { Link } from 'react-router'
import { useWorkflow } from '../../context/workflowState'
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
  const { addNote, applyAction } = useWorkflow()
  const [note, setNote] = useState('')
  const [returnReason, setReturnReason] = useState('')
  const [returnTarget, setReturnTarget] = useState<'CALISAN' | 'BASKAN_YARDIMCISI'>('CALISAN')
  const [activeAction, setActiveAction] = useState<ReviewAction | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)
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
        ? { action: 'BASKANA_ILET' as const, targetUser: chair, comment: note }
        : activeAction === 'return'
          ? returnTarget === 'CALISAN'
            ? { action: 'CALISANA_GERI_GONDER' as const, comment: returnReason }
            : { action: 'BASKAN_YARDIMCISINA_GERI_GONDER' as const, comment: returnReason, targetUser: deputy }
          : activeAction === 'approve'
            ? { action: 'ONAYLA' as const, comment: note }
            : { action: 'REDDET' as const, comment: note }

    applyAction(record.id, workflowInput)
    setActiveAction(null)
    setReturnReason('')
    setNote('')
  })

  if (employeeCanEdit) {
    return (
      <section className="rounded-2xl border border-brand-200 bg-gradient-to-br from-brand-50 to-white p-5 shadow-sm sm:p-6">
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-brand-700 shadow-sm ring-1 ring-brand-100">
            <FilePenLine className="size-5" aria-hidden="true" />
          </span>
          <div>
            <h2 className="font-bold text-slate-950">Kayıt İşlemleri</h2>
            <p className="mt-1 text-xs leading-5 text-slate-600">
              {record.status === 'TASLAK'
                ? 'Taslağınıza devam edebilir veya incelemeye gönderebilirsiniz.'
                : 'İstenen düzeltmeleri tamamlayıp kaydı yeniden gönderebilirsiniz.'}
            </p>
          </div>
        </div>

        {feedback ? <FeedbackMessage message={feedback} /> : null}

        <div className="mt-5 grid gap-2 sm:grid-cols-2 xl:grid-cols-1">
          <Link
            to={`/kayitlar/${record.id}/duzenle`}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-4 text-sm font-bold text-brand-700 transition hover:border-brand-300 hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <FilePenLine className="size-4" aria-hidden="true" />
            Düzenlemeye Devam Et
          </Link>
          <button
            type="button"
            onClick={() => setActiveAction('submit')}
            className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white shadow-lg shadow-brand-200 transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            <Send className="size-4" aria-hidden="true" />
            {record.status === 'TASLAK' ? 'İncelemeye Gönder' : 'Yeniden Gönder'}
          </button>
        </div>

        <ActionDialog
          action={activeAction}
          note=""
          returnReason={returnReason}
          returnTarget={returnTarget}
          role={role}
          onReasonChange={setReturnReason}
          onTargetChange={setReturnTarget}
          onClose={() => setActiveAction(null)}
          onConfirm={completeAction}
          busy={mutationBusy}
        />
      </section>
    )
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-start gap-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
          <ShieldCheck className="size-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-bold text-slate-950">İnceleme İşlemleri</h2>
          <p className="mt-1 text-xs leading-5 text-slate-600">Not ekleyebilir veya süreç aksiyonu alabilirsiniz.</p>
        </div>
      </div>

      <label className="mt-5 block">
        <span className="mb-1.5 block text-xs font-bold text-slate-700">İnceleme notu</span>
        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          rows={4}
          placeholder="Değerlendirmenizi veya yönlendirmenizi yazın…"
          className="w-full resize-none rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
        />
      </label>

      <button
        type="button"
        disabled={!note.trim() || mutationBusy}
        onClick={() => runMutation(() => {
          addNote(record.id, note)
          setFeedback('İnceleme notu işlem geçmişine eklendi.')
          setNote('')
        })}
        className="mt-2 flex min-h-10 w-full items-center justify-center gap-2 rounded-xl border border-slate-200 px-3 text-xs font-bold text-slate-700 transition hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      >
        <MessageSquareText className="size-4" aria-hidden="true" />
        Notu Kaydet
      </button>

      {feedback ? <FeedbackMessage message={feedback} /> : null}

      <div className="mt-5 grid gap-2">
        <button
          type="button"
          onClick={() => setActiveAction('return')}
          className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 text-sm font-bold text-rose-700 transition hover:bg-rose-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
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
              className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 px-3 text-sm font-bold text-rose-700 transition hover:bg-rose-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
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
        note={note}
        returnReason={returnReason}
        returnTarget={returnTarget}
        role={role}
        onReasonChange={setReturnReason}
        onTargetChange={setReturnTarget}
        onClose={() => setActiveAction(null)}
        onConfirm={completeAction}
        busy={mutationBusy}
      />
    </section>
  )
}

function FeedbackMessage({ message }: { message: string }) {
  return (
    <p className="mt-4 rounded-xl border border-emerald-100 bg-emerald-50 px-3 py-2.5 text-xs font-semibold leading-5 text-emerald-800" role="status">
      {message}
    </p>
  )
}

function ActionDialog({
  action,
  note,
  returnReason,
  returnTarget,
  role,
  onReasonChange,
  onTargetChange,
  onClose,
  onConfirm,
  busy,
}: {
  action: ReviewAction | null
  note: string
  returnReason: string
  returnTarget: 'CALISAN' | 'BASKAN_YARDIMCISI'
  role: UserRole
  onReasonChange: (value: string) => void
  onTargetChange: (value: 'CALISAN' | 'BASKAN_YARDIMCISI') => void
  onClose: () => void
  onConfirm: () => void | Promise<unknown>
  busy: boolean
}) {
  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const returnReasonRef = useRef<HTMLTextAreaElement>(null)
  useModalDialog({
    open: Boolean(action),
    onClose,
    dialogRef,
    initialFocusRef: action === 'return' ? returnReasonRef : closeButtonRef,
  })

  if (!action) return null
  const copy = actionCopy[action]
  const isReturn = action === 'return'

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
      <section
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby="record-action-title"
        className="w-full rounded-t-3xl bg-white p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id="record-action-title" className="text-lg font-bold text-slate-950">{copy.title}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">{copy.description}</p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="flex size-10 shrink-0 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-brand-500"
            aria-label="İşlem penceresini kapat"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {isReturn ? (
          <div className="mt-5 space-y-4">
            {role === 'BASKAN' ? (
              <label className="block">
                <span className="mb-1.5 block text-xs font-bold text-slate-700">Geri gönderilecek kişi</span>
                <select
                  value={returnTarget}
                  onChange={(event) => onTargetChange(event.target.value as 'CALISAN' | 'BASKAN_YARDIMCISI')}
                  className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                >
                  <option value="CALISAN">Çalışan</option>
                  <option value="BASKAN_YARDIMCISI">Başkan Yardımcısı</option>
                </select>
              </label>
            ) : null}
            <label className="block">
              <span className="mb-1.5 block text-xs font-bold text-slate-700">Geri gönderme açıklaması *</span>
              <textarea
                ref={returnReasonRef}
                value={returnReason}
                onChange={(event) => onReasonChange(event.target.value)}
                required
                rows={4}
                placeholder="Eksik veya düzeltilmesi gereken alanları açıklayın…"
                className="w-full resize-none rounded-xl border border-slate-200 px-3.5 py-3 text-sm leading-6 text-slate-900 outline-none placeholder:text-slate-400 focus:border-rose-400 focus:ring-4 focus:ring-rose-100"
              />
            </label>
          </div>
        ) : note.trim() ? (
          <div className="mt-5 rounded-xl bg-slate-50 px-4 py-3">
            <p className="text-[11px] font-bold uppercase tracking-wide text-slate-500">İşleme eklenecek not</p>
            <p className="mt-1 text-sm leading-6 text-slate-700">{note}</p>
          </div>
        ) : null}

        <div className="mt-6 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={onClose}
            className="min-h-11 rounded-xl border border-slate-200 px-4 text-sm font-bold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            İptal
          </button>
          <button
            type="button"
            disabled={busy || (isReturn && !returnReason.trim()) || (action === 'reject' && !note.trim())}
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
