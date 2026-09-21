import { useId, useRef, useState } from 'react'
import { Plus, Trash2, X } from 'lucide-react'
import { useModalDialog } from '../../hooks/useModalDialog'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { useAssignableUsers, useSplitIntoSubtasks } from '../../hooks/useSubtasks'
import { useToast } from '../../context/toastState'
import type { ApprovalPolicy } from '../../types/subtask'

type DraftSubtask = {
  key: string
  title: string
  description: string
  assigneeUserId: string
}

function emptyDraft(): DraftSubtask {
  return { key: crypto.randomUUID(), title: '', description: '', assigneeUserId: '' }
}

const policyCopy: Record<ApprovalPolicy, { label: string; description: string }> = {
  UNANIMOUS: {
    label: 'Tümü onaylanmalı',
    description: 'Parent, ancak bütün alt görevler tamamlandığında ilerler.',
  },
  MAJORITY: {
    label: 'Çoğunluk yeterli',
    description: 'Alt görevlerin çoğunluğu tamamlanırsa Parent ilerler; azınlıkta kalan red de KONTROL adımında görünür kalır.',
  },
}

/**
 * B10/WEB-1'in genel ActionDialog'u N kişi + politika seçimi toplayamadığı
 * için ALT_GOREVLERE_AYIR aksiyonu bu özel diyaloğu açar - bkz.
 * docs/PARENT_SUBTASK_GOREV_DAGILIMI.md §4 madde 8.
 */
