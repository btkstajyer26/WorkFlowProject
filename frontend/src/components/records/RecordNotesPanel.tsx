import { useState, type FormEvent } from 'react'
import { MessageSquareText, PencilLine, Save, X } from 'lucide-react'
import { useWorkflow } from '../../context/workflowState'
import { useToast } from '../../context/toastState'
import { canManageRecordNote, recordNoteMaxLength } from '../../domain/recordNotes'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import type { WorkflowRecord } from '../../types/record'

const noteDateFormatter = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function RecordNotesPanel({ record }: { record: WorkflowRecord }) {
  const { user, saveNote } = useWorkflow()
  const { showToast } = useToast()
  const [editorOpen, setEditorOpen] = useState(false)
  const [draft, setDraft] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { busy, run } = useSingleFlight()
  const ownNote = record.notes.find((note) => note.authorId === user.id)
  const canManage = canManageRecordNote(record, user)

  if (!canManage && !ownNote) return null

  const openEditor = () => {
    setDraft(ownNote?.body ?? '')
    setError(null)
    setEditorOpen(true)
  }

  const closeEditor = () => {
    setDraft(ownNote?.body ?? '')
    setError(null)
    setEditorOpen(false)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    void run(() => {
      try {
        saveNote(record.id, draft)
        showToast({
          title: ownNote ? 'Çalışma notunuz güncellendi' : 'Çalışma notunuz kaydedildi',
          description: 'İşlem açıklamasında kullanıma hazır.',
          tone: 'success',
        })
        setError(null)
        setEditorOpen(false)
      } catch (caughtError) {
        setError(caughtError instanceof Error ? caughtError.message : 'Not kaydedilemedi.')
      }
    })
  }

  return (
    <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6" aria-labelledby="record-notes-title">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <MessageSquareText className="size-5 shrink-0 text-violet-500" aria-hidden="true" />
          <h2 id="record-notes-title" className="font-bold text-app-text">Çalışma Notu</h2>
        </div>

        {canManage && !editorOpen ? (
          <button
            type="button"
            onClick={openEditor}
            className="inline-flex min-h-9 shrink-0 items-center gap-1.5 rounded-lg border border-app-border px-3 text-xs font-bold text-app-text-secondary transition hover:border-violet-400 hover:text-violet-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500 dark:hover:text-violet-300"
          >
            {ownNote ? <PencilLine className="size-3.5" aria-hidden="true" /> : <MessageSquareText className="size-3.5" aria-hidden="true" />}
            {ownNote ? 'Notumu Düzenle' : 'Not Ekle'}
          </button>
        ) : null}
      </div>

      {editorOpen ? (
        <form className="mt-4" onSubmit={handleSubmit}>
          <label htmlFor="record-note" className="sr-only">
            {ownNote ? 'Notunuzu düzenleyin' : 'İnceleme notunuzu yazın'}
          </label>
          <textarea
            id="record-note"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            rows={4}
            maxLength={recordNoteMaxLength}
            autoFocus
            placeholder="Kayıtla ilgili değerlendirmenizi yazın…"
            className="w-full resize-y rounded-xl border border-app-border bg-app-surface-muted px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-violet-500"
          />
          <div className="mt-1.5 flex items-center justify-between gap-3">
            <p className="text-xs text-rose-600 dark:text-rose-300" role={error ? 'alert' : undefined}>{error}</p>
            <span className="text-[11px] font-medium text-app-text-subtle">{draft.length}/{recordNoteMaxLength}</span>
          </div>
          <div className="mt-3 flex justify-end gap-2">
            <button
              type="button"
              onClick={closeEditor}
              className="inline-flex min-h-9 items-center justify-center gap-2 rounded-lg px-3 text-xs font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500"
            >
              <X className="size-4" aria-hidden="true" />
              Vazgeç
            </button>
            <button
              type="submit"
              disabled={!draft.trim() || busy}
              className="inline-flex min-h-9 items-center justify-center gap-2 rounded-lg bg-violet-600 px-3 text-xs font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500"
            >
              <Save className="size-4" aria-hidden="true" />
              {ownNote ? 'Değişiklikleri Kaydet' : 'Notu Kaydet'}
            </button>
          </div>
        </form>
      ) : ownNote ? (
        <article className="mt-4 border-l-2 border-violet-500 pl-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[10px] font-bold uppercase tracking-wider text-violet-700 dark:text-violet-300">Özel taslağınız</p>
              <p className="mt-1 truncate text-sm font-bold text-app-text-emphasis">{ownNote.author}</p>
            </div>
            <time className="shrink-0 text-right text-[10px] font-medium leading-4 text-app-text-faint" dateTime={ownNote.updatedAt}>
              {noteDateFormatter.format(new Date(ownNote.updatedAt))}
              {ownNote.updatedAt !== ownNote.createdAt ? <span className="block text-violet-600 dark:text-violet-300">Düzenlendi</span> : null}
            </time>
          </div>
          <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-app-text-secondary">{ownNote.body}</p>
        </article>
      ) : (
        <p className="mt-4 text-sm text-app-text-subtle">Henüz bir çalışma notunuz bulunmuyor.</p>
      )}
    </section>
  )
}
