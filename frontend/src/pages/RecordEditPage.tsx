import { useEffect, useRef, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import {
  AlertCircle,
  ArrowLeft,
  FileText,
  Paperclip,
  Save,
  Send,
  Trash2,
  X,
} from 'lucide-react'
import { Navigate, useNavigate, useParams } from 'react-router'
import { RecordStatusBadge } from '../components/records/RecordStatusBadge'
import {
  attachmentAcceptValue,
  getAttachmentValidationError,
  maxRecordTitleLength,
  recordCategories,
} from '../config/records'
import { useWorkflow } from '../context/workflowState'
import { recordFormSchema, type RecordFormValues } from '../schemas/record'
import { useModalDialog } from '../hooks/useModalDialog'
import { useSingleFlight } from '../hooks/useSingleFlight'
import type { UserRole } from '../types/auth'
import type { WorkflowRecord } from '../types/record'

const fieldClass =
  'w-full rounded-xl border border-app-border bg-app-surface px-3.5 py-3 text-sm text-app-text-strong outline-none transition placeholder:text-app-text-faint focus:border-brand-400 focus:ring-4 focus:ring-brand-100 dark:focus:ring-brand-800/60'

export function RecordEditPage({ role }: { role: UserRole }) {
  const { recordId } = useParams()
  const { records, visibleRecords } = useWorkflow()
  const record = visibleRecords.find((item) => item.id === recordId)

  const editable = record && role === 'CALISAN' && (record.status === 'TASLAK' || record.status === 'DUZENLEME_BEKLIYOR')
  if (!record) return <Navigate to={records.some((item) => item.id === recordId) ? '/403' : '/404'} replace />
  if (!editable) return <Navigate to="/403" replace />

  return <EditableRecordForm key={record.id} record={record} />
}

function EditableRecordForm({ record }: { record: WorkflowRecord }) {
  const navigate = useNavigate()
  const { updateEditableRecord, updateAndSubmit, deleteDraft } = useWorkflow()
  const [attachments, setAttachments] = useState(record?.attachments ?? [])
  const [attachmentsDirty, setAttachmentsDirty] = useState(false)
  const [attachmentError, setAttachmentError] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)
  const [activeDialog, setActiveDialog] = useState<'submit' | 'delete' | 'discard' | null>(null)
  const dialogRef = useRef<HTMLElement>(null)
  const dialogCloseButtonRef = useRef<HTMLButtonElement>(null)
  const {
    register,
    handleSubmit,
    getValues,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<RecordFormValues>({
    resolver: zodResolver(recordFormSchema),
    defaultValues: {
      title: record.title,
      category: record.category,
      description: record.description,
    },
  })
  const titleValue = watch('title')
  const hasUnsavedChanges = isDirty || attachmentsDirty
  const { busy: mutationBusy, run: runMutation } = useSingleFlight()
  useModalDialog({
    open: Boolean(activeDialog),
    onClose: () => setActiveDialog(null),
    dialogRef,
    initialFocusRef: dialogCloseButtonRef,
  })

  useEffect(() => {
    if (!hasUnsavedChanges) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const latestRevisionNote = [...record.history].reverse().find((item) => item.note)?.note

  const toDraftInput = (values: RecordFormValues) => ({
    ...values,
    attachments,
  })

  const saveDraft = handleSubmit((values) => runMutation(() => {
    updateEditableRecord(record.id, toDraftInput(values))
    setFeedback('Değişiklikler kaydedildi.')
    setAttachmentsDirty(false)
    reset(values)
  }))

  return (
    <div className="space-y-5">
      <button
        type="button"
        onClick={() => {
          if (hasUnsavedChanges) setActiveDialog('discard')
          else navigate(`/kayitlar/${record.id}`)
        }}
        className="inline-flex items-center gap-2 text-sm font-bold text-app-text-muted transition hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
      >
        <ArrowLeft className="size-4" aria-hidden="true" />
        Kayıt detayına dön
      </button>

      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold text-brand-600 dark:text-brand-400">{record.recordNumber}</p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-app-text sm:text-3xl">
            {record.status === 'TASLAK' ? 'Taslağı Düzenle' : 'Düzeltmeleri Tamamla'}
          </h1>
          <p className="mt-2 text-sm text-app-text-muted">Kaydı kaydedebilir veya Başkan Yardımcısı incelemesine gönderebilirsiniz.</p>
        </div>
        <RecordStatusBadge status={record.status} />
      </header>

      <form
        className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]"
        noValidate
        onSubmit={handleSubmit(() => setActiveDialog('submit'))}
      >
        <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm sm:p-6">
          <div className="space-y-5">
            <label className="block" htmlFor="edit-record-title">
              <span className="mb-1.5 flex items-center justify-between gap-3 text-xs font-bold text-app-text-secondary">
                <span>Başlık *</span>
                <span className="text-[11px] font-semibold text-app-text-faint">
                  {titleValue.length} / {maxRecordTitleLength}
                </span>
              </span>
              <input
                id="edit-record-title"
                {...register('title')}
                maxLength={maxRecordTitleLength}
                aria-invalid={Boolean(errors.title)}
                aria-describedby={errors.title ? 'edit-record-title-error' : undefined}
                className={fieldClass}
              />
              {errors.title ? <FieldError id="edit-record-title-error" message={errors.title.message} /> : null}
            </label>

            <label className="block" htmlFor="edit-record-category">
              <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Kategori *</span>
              <select
                id="edit-record-category"
                {...register('category')}
                aria-invalid={Boolean(errors.category)}
                aria-describedby={errors.category ? 'edit-record-category-error' : undefined}
                className={fieldClass}
              >
                {recordCategories.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
              {errors.category ? <FieldError id="edit-record-category-error" message={errors.category.message} /> : null}
            </label>

            <label className="block" htmlFor="edit-record-description">
              <span className="mb-1.5 block text-xs font-bold text-app-text-secondary">Kayıt açıklaması *</span>
              <textarea
                id="edit-record-description"
                {...register('description')}
                rows={12}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={errors.description ? 'edit-record-description-error' : undefined}
                className={`${fieldClass} min-h-64 resize-y leading-6`}
              />
              {errors.description ? <FieldError id="edit-record-description-error" message={errors.description.message} /> : null}
            </label>

            <div>
              <div className="mb-2 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold text-app-text-secondary">Ek dosyalar</p>
                  <p className="mt-0.5 text-xs text-app-text-subtle">PDF, Word, Excel, JPG veya PNG</p>
                </div>
                <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-app-border px-3 text-xs font-bold text-app-text-secondary transition hover:border-brand-300 dark:hover:border-brand-600 hover:text-brand-700 dark:hover:text-brand-300 focus-within:outline-2 focus-within:outline-brand-500">
                  <Paperclip className="size-4" aria-hidden="true" />
                  Dosya ekle
                  <input
                    type="file"
                    multiple
                    accept={attachmentAcceptValue}
                    aria-invalid={Boolean(attachmentError)}
                    aria-describedby={attachmentError ? 'edit-record-attachment-error' : undefined}
                    className="sr-only"
                    onChange={(event) => {
                      const selectedFiles = Array.from(event.target.files ?? [])
                      const validationError = getAttachmentValidationError(selectedFiles)
                      setAttachmentError(validationError)
                      if (validationError) {
                        event.target.value = ''
                        return
                      }

                      const newFiles = selectedFiles.map((file, index) => ({
                        id: `local-${Date.now()}-${index}`,
                        name: file.name,
                        size: `${Math.max(1, Math.round(file.size / 1024))} KB`,
                      }))
                      const seen = new Set(attachments.map((item) => item.name.toLocaleLowerCase('tr-TR')))
                      const uniqueFiles = newFiles.filter((item) => {
                        const key = item.name.toLocaleLowerCase('tr-TR')
                        if (seen.has(key)) return false
                        seen.add(key)
                        return true
                      })
                      if (uniqueFiles.length !== newFiles.length) setAttachmentError('Aynı dosya birden fazla kez eklenemez.')
                      if (uniqueFiles.length > 0) {
                        setAttachmentsDirty(true)
                        setAttachments((current) => [...current, ...uniqueFiles])
                      }
                      event.target.value = ''
                    }}
                  />
                </label>
              </div>

              {attachmentError ? <FieldError id="edit-record-attachment-error" message={attachmentError} /> : null}

              {attachments.length > 0 ? (
                <ul className="space-y-2" aria-label="Kayıt ekleri">
                  {attachments.map((attachment) => (
                    <li key={attachment.id} className="flex items-center gap-3 rounded-xl bg-app-surface-muted px-3 py-2.5">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-app-surface text-brand-600 dark:text-brand-400 ring-1 ring-app-border">
                        <FileText className="size-4" aria-hidden="true" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-xs font-bold text-app-text-emphasis">{attachment.name}</p>
                        <p className="mt-0.5 text-[11px] text-app-text-subtle">{attachment.size}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          setAttachmentsDirty(true)
                          setAttachments((current) => current.filter((item) => item.id !== attachment.id))
                        }}
                        className="flex size-9 items-center justify-center rounded-lg text-app-text-faint transition hover:bg-app-surface hover:text-rose-600 dark:hover:text-rose-400 focus-visible:outline-2 focus-visible:outline-brand-500"
                        aria-label={`${attachment.name} dosyasını kaldır`}
                      >
                        <Trash2 className="size-4" aria-hidden="true" />
                      </button>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="rounded-xl border border-dashed border-app-border bg-app-surface-muted px-4 py-5 text-center text-xs text-app-text-subtle">Henüz dosya eklenmedi.</p>
              )}
            </div>
          </div>
        </section>

        <aside className="space-y-4 xl:sticky xl:top-6 xl:self-start">
          {record.status === 'DUZENLEME_BEKLIYOR' && latestRevisionNote ? (
            <section className="rounded-2xl border border-amber-200 dark:border-amber-800/70 bg-amber-50 dark:bg-amber-950/40 p-5">
              <div className="flex items-start gap-3">
                <AlertCircle className="mt-0.5 size-5 shrink-0 text-amber-700 dark:text-amber-300" aria-hidden="true" />
                <div>
                  <h2 className="text-sm font-bold text-amber-950 dark:text-amber-100">Düzeltme Talebi</h2>
                  <p className="mt-2 text-sm leading-6 text-amber-900 dark:text-amber-200">{latestRevisionNote}</p>
                </div>
              </div>
            </section>
          ) : null}

          <section className="rounded-2xl border border-app-border bg-app-surface p-5 shadow-sm">
            <h2 className="font-bold text-app-text">Kaydet ve Gönder</h2>
            <p className="mt-2 text-xs leading-5 text-app-text-muted">Göndermeden önce değişikliklerinizi taslak olarak kaydedebilirsiniz.</p>

            {feedback ? (
              <p className="mt-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 px-3 py-2.5 text-xs font-semibold leading-5 text-emerald-800 dark:text-emerald-200" role="status">{feedback}</p>
            ) : null}

            <div className="mt-5 grid gap-2">
              <button
                type="button"
                onClick={saveDraft}
                disabled={mutationBusy}
                className="flex min-h-11 items-center justify-center gap-2 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:border-brand-200 dark:hover:border-brand-700/60 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                <Save className="size-4" aria-hidden="true" />
                Taslağı Kaydet
              </button>
              <button
                type="submit"
                disabled={mutationBusy}
                className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-700 px-4 text-sm font-bold text-white shadow-lg shadow-brand-200 dark:shadow-black/20 transition hover:bg-brand-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              >
                <Send className="size-4" aria-hidden="true" />
                {record.status === 'TASLAK' ? 'İncelemeye Gönder' : 'Yeniden Gönder'}
              </button>
              {record.status === 'TASLAK' ? (
                <button
                  type="button"
                  onClick={() => setActiveDialog('delete')}
                  className="mt-2 flex min-h-11 items-center justify-center gap-2 border-t border-app-border-subtle pt-3 text-sm font-bold text-rose-600 dark:text-rose-400 transition hover:text-rose-700 dark:hover:text-rose-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500"
                >
                  <Trash2 className="size-4" aria-hidden="true" />
                  Taslağı Sil
                </button>
              ) : null}
            </div>
          </section>
        </aside>
      </form>

      {activeDialog ? (
        <div className="fixed inset-0 z-[80] flex items-end justify-center bg-slate-950/35 backdrop-blur-[2px] sm:items-center sm:p-4" role="presentation">
          <section ref={dialogRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="draft-dialog-title" className="w-full rounded-t-3xl bg-app-surface p-5 shadow-2xl sm:max-w-md sm:rounded-2xl sm:p-6">
            <div className="flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <h2 id="draft-dialog-title" className="text-lg font-bold text-app-text">
                  {activeDialog === 'delete'
                    ? 'Taslağı silmek istediğinize emin misiniz?'
                    : activeDialog === 'discard'
                      ? 'Kaydedilmemiş değişikliklerden vazgeçilsin mi?'
                      : 'Başkan Yardımcısına gönder'}
                </h2>
                <p className="mt-2 text-sm leading-6 text-app-text-muted">
                  {activeDialog === 'delete'
                    ? 'Bu işlem geri alınamaz. Taslak ve taslağa eklediğiniz dosyalar kalıcı olarak silinecek.'
                    : activeDialog === 'discard'
                      ? 'Kaydetmeden ayrılırsanız formdaki değişiklikler silinecek.'
                      : 'Kayıt inceleme akışına alınacak ve gönderildikten sonra düzenlenemeyecek.'}
                </p>
              </div>
              <button
                ref={dialogCloseButtonRef}
                type="button"
                onClick={() => setActiveDialog(null)}
                className="flex size-10 shrink-0 items-center justify-center rounded-xl text-app-text-subtle transition hover:bg-app-surface-strong hover:text-app-text-strong focus-visible:outline-2 focus-visible:outline-brand-500"
                aria-label={activeDialog === 'delete' ? 'Silme penceresini kapat' : activeDialog === 'discard' ? 'Vazgeçme penceresini kapat' : 'Gönderme penceresini kapat'}
              >
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <button type="button" onClick={() => setActiveDialog(null)} className="min-h-11 rounded-xl border border-app-border px-4 text-sm font-bold text-app-text-secondary transition hover:bg-app-surface-muted">İptal</button>
              <button
                type="button"
                disabled={mutationBusy}
                onClick={() => runMutation(() => {
                  const completedAction = activeDialog
                  setActiveDialog(null)
                  if (completedAction === 'discard') {
                    navigate(`/kayitlar/${record.id}`)
                    return
                  }
                  if (completedAction === 'delete') {
                    deleteDraft(record.id)
                    navigate('/kayitlar?gorunum=taslaklar', { replace: true })
                    return
                  }

                  updateAndSubmit(record.id, toDraftInput(getValues()))
                  navigate(`/kayitlar/${record.id}`, { replace: true })
                })}
                className={`min-h-11 rounded-xl px-4 text-sm font-bold text-white transition ${
                  activeDialog === 'delete' || activeDialog === 'discard' ? 'bg-rose-600 hover:bg-rose-700' : 'bg-brand-700 hover:bg-brand-800'
                }`}
              >
                {activeDialog === 'delete' ? 'Evet, Taslağı Sil' : activeDialog === 'discard' ? 'Değişiklikleri Sil' : 'İncelemeye Gönder'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return (
    <span id={id} className="mt-1.5 block text-xs font-semibold text-rose-700 dark:text-rose-300" role="alert">
      {message}
    </span>
  )
}