export function SplitIntoSubtasksDialog({
  recordId,
  open,
  onClose,
}: {
  recordId: string
  open: boolean
  onClose: () => void
}) {
  const { showToast } = useToast()
  const splitMutation = useSplitIntoSubtasks(recordId)
  const assignableUsersQuery = useAssignableUsers(recordId, open)
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  const [policy, setPolicy] = useState<ApprovalPolicy>('UNANIMOUS')
  const [drafts, setDrafts] = useState<DraftSubtask[]>([emptyDraft(), emptyDraft()])
  const titleId = useId()

  const dialogRef = useRef<HTMLElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  useModalDialog({ open, onClose, dialogRef, initialFocusRef: closeButtonRef })

  if (!open) return null

  const assignableUsers = assignableUsersQuery.data ?? []
  const usersUnavailable = assignableUsersQuery.isPending || assignableUsersQuery.isFetching ||
    assignableUsersQuery.isError || assignableUsers.length === 0

  const updateDraft = (key: string, patch: Partial<DraftSubtask>) => {
    setDrafts((current) => current.map((draft) => draft.key === key ? { ...draft, ...patch } : draft))
  }
  const addDraft = () => setDrafts((current) => [...current, emptyDraft()])
  const removeDraft = (key: string) => setDrafts((current) => current.length > 2 ? current.filter((draft) => draft.key !== key) : current)

  const resetAndClose = () => {
    setDrafts([emptyDraft(), emptyDraft()])
    setPolicy('UNANIMOUS')
    onClose()
  }

  const confirmSplit = () => runMutation(async () => {
    const trimmed = drafts.map((draft) => ({ ...draft, title: draft.title.trim(), description: draft.description.trim() }))
    if (trimmed.some((draft) => !draft.title)) {
      showToast({ title: 'Her alt görev için bir başlık girin', tone: 'error' })
      return
    }
    if (trimmed.some((draft) => !draft.assigneeUserId)) {
      showToast({ title: 'Her alt görev için bir kişi seçin', tone: 'error' })
      return
    }
    const assigneeIds = trimmed.map((draft) => draft.assigneeUserId)
    if (new Set(assigneeIds).size !== assigneeIds.length) {
      showToast({ title: 'Aynı kişi birden fazla alt göreve atanamaz', tone: 'error' })
      return
    }

    try {
      await splitMutation.mutateAsync({
        approvalPolicy: policy,
        subtasks: trimmed.map((draft) => ({
          title: draft.title,
          description: draft.description,
          assigneeUserId: draft.assigneeUserId,
        })),
      })
      showToast({ title: 'Kayıt alt görevlere bölündü', description: `${trimmed.length} alt görev oluşturuldu.`, tone: 'success' })
      resetAndClose()
    } catch (error) {
      showToast({
        title: 'Bölme işlemi tamamlanamadı',
        description: error instanceof Error ? error.message : 'Alt görevler oluşturulurken bir hata oluştu.',
        tone: 'error',
      })
    }
  })

  return (
    <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
      <section
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="max-h-[90vh] w-full overflow-y-auto rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-lg sm:rounded-2xl sm:p-6"
      >
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <h2 id={titleId} className="text-lg font-bold text-app-text">Alt Görevlere Ayır</h2>
            <p className="mt-2 text-sm leading-6 text-app-text-muted">
              Kaydı bağımsız ilerleyecek alt görevlere bölün. Parent, tüm alt görevler sonuçlanana kadar bekleme durumuna alınır.
            </p>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={resetAndClose}
            className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
            aria-label="Bölme penceresini kapat"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {usersUnavailable ? (
          <p className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700 dark:border-rose-800/70 dark:bg-rose-950/40 dark:text-rose-300" role="alert">
            {assignableUsersQuery.isPending || assignableUsersQuery.isFetching
              ? 'Kullanıcılar yükleniyor…'
              : 'Atanabilecek kullanıcı listesi alınamadı.'}
          </p>
        ) : (
          <>
            <div className="mt-5 space-y-3">
              {drafts.map((draft, index) => (
                <div key={draft.key} className="rounded-xl border border-app-border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-bold text-app-text-secondary">Alt Görev {index + 1}</span>
                    {drafts.length > 2 ? (
                      <button
                        type="button"
                        onClick={() => removeDraft(draft.key)}
                        className="flex size-7 items-center justify-center rounded-lg text-app-text-subtle transition hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-950/40"
                        aria-label={`Alt Görev ${index + 1} satırını kaldır`}
                      >
                        <Trash2 className="size-3.5" aria-hidden="true" />
                      </button>
                    ) : null}
                  </div>
                  <div className="mt-2 space-y-2">
                    <input
                      type="text"
                      value={draft.title}
                      onChange={(event) => updateDraft(draft.key, { title: event.target.value })}
                      placeholder="Başlık"
                      maxLength={255}
                      className="h-10 w-full rounded-lg border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-500"
                    />
                    <textarea
                      value={draft.description}
                      onChange={(event) => updateDraft(draft.key, { description: event.target.value })}
                      placeholder="Açıklama (isteğe bağlı)"
                      rows={2}
                      maxLength={1000}
                      className="w-full resize-y rounded-lg border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text-strong outline-none focus:border-brand-500"
                    />
                    <select
                      value={draft.assigneeUserId}
                      onChange={(event) => updateDraft(draft.key, { assigneeUserId: event.target.value })}
                      className="h-10 w-full rounded-lg border border-app-border bg-app-surface px-3 text-sm text-app-text-strong outline-none focus:border-brand-500"
                    >
                      <option value="">Kişi seçin…</option>
                      {assignableUsers.map((user) => (
                        <option key={user.id} value={user.id}>{user.fullName}</option>
                      ))}
                    </select>
                  </div>
                </div>
              ))}
            </div>

            <button
              type="button"
              onClick={addDraft}
              className="mt-3 flex min-h-9 items-center gap-1.5 rounded-lg border border-dashed border-app-border px-3 text-xs font-bold text-app-text-secondary transition hover:border-brand-300 hover:text-brand-700 dark:hover:border-brand-600 dark:hover:text-brand-300"
            >
              <Plus className="size-3.5" aria-hidden="true" />
              Alt görev ekle
            </button>

            <fieldset className="mt-5">
              <legend className="mb-1.5 text-xs font-bold text-app-text-secondary">Onay politikası</legend>
              <div className="space-y-2">
                {(['UNANIMOUS', 'MAJORITY'] as const).map((option) => (
                  <label
                    key={option}
                    className={`flex cursor-pointer items-start gap-2.5 rounded-xl border px-3.5 py-2.5 text-sm transition ${
                      policy === option
                        ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/30'
                        : 'border-app-border hover:bg-app-surface-muted'
                    }`}
                  >
                    <input
                      type="radio"
                      name="approval-policy"
                      value={option}
                      checked={policy === option}
                      onChange={() => setPolicy(option)}
                      className="mt-0.5"
                    />
                    <span>
                      <span className="block font-bold text-app-text-strong">{policyCopy[option].label}</span>
                      <span className="block text-xs text-app-text-subtle">{policyCopy[option].description}</span>
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>
          </>
        )}

        <div className="mt-6 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={resetAndClose}
            className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          >
            İptal
          </button>
          <button
            type="button"
            disabled={mutationBusy || usersUnavailable}
            onClick={confirmSplit}
            className="min-h-11 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Alt Görevlere Ayır
          </button>
        </div>
      </section>
    </div>
  )
}
