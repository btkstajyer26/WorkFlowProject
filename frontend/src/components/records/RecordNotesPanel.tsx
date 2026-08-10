import { useState, type FormEvent } from 'react'
import { MessageSquareText, PencilLine, Save, X } from 'lucide-react'
import { recordNoteMaxLength } from '../../domain/recordNotes'
import { useWorkflow } from '../../context/workflowState'
import { useToast } from '../../context/toastState'
import { useSingleFlight } from '../../hooks/useSingleFlight'
import { roleLabels } from '../../types/auth'
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
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { busy, run } = useSingleFlight()
  const ownNote = record.notes.find((note) => note.authorId === user.id)
  const canEdit = !['ONAYLANDI', 'REDDEDILDI'].includes(record.status)
  const sortedNotes = [...record.notes].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))

  const openEditor = () => {
    setDraft(ownNote?.body ?? '')
    setError(null)
    setIsEditing(true)
  }

  const closeEditor = () => {
    setDraft(ownNote?.body ?? '')
    setError(null)
    setIsEditing(false)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    void run(() => {
      try {
        saveNote(record.id, draft)
        showToast({
          title: ownNote ? 'Notunuz güncellendi' : 'Notunuz kaydedildi',
          description: 'Değişiklik kayıt detayında görüntüleniyor.',
          tone: 'success',
        })
        setError(null)
        setIsEditing(false)
      } catch (caughtError) {
        setError(caughtError instanceof Error ? caughtError.message : 'Not kaydedilemedi.')
      }
    })
  }

  return (
    <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6" aria-labelledby="record-notes-title">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-violet-50 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300">
            <MessageSquareText className="size-5" aria-hidden="true" />
          </span>
          <div>
            <h2 id="record-notes-title" className="font-bold text-app-text">Notlar</h2>
          </div>
        </div>

        {canEdit && !isEditing ? (
          <button
            type="button"
            onClick={openEditor}
            className="inline-flex min-h-9 shrink-0 items-center gap-1.5 rounded-xl border border-violet-200 bg-violet-50 px-3 text-xs font-bold text-violet-700 transition hover:bg-violet-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500 dark:border-violet-800/70 dark:bg-violet-950/40 dark:text-violet-300 dark:hover:bg-violet-900/60"
          >
            {ownNote ? <PencilLine className="size-3.5" aria-hidden="true" /> : <MessageSquareText className="size-3.5" aria-hidden="true" />}
            {ownNote ? 'Notumu Düzenle' : 'Not Ekle'}
          </button>
        ) : null}
      </div>

      {isEditing ? (
        <form className="mt-5 rounded-2xl border border-violet-200 bg-violet-50/60 p-4 dark:border-violet-800/70 dark:bg-violet-950/20" onSubmit={handleSubmit}>
          <label htmlFor="record-note" className="text-xs font-bold text-app-text-secondary">
            {ownNote ? 'Notunuzu güncelleyin' : 'Notunuzu yazın'}
          </label>
          <textarea
            id="record-note"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            rows={5}
            maxLength={recordNoteMaxLength}
            autoFocus
            placeholder="Bu kayıtla ilgili değerlendirmenizi yazın…"
            className="mt-2 w-full resize-y rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm leading-6 text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-violet-400 focus:ring-4 focus:ring-violet-100 dark:focus:ring-violet-900/60"
          />
          <div className="mt-1.5 flex items-center justify-between gap-3">
            <p className="text-xs text-rose-600 dark:text-rose-300" role={error ? 'alert' : undefined}>{error}</p>
            <span className="text-[11px] font-medium text-app-text-subtle">{draft.length}/{recordNoteMaxLength}</span>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={closeEditor}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-app-border bg-app-surface px-3 text-xs font-bold text-app-text-secondary transition hover:bg-app-surface-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500"
            >
              <X className="size-4" aria-hidden="true" />
              Vazgeç
            </button>
            <button
              type="submit"
              disabled={!draft.trim() || busy}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-violet-600 px-3 text-xs font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500"
            >
              <Save className="size-4" aria-hidden="true" />
              {ownNote ? 'Değişiklikleri Kaydet' : 'Notu Kaydet'}
            </button>
          </div>
        </form>
      ) : null}

      {sortedNotes.length > 0 ? (
        <ul className="mt-5 space-y-3">
          {sortedNotes.map((note) => {
            const isOwn = note.authorId === user.id
            const wasEdited = note.updatedAt !== note.createdAt
            return (
              <li key={note.id} className={`rounded-2xl border p-4 ${isOwn ? 'border-violet-200 bg-violet-50/50 dark:border-violet-800/70 dark:bg-violet-950/20' : 'border-app-border bg-app-surface-muted/70'}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate text-sm font-bold text-app-text-emphasis">{note.author}</p>
                      {isOwn ? <span className="rounded-full bg-violet-100 px-2 py-0.5 text-[10px] font-bold text-violet-700 dark:bg-violet-900/60 dark:text-violet-200">Sizin notunuz</span> : null}
                    </div>
                    <p className="mt-0.5 text-[11px] font-medium text-app-text-subtle">{roleLabels[note.authorRole]}</p>
                  </div>
                  <time className="shrink-0 text-right text-[10px] font-medium leading-4 text-app-text-faint" dateTime={note.updatedAt}>
                    {noteDateFormatter.format(new Date(note.updatedAt))}
                    {wasEdited ? <span className="block text-violet-600 dark:text-violet-300">Düzenlendi</span> : null}
                  </time>
                </div>
                <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-app-text-secondary">{note.body}</p>
              </li>
            )
          })}
        </ul>
      ) : !isEditing ? (
        <p className="mt-5 rounded-xl border border-dashed border-app-border bg-app-surface-muted px-4 py-6 text-center text-sm text-app-text-subtle">
          Bu kayıt için henüz not eklenmemiş.
        </p>
      ) : null}

      {!canEdit ? (
        <p className="mt-4 rounded-xl bg-app-surface-muted px-3 py-2.5 text-xs leading-5 text-app-text-subtle">
          Sonuçlanan kayıtlarda notlar görüntülenebilir ancak değiştirilemez.
        </p>
      ) : null}
    </section>
  )
}
